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
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Full context with the real AnthropicLlmProvider wired (fake key, base URL pointed at a local
 * in-process server — never the real API): proves the bean swap works end to end, that a real
 * tool_use response still goes through ScenarioValidator, and that provider failures surface as
 * clean 502/422 responses, not 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AnthropicGenerationEndpointTest {

	private static final String PASSWORD = "supersecret1";

	private static final String GENERATION_REQUEST_BODY = """
			{"topic":"road_safety","patternIds":["1.6"],"ageBand":"age_5_6",
			 "title":"Crossing the street","learningGoal":"Look both ways before crossing"}
			""";

	private static final HttpServer FAKE_ANTHROPIC = startFakeAnthropic();
	private static volatile int responseStatus;
	private static volatile String responseBody;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private LlmProvider llmProvider;

	@Autowired
	private LessonVersionRepository lessonVersionRepository;

	private static HttpServer startFakeAnthropic() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/v1/messages", exchange -> {
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
	static void anthropicProperties(DynamicPropertyRegistry registry) {
		registry.add("app.llm.provider", () -> "anthropic");
		registry.add("app.llm.api-key", () -> "test-key-not-a-real-secret");
		registry.add("app.llm.base-url", () -> "http://127.0.0.1:" + FAKE_ANTHROPIC.getAddress().getPort());
	}

	@AfterAll
	static void stopFakeAnthropic() {
		FAKE_ANTHROPIC.stop(0);
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

	private String createLesson(MockHttpSession adminSession) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions generate() throws Exception {
		MockHttpSession adminSession = loginAsNewAdmin();
		String lessonId = createLesson(adminSession);
		return mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
				.session(adminSession)
				.contentType(MediaType.APPLICATION_JSON)
				.content(GENERATION_REQUEST_BODY));
	}

	@Test
	void realProviderIsTheOnlyWiredProvider() {
		assertThat(llmProvider).isInstanceOf(AnthropicLlmProvider.class);
	}

	@Test
	void toolUseScenarioGoesThroughValidatorAndIsPersistedAsAutoValidated() throws Exception {
		responseStatus = 200;
		responseBody = """
				{"id":"msg_01","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[{"type":"tool_use","id":"toolu_01","name":"emit_lesson_scenario","input":%s}],
				 "stop_reason":"tool_use","stop_details":null,"usage":{"input_tokens":1,"output_tokens":1}}
				""".formatted(ScenarioFixtures.VALID_MINIMAL_SCENARIO);

		MvcResult result = generate().andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("auto_validated"))
				.andReturn();

		String versionId = JsonPath.read(result.getResponse().getContentAsString(), "$.versionId");
		assertThat(lessonVersionRepository.findById(UUID.fromString(versionId)).orElseThrow().getAiModel())
				.isEqualTo("claude-sonnet-5-5");
	}

	@Test
	void schemaValidButRuleBreakingToolUseScenarioIsStillRejectedByScenarioValidator() throws Exception {
		responseStatus = 200;
		// Strict tool use can't enforce the business rules: last interactive scene not a control scene.
		String ruleBreaking = ScenarioFixtures.VALID_MINIMAL_SCENARIO.replace("\"is_control\": true",
				"\"is_control\": false");
		assertThat(ruleBreaking).isNotEqualTo(ScenarioFixtures.VALID_MINIMAL_SCENARIO);
		responseBody = """
				{"id":"msg_02","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[{"type":"tool_use","id":"toolu_02","name":"emit_lesson_scenario","input":%s}],
				 "stop_reason":"tool_use","stop_details":null,"usage":{"input_tokens":1,"output_tokens":1}}
				""".formatted(ruleBreaking);

		generate().andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("rejected_auto"))
				.andExpect(jsonPath("$.problems").isNotEmpty());
	}

	@Test
	void upstreamErrorIsMappedTo502NotA500() throws Exception {
		responseStatus = 500;
		responseBody = """
				{"type":"error","error":{"type":"api_error","message":"Internal server error"}}
				""";

		generate().andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("LLM_UPSTREAM_ERROR"));
	}

	@Test
	void missingToolUseIsMappedTo502() throws Exception {
		responseStatus = 200;
		responseBody = """
				{"id":"msg_03","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[{"type":"text","text":"..."}],
				 "stop_reason":"end_turn","stop_details":null,"usage":{"input_tokens":1,"output_tokens":1}}
				""";

		generate().andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("LLM_NO_SCENARIO"));
	}

	@Test
	void refusalIsMappedTo422() throws Exception {
		responseStatus = 200;
		responseBody = """
				{"id":"msg_04","type":"message","role":"assistant","model":"claude-sonnet-5-5",
				 "content":[],"stop_reason":"refusal",
				 "stop_details":{"type":"refusal","category":"general_harms","explanation":"..."},
				 "usage":{"input_tokens":1,"output_tokens":0}}
				""";

		generate().andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("LLM_REFUSED"));
	}

}
