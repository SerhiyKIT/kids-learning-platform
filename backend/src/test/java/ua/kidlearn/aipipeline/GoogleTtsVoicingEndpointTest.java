package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Full context with the real GoogleTtsProvider wired (fake key, base URL pointed at a local
 * in-process server — never the real API) and in-memory audio storage (no MinIO): proves the bean
 * swap works end to end through the unchanged VoicingService cache path, and that provider
 * failures surface as a clean 502, not 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(InMemoryAudioStorageTestConfig.class)
@Transactional
class GoogleTtsVoicingEndpointTest {

	private static final String PASSWORD = "supersecret1";
	private static final byte[] AUDIO_BYTES = { (byte) 0xFF, (byte) 0xFB, 0x10, 0x00 };

	private static final HttpServer FAKE_GOOGLE = startFakeGoogle();
	private static volatile int responseStatus;
	private static volatile String responseBody;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TtsProvider ttsProvider;

	@Autowired
	private AudioAssetRepository audioAssetRepository;

	private static HttpServer startFakeGoogle() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/v1/text:synthesize", exchange -> {
				exchange.getRequestBody().readAllBytes();
				byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("content-type", "application/json");
				exchange.sendResponseHeaders(responseStatus, bytes.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			});
			server.start();
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@DynamicPropertySource
	static void googleProperties(DynamicPropertyRegistry registry) {
		registry.add("app.tts.provider", () -> "google");
		registry.add("app.tts.api-key", () -> "test-key-not-a-real-secret");
		registry.add("app.tts.base-url", () -> "http://127.0.0.1:" + FAKE_GOOGLE.getAddress().getPort());
		registry.add("app.tts.storage", () -> "memory");
	}

	@AfterAll
	static void stopFakeGoogle() {
		FAKE_GOOGLE.stop(0);
	}

	private MockHttpSession loginAsNewAdmin() throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@example.test";
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		AdminTwoFactorTestSupport.completeSetup(mockMvc, session);
		return session;
	}

	/** Version whose reality_bridge text is unique, so at least that line is never a cache hit. */
	private UUID createVersion(MockHttpSession adminSession, String uniqueText) throws Exception {
		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");
		String scenario = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("Remember to look both ways", uniqueText);
		assertThat(scenario).contains(uniqueText);

		MvcResult versionResult = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/versions").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scenario\":%s,\"generatedBy\":\"human\"}".formatted(scenario)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id"));
	}

	private ResultActions voice(String uniqueText) throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		UUID versionId = createVersion(adminSession, uniqueText);
		return mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf())
				.session(adminSession));
	}

	private static String uniqueText() {
		return "Line " + UUID.randomUUID();
	}

	@Test
	void realProviderIsTheOnlyWiredProvider() {
		assertThat(ttsProvider).isInstanceOf(GoogleTtsProvider.class);
	}

	@Test
	void synthesizedAudioIsCachedUnderTheGoogleProvider() throws Exception {
		responseStatus = 200;
		responseBody = "{\"audioContent\":\"" + Base64.getEncoder().encodeToString(AUDIO_BYTES) + "\"}";
		String text = uniqueText();

		voice(text).andExpect(status().isOk());

		AudioAsset asset = audioAssetRepository.findAll()
				.stream()
				.filter(a -> a.getText().equals(text))
				.findFirst()
				.orElseThrow();
		assertThat(asset.getProvider()).isEqualTo("google");
		assertThat(asset.getFileUrl()).startsWith("memory://tts/");
		assertThat(asset.getDurationMs()).isEqualTo(GoogleTtsProvider.durationMs(AUDIO_BYTES.length));
	}

	@Test
	void upstreamErrorIsMappedTo502NotA500() throws Exception {
		responseStatus = 500;
		responseBody = "{\"error\":{\"code\":500,\"message\":\"Internal error\",\"status\":\"INTERNAL\"}}";

		voice(uniqueText()).andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("TTS_UPSTREAM_ERROR"));
	}

	@Test
	void emptyAudioIsMappedTo502() throws Exception {
		responseStatus = 200;
		responseBody = "{}";

		voice(uniqueText()).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("TTS_EMPTY_AUDIO"));
	}

}
