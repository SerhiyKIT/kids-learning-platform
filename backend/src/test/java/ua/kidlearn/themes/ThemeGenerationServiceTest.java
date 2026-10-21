package ua.kidlearn.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.users.Role;

/** Generation outcomes that the stub never produces: an invalid theme and a failing provider. */
class ThemeGenerationServiceTest {

	private final ThemeGenerator generator = mock(ThemeGenerator.class);
	private final ThemeRepository repository = mock(ThemeRepository.class);
	private final TransactionTemplate transactions = new TransactionTemplate() {
		@Override
		public void executeWithoutResult(Consumer<TransactionStatus> action) {
			action.accept(null);
		}
	};
	private final ThemeGenerationService service = new ThemeGenerationService(generator, new ThemeValidator(),
			repository, mock(AuditService.class), JsonMapper.builder().build(), transactions,
			Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC));

	private Theme generateAndCapture() {
		service.generateIfAbsent("blue|space|calm", null, Role.PARENT);
		ArgumentCaptor<Theme> saved = ArgumentCaptor.forClass(Theme.class);
		verify(repository).saveAndFlush(saved.capture());
		return saved.getValue();
	}

	@Test
	void anInvalidThemeIsSavedAsRejectedWithItsReasonsNotAsADraft() {
		Map<String, String> palette = new LinkedHashMap<>(new StubThemeGenerator().generate("blue", "space", "calm").palette());
		palette.put("glow", "#00ff00");
		when(generator.generate("blue", "space", "calm")).thenReturn(new GeneratedTheme("Кислотна", palette, "helmet", "m"));

		Theme theme = generateAndCapture();

		assertThat(theme.getStatus()).isEqualTo(Theme.STATUS_REJECTED);
		assertThat(theme.getRejectReason()).contains("palette.glow: too saturated");
		assertThat(theme.getPreferenceKey()).isEqualTo("blue|space|calm");
	}

	@Test
	void aProviderFailureIsSavedAsRejectedForTheAdminToRegenerate() {
		when(generator.generate(any(), any(), any())).thenThrow(new ThemeGenerationException("Anthropic API returned 529"));

		Theme theme = generateAndCapture();

		assertThat(theme.getStatus()).isEqualTo(Theme.STATUS_REJECTED);
		assertThat(theme.getRejectReason()).isEqualTo("generation failed: Anthropic API returned 529");
	}

	@Test
	void anExistingKeyIsNeverGeneratedAgain() {
		when(repository.existsByPreferenceKey("blue|space|calm")).thenReturn(true);
		service.generateIfAbsent("blue|space|calm", null, Role.PARENT);
		verify(generator, never()).generate(any(), any(), any());
		verify(repository, never()).saveAndFlush(any());
	}

}
