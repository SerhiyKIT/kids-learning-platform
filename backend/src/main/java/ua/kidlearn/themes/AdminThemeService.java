package ua.kidlearn.themes;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Theme moderation, like lessons: only a validated draft can be approved or rejected; approving
 * hands the theme to every child whose answers match its key. theme_default isn't moderated.
 */
@Service
public class AdminThemeService {

	private static final Set<String> LISTABLE = Set.of(Theme.STATUS_DRAFT, Theme.STATUS_APPROVED, Theme.STATUS_REJECTED);

	private final ThemeRepository themeRepository;
	private final ChildStylePrefsRepository prefsRepository;
	private final ThemeValidator validator;
	private final ThemeGenerationService generationService;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public AdminThemeService(ThemeRepository themeRepository, ChildStylePrefsRepository prefsRepository,
			ThemeValidator validator, ThemeGenerationService generationService, ObjectMapper objectMapper, Clock clock) {
		this.themeRepository = themeRepository;
		this.prefsRepository = prefsRepository;
		this.validator = validator;
		this.generationService = generationService;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<AdminThemeEntry> list(String status) {
		if (!LISTABLE.contains(status)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + status);
		}
		return themeRepository.findByStatusAndPreferenceKeyIsNotNullOrderByCreatedAtDesc(status).stream()
				.map(this::toEntry)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminThemeEntry get(UUID id) {
		return toEntry(requireGenerated(id));
	}

	@Transactional
	public AdminThemeEntry approve(UUID id, UUID adminId) {
		Theme theme = requireDraft(id);
		theme.approve(adminId, clock.instant());
		themeRepository.flush();
		prefsRepository.assignTheme(theme.getId(), theme.getPreferenceKey());
		return toEntry(theme);
	}

	@Transactional
	public AdminThemeEntry reject(UUID id, String reason) {
		Theme theme = requireDraft(id);
		theme.reject(reason.strip());
		return toEntry(theme);
	}

	/** Only a rejected theme gets another generation (in place, re-validated: draft or rejected again). */
	public AdminThemeEntry regenerate(String preferenceKey) {
		Theme theme = themeRepository.findByPreferenceKey(preferenceKey)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No theme for " + preferenceKey));
		if (!Theme.STATUS_REJECTED.equals(theme.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a rejected theme can be regenerated");
		}
		return toEntry(generationService.regenerate(theme));
	}

	private Theme requireGenerated(UUID id) {
		return themeRepository.findById(id)
				.filter(t -> t.getPreferenceKey() != null)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme not found"));
	}

	private Theme requireDraft(UUID id) {
		Theme theme = requireGenerated(id);
		if (!Theme.STATUS_DRAFT.equals(theme.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a draft theme can be approved or rejected");
		}
		return theme;
	}

	private AdminThemeEntry toEntry(Theme theme) {
		JsonNode palette = objectMapper.readTree(theme.getPalette());
		Map<String, String> colours = new LinkedHashMap<>();
		palette.properties().forEach(e -> colours.put(e.getKey(), e.getValue().asText()));
		ThemeValidator.Result result = validator.validate(colours, theme.getWorldId(), theme.getGuideAccessory(),
				theme.getName());
		StyleAnswers answers = ThemeCatalog.parseKey(theme.getPreferenceKey()).orElseThrow();
		return new AdminThemeEntry(theme.getId(), theme.getName(), theme.getPreferenceKey(), answers.colorId(),
				answers.worldId(), answers.moodId(), palette, theme.getGuideAccessory(), theme.getStatus(),
				theme.getRejectReason(), theme.getGeneratedBy(), theme.getAiModel(), theme.getCreatedAt(),
				theme.getApprovedAt(), result.contrast(), result.problems());
	}

}
