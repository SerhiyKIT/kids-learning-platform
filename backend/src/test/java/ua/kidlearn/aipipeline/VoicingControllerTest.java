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

	/**
	 * A schema-valid scenario (1 reality_bridge + 1 demo narration line + 2 choice scenes, each with
	 * setup + 2 options*(label+feedback) + 2 hints + re_explain = 8) = 18 distinct voice-line objects,
	 * every key/text namespaced by keyPrefix so two calls with different prefixes share no text except
	 * whatever bridgeText is passed.
	 */
	private static String scenario(String keyPrefix, String bridgeText) {
		return """
				{
				  "schema_version": "1.0.0",
				  "module": "safety",
				  "topic": "road_safety",
				  "pattern_ids": ["1.6"],
				  "age_band": "age_5_6",
				  "title": "Crossing the street",
				  "learning_goal": "Look both ways before crossing",
				  "reality_bridge": {"key": "%1$s_bridge", "text": "%2$s"},
				  "scenes": [
				    {
				      "key": "scene_1", "type": "demo", "background": "street_bg",
				      "characters": [{"id": "guide", "emotion": "neutral"}],
				      "narration": [
				        {"line": {"key": "%1$s_narr", "text": "%1$s narration line here now"}}
				      ]
				    },
				    {
				      "key": "scene_2", "type": "choice", "background": "street_bg",
				      "characters": [{"id": "guide", "emotion": "thinking"}],
				      "is_control": false,
				      "setup": {"key": "%1$s_setup1", "text": "%1$s setup one question text"},
				      "options": [
				        {"id": "a", "icon": "icon_a", "label": {"key": "%1$s_opt1a", "text": "%1$s option one a"}, "correct": true,
				         "feedback": {"line": {"key": "%1$s_fb1a", "text": "%1$s feedback one a"}}},
				        {"id": "b", "icon": "icon_b", "label": {"key": "%1$s_opt1b", "text": "%1$s option one b"}, "correct": false,
				         "feedback": {"line": {"key": "%1$s_fb1b", "text": "%1$s feedback one b"}}}
				      ],
				      "assistant": {
				        "hints": [
				          {"level": 1, "line": {"key": "%1$s_hint1a", "text": "%1$s hint one a"}},
				          {"level": 2, "line": {"key": "%1$s_hint1b", "text": "%1$s hint one b"}}
				        ],
				        "re_explain": {"key": "%1$s_re1", "text": "%1$s re explain one"}
				      }
				    },
				    {
				      "key": "scene_3", "type": "choice", "background": "street_bg",
				      "characters": [{"id": "guide", "emotion": "pride"}],
				      "is_control": true,
				      "setup": {"key": "%1$s_setup2", "text": "%1$s setup two question text"},
				      "options": [
				        {"id": "a", "icon": "icon_a", "label": {"key": "%1$s_opt2a", "text": "%1$s option two a"}, "correct": true,
				         "feedback": {"line": {"key": "%1$s_fb2a", "text": "%1$s feedback two a"}}},
				        {"id": "b", "icon": "icon_b", "label": {"key": "%1$s_opt2b", "text": "%1$s option two b"}, "correct": false,
				         "feedback": {"line": {"key": "%1$s_fb2b", "text": "%1$s feedback two b"}}}
				      ],
				      "assistant": {
				        "hints": [
				          {"level": 1, "line": {"key": "%1$s_hint2a", "text": "%1$s hint two a"}},
				          {"level": 2, "line": {"key": "%1$s_hint2b", "text": "%1$s hint two b"}}
				        ],
				        "re_explain": {"key": "%1$s_re2", "text": "%1$s re explain two"}
				      }
				    }
				  ]
				}
				""".formatted(keyPrefix, bridgeText);
	}

	private static final int VOICE_LINES_PER_SCENARIO = 18;

	@Test
	void firstVoicingSynthesizesEveryDistinctLineWithNoCacheHits() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "Crossing the street", scenario("va", "Remember to look both ways"));

		VoicingResult result = voice(adminSession, versionId);

		assertThat(result.linesTotal()).isEqualTo(VOICE_LINES_PER_SCENARIO);
		assertThat(result.synthesized()).isEqualTo(VOICE_LINES_PER_SCENARIO);
		assertThat(result.cached()).isEqualTo(0);
	}

	@Test
	void secondVoicingIsFullyCached() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "Crossing the street", scenario("va", "Remember to look both ways"));

		voice(adminSession, versionId);
		VoicingResult second = voice(adminSession, versionId);

		assertThat(second.linesTotal()).isEqualTo(VOICE_LINES_PER_SCENARIO);
		assertThat(second.synthesized()).isEqualTo(0);
		assertThat(second.cached()).isEqualTo(VOICE_LINES_PER_SCENARIO);
	}

	@Test
	void sharedVoiceLineAcrossTwoVersionsIsSynthesizedOnce() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);

		UUID versionA = createVersion(adminSession, "Crossing the street", scenario("va", "Remember to look both ways"));
		voice(adminSession, versionA);

		// Only the reality_bridge text is shared with versionA; every other line is namespaced "vb".
		UUID versionB = createVersion(adminSession, "Emergency numbers", scenario("vb", "Remember to look both ways"));
		VoicingResult resultB = voice(adminSession, versionB);

		assertThat(resultB.linesTotal()).isEqualTo(VOICE_LINES_PER_SCENARIO);
		assertThat(resultB.synthesized()).isEqualTo(VOICE_LINES_PER_SCENARIO - 1);
		assertThat(resultB.cached()).isEqualTo(1);
	}

	@Test
	void nonAdminCannotVoice() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID versionId = createVersion(adminSession, "Crossing the street", scenario("nc", "Remember to look both ways"));

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf())
						.session(teacherSession))
				.andExpect(status().isForbidden());
	}

}
