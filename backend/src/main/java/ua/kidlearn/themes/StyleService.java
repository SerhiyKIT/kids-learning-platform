package ua.kidlearn.themes;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.consents.Consent;
import ua.kidlearn.consents.ConsentRepository;
import ua.kidlearn.consents.ConsentRevokedEvent;
import ua.kidlearn.users.Role;

/**
 * A child's style: their answers (only with style_personalization consent) and the theme they see.
 * The child only ever sees an APPROVED theme; until their answers' theme is approved they keep
 * whatever they had (theme_default at first).
 */
@Service
public class StyleService {

	private final ChildStylePrefsRepository prefsRepository;
	private final ThemeRepository themeRepository;
	private final ParentChildRepository parentChildRepository;
	private final ConsentRepository consentRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final ObjectMapper objectMapper;

	public StyleService(ChildStylePrefsRepository prefsRepository, ThemeRepository themeRepository,
			ParentChildRepository parentChildRepository, ConsentRepository consentRepository,
			ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
		this.prefsRepository = prefsRepository;
		this.themeRepository = themeRepository;
		this.parentChildRepository = parentChildRepository;
		this.consentRepository = consentRepository;
		this.eventPublisher = eventPublisher;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public StyleResponse style(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		return toResponse(childId, prefsRepository.findById(childId));
	}

	@Transactional
	public StyleResponse choose(UUID parentId, Role parentRole, UUID childId, StyleAnswers answers) {
		requireChildOwnership(parentId, childId);
		if (!hasConsent(childId)) {
			throw new StyleConsentRequiredException();
		}
		if (!ThemeCatalog.isValid(answers.colorId(), answers.worldId(), answers.moodId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown colorId, worldId or moodId");
		}
		ChildStylePrefs prefs = prefsRepository.findById(childId).orElseGet(() -> new ChildStylePrefs(childId));
		prefs.setAnswers(objectMapper.writeValueAsString(answers));
		Optional<Theme> theme = themeRepository.findByPreferenceKey(answers.preferenceKey());
		if (theme.isPresent() && theme.get().isApproved()) {
			prefs.setThemeId(theme.get().getId());
		} else if (theme.isEmpty()) {
			eventPublisher.publishEvent(new ThemeGenerationRequested(answers.preferenceKey(), parentId, parentRole));
		}
		// A draft or rejected theme: wait for moderation; the child keeps their current theme meanwhile.
		return toResponse(childId, Optional.of(prefsRepository.save(prefs)));
	}

	/** The guide has announced the child's current theme. */
	@Transactional
	public void markSeen(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		prefsRepository.findById(childId).ifPresent(ChildStylePrefs::markSeen);
	}

	/** Revoking style_personalization deletes what it allowed us to keep (back to theme_default). */
	@EventListener
	public void onConsentRevoked(ConsentRevokedEvent event) {
		if (Consent.TYPE_STYLE_PERSONALIZATION.equals(event.type())) {
			prefsRepository.findById(event.childId()).ifPresent(prefsRepository::delete);
		}
	}

	private StyleResponse toResponse(UUID childId, Optional<ChildStylePrefs> prefs) {
		boolean consented = hasConsent(childId);
		StyleAnswers answers = prefs.map(p -> objectMapper.readValue(p.getAnswers(), StyleAnswers.class)).orElse(null);
		Theme theme = prefs.map(ChildStylePrefs::getThemeId)
				.flatMap(themeRepository::findById)
				.filter(Theme::isApproved)
				.orElseGet(() -> themeRepository.findById(Theme.DEFAULT_ID).orElseThrow());
		boolean pending = answers != null && !answers.preferenceKey().equals(theme.getPreferenceKey());
		boolean announce = prefs.map(p -> p.getThemeId() != null && !Objects.equals(p.getThemeId(), p.getThemeSeenId()))
				.orElse(false);
		return new StyleResponse(consented, answers, new StyleResponse.ThemeView(theme.getId(),
				objectMapper.readTree(theme.getPalette()), theme.getWorldId(), theme.getGuideAccessory()), pending,
				announce);
	}

	private boolean hasConsent(UUID childId) {
		return consentRepository.existsByChildIdAndTypeAndRevokedAtIsNull(childId,
				Consent.TYPE_STYLE_PERSONALIZATION);
	}

	private void requireChildOwnership(UUID parentId, UUID childId) {
		if (!parentChildRepository.existsById_ParentIdAndId_ChildId(parentId, childId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

}
