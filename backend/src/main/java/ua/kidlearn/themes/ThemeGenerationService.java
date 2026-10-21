package ua.kidlearn.themes;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.users.Role;

/**
 * Generates the one theme for a preference key: generator → ThemeValidator → saved as draft (for
 * moderation) or rejected with the reasons (like a lesson's rejected_auto). The generator call runs
 * outside any transaction; only the save does not. Audited as GENERATE_THEME.
 */
@Service
public class ThemeGenerationService {

	private static final Logger log = LoggerFactory.getLogger(ThemeGenerationService.class);

	private final ThemeGenerator generator;
	private final ThemeValidator validator;
	private final ThemeRepository themeRepository;
	private final AuditService auditService;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;
	// Two children asking for the same new key at once must not cost two generator calls.
	private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

	public ThemeGenerationService(ThemeGenerator generator, ThemeValidator validator, ThemeRepository themeRepository,
			AuditService auditService, ObjectMapper objectMapper, TransactionTemplate transactionTemplate, Clock clock) {
		this.generator = generator;
		this.validator = validator;
		this.themeRepository = themeRepository;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
		this.transactionTemplate = transactionTemplate;
		this.clock = clock;
	}

	/** First generation for a key; does nothing if a theme for it exists (or is being generated). */
	public void generateIfAbsent(String preferenceKey, UUID actorId, Role actorRole) {
		StyleAnswers answers = ThemeCatalog.parseKey(preferenceKey)
				.orElseThrow(() -> new IllegalArgumentException("Not a preference key: " + preferenceKey));
		if (!inFlight.add(preferenceKey)) {
			return;
		}
		try {
			if (themeRepository.existsByPreferenceKey(preferenceKey)) {
				return;
			}
			Theme theme = Theme.generated(preferenceKey, answers.worldId(), clock.instant());
			fill(theme, answers);
			transactionTemplate.executeWithoutResult(status -> {
				themeRepository.saveAndFlush(theme);
				auditService.record(actorId, actorRole, AuditAction.GENERATE_THEME, AuditTargetType.THEME, theme.getId(),
						null);
			});
		} catch (DataIntegrityViolationException e) {
			// Another instance won the race for this key: its theme is the one.
			log.info("Theme for {} already created concurrently", preferenceKey);
		} finally {
			inFlight.remove(preferenceKey);
		}
	}

	/** A new attempt for a rejected theme, in place (the key stays unique). The caller audits it. */
	public Theme regenerate(Theme rejected) {
		StyleAnswers answers = ThemeCatalog.parseKey(rejected.getPreferenceKey()).orElseThrow();
		fill(rejected, answers);
		return transactionTemplate.execute(status -> themeRepository.saveAndFlush(rejected));
	}

	private void fill(Theme theme, StyleAnswers answers) {
		try {
			GeneratedTheme generated = generator.generate(answers.colorId(), answers.worldId(), answers.moodId());
			ThemeValidator.Result result = validator.validate(generated.palette(), answers.worldId(),
					generated.guideAccessory(), generated.name());
			String reason = result.isValid() ? null : String.join("; ", result.problems());
			theme.setGeneration(orDefault(generated.name(), answers.preferenceKey()),
					objectMapper.writeValueAsString(generated.palette()), orDefault(generated.guideAccessory(), "none"),
					generated.model(), reason);
		} catch (ThemeGenerationException e) {
			log.warn("Theme generation failed for {}: {}", answers.preferenceKey(), e.getMessage());
			theme.setGeneration(answers.preferenceKey(), objectMapper.writeValueAsString(Map.of()), "none", null,
					"generation failed: " + e.getMessage());
		}
	}

	private static String orDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

}
