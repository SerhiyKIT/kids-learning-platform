package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * app.tts.storage=memory keeps MinioAudioStorage (@ConditionalOnProperty)
 * from ever being instantiated, so this suite needs no MinIO. StubTtsProvider
 * already does no network by design, so no swap is needed for it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.tts.storage=memory")
@Import(InMemoryAudioStorageTestConfig.class)
@Transactional
class VoicingControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private void registerAdmin(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
	}

	private void registerTeacher(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.TEACHER, "Teacher", "uk"));
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private UUID createVersion(MockHttpSession adminSession, String title, String scenarioJson) throws Exception {
		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"%s\"}".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");

		MvcResult versionResult = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/versions").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scenario\":%s,\"generatedBy\":\"human\"}".formatted(scenarioJson)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id"));
	}

	private VoicingResult voice(MockHttpSession adminSession, UUID versionId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		return new VoicingResult(
				((Number) JsonPath.<Object>read(body, "$.linesTotal")).intValue(),
				((Number) JsonPath.<Object>read(body, "$.synthesized")).intValue(),
				((Number) JsonPath.<Object>read(body, "$.cached")).intValue());
	}

	// 5 voice-line objects, 2 of them duplicating another line's text
	// (fb_b repeats bridge1's text, hint1 repeats setup1's text) -> 5 distinct.
	private static final String SCENARIO_WITH_DUPLICATES = """
			{
			  "learning_goal": "Cross safely",
			  "reality_bridge": {"key": "bridge1", "text": "Remember to look both ways"},
			  "scenes": [
			    {
			      "key": "scene_1",
			      "setup": {"key": "setup1", "text": "Look both ways before crossing"},
			      "options": [
			        {"id": "a", "label": {"key": "opt_a", "text": "Cross now"}, "correct": false,
			         "feedback": {"line": {"key": "fb_a", "text": "Not yet, look again"}}},
			        {"id": "b", "label": {"key": "opt_b", "text": "Look both ways"}, "correct": true,
			         "feedback": {"line": {"key": "fb_b", "text": "Remember to look both ways"}}}
			      ],
			      "assistant": {
			        "hints": [
			          {"level": 1, "line": {"key": "hint1", "text": "Look both ways before crossing"}}
			        ]
			      }
			    }
			  ]
			}
			""";

	@Test
	void firstVoicingSynthesizesEveryDistinctLineWithNoCacheHits() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "Crossing the street", SCENARIO_WITH_DUPLICATES);

		VoicingResult result = voice(adminSession, versionId);

		assertThat(result.linesTotal()).isEqualTo(5);
		assertThat(result.synthesized()).isEqualTo(5);
		assertThat(result.cached()).isEqualTo(0);
	}

	@Test
	void secondVoicingIsFullyCached() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "Crossing the street", SCENARIO_WITH_DUPLICATES);

		voice(adminSession, versionId);
		VoicingResult second = voice(adminSession, versionId);

		assertThat(second.linesTotal()).isEqualTo(5);
		assertThat(second.synthesized()).isEqualTo(0);
		assertThat(second.cached()).isEqualTo(5);
	}

	@Test
	void sharedVoiceLineAcrossTwoVersionsIsSynthesizedOnce() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);

		UUID versionA = createVersion(adminSession, "Crossing the street", SCENARIO_WITH_DUPLICATES);
		voice(adminSession, versionA);

		// One line ("Remember to look both ways") is identical to versionA's bridge1 text.
		String scenarioB = """
				{
				  "learning_goal": "Know emergency numbers",
				  "reality_bridge": {"key": "bridge2", "text": "Remember to look both ways"},
				  "scenes": [
				    {
				      "key": "scene_1",
				      "setup": {"key": "setup2", "text": "Which number do you call"},
				      "options": [
				        {"id": "a", "label": {"key": "opt_c", "text": "Call 101"}, "correct": true,
				         "feedback": {"line": {"key": "fb_c", "text": "Great job"}}}
				      ]
				    }
				  ]
				}
				""";
		UUID versionB = createVersion(adminSession, "Emergency numbers", scenarioB);
		VoicingResult resultB = voice(adminSession, versionB);

		assertThat(resultB.linesTotal()).isEqualTo(4);
		assertThat(resultB.synthesized()).isEqualTo(3);
		assertThat(resultB.cached()).isEqualTo(1);
	}

	@Test
	void versionWithNoVoiceLinesVoicesCleanly() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "No voice lines", "{\"foo\":\"bar\",\"nested\":{\"baz\":123}}");

		VoicingResult result = voice(adminSession, versionId);

		assertThat(result.linesTotal()).isEqualTo(0);
		assertThat(result.synthesized()).isEqualTo(0);
		assertThat(result.cached()).isEqualTo(0);
	}

	@Test
	void nonAdminCannotVoice() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "Crossing the street", SCENARIO_WITH_DUPLICATES);

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf())
						.session(teacherSession))
				.andExpect(status().isForbidden());
	}

}
