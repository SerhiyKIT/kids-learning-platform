package ua.kidlearn.support;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ua.kidlearn.aipipeline.InMemoryAudioStorageTestConfig;
import ua.kidlearn.aipipeline.LlmProvider;
import ua.kidlearn.support.WebIntegrationTest;

/**
 * WebIntegrationTest with audio kept in memory instead of MinIO (CI has none), for the content
 * pipeline and audio delivery. The LLM provider is a spy for everyone here — it behaves like the
 * real (stub) one unless a test stubs it.
 */
@TestPropertySource(properties = "app.tts.storage=memory")
@Import(InMemoryAudioStorageTestConfig.class)
public abstract class InMemoryAudioWebIntegrationTest extends WebIntegrationTest {

	@MockitoSpyBean
	protected LlmProvider llmProvider;

}
