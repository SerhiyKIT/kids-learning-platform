package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.config.TtsProperties;
import ua.kidlearn.lessons.LessonAssignment;
import ua.kidlearn.lessons.LessonAssignmentRepository;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Voice-line audio delivery to child mode: GET /api/audio/{textHash} and the audioUrl enrichment
 * of the child's scenario. Same context configuration as VoicingControllerTest (in-memory audio
 * storage, no MinIO), so the cached Spring context — and its DB pool — is shared.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.tts.storage=memory")
@Import(InMemoryAudioStorageTestConfig.class)
@Transactional
class AudioDeliveryTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private LessonService lessonService;

	@Autowired
	private LessonAssignmentRepository lessonAssignmentRepository;

	@Autowired
	private AudioAssetRepository audioAssetRepository;

	@Autowired
	private AudioStorage audioStorage;

	@Autowired
	private TtsProperties ttsProperties;

	@Autowired
	private ObjectMapper objectMapper;

	private record Fixture(MockHttpSession parent, UUID childId, UUID versionId, String scenarioJson) {
	}

	private User saveUser(Role role) {
		User user = new User(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.test",
				passwordEncoder.encode(PASSWORD), role, "User", "uk");
		user.markEmailVerified();
		return userRepository.save(user);
	}

	private MockHttpSession login(User user) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(user.getEmail()).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	/** Published, voiced lesson (unique texts) assigned directly to a parent's child. */
	private Fixture voicedLessonForAChild() throws Exception {
		MockHttpSession admin = login(saveUser(Role.ADMIN));
		AdminTwoFactorTestSupport.completeSetup(mockMvc, admin);
		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		UUID lessonId = UUID.fromString(JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id"));
		String unique = UUID.randomUUID().toString().substring(0, 8);
		String scenarioJson = ScenarioFixtures.VALID_MINIMAL_SCENARIO
				.replace("Remember to look both ways", "Remember " + unique)
				.replace("Watch me cross the street safely", "Watch me " + unique);
		UUID versionId = lessonService.createGeneratedVersion(lessonId, objectMapper.readTree(scenarioJson),
				"test-fixture", LessonVersion.STATUS_AUTO_VALIDATED).getId();
		for (String action : List.of("approve", "voice", "publish")) {
			mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/" + action).with(csrf()).session(admin))
					.andExpect(status().isOk());
		}

		User parentUser = saveUser(Role.PARENT);
		MockHttpSession parent = login(parentUser);
		MvcResult childResult = mockMvc.perform(post("/api/children").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Kid\",\"birthYear\":%d,\"relation\":\"mother\"}"
								.formatted(Year.now().getValue() - 5)))
				.andExpect(status().isCreated())
				.andReturn();
		UUID childId = UUID.fromString(JsonPath.read(childResult.getResponse().getContentAsString(), "$.id"));
		lessonAssignmentRepository.save(new LessonAssignment(versionId, null, childId, saveUser(Role.TEACHER).getId(), null, null));
		return new Fixture(parent, childId, versionId, scenarioJson);
	}

	private JsonNode scenario(Fixture f) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/children/" + f.childId() + "/lessons/" + f.versionId() + "/scenario")
						.session(f.parent()))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private static List<JsonNode> voiceLines(JsonNode scenario) {
		List<JsonNode> lines = new ArrayList<>();
		VoiceLines.forEach(scenario, lines::add);
		return lines;
	}

	private String currentHash(String text) {
		return VoiceLines.hash(ttsProperties.voiceId(), text);
	}

	// ---- scenario enrichment ------------------------------------------------------------------

	@Test
	void everyVoicedLineGetsItsAudioUrl() throws Exception {
		Fixture f = voicedLessonForAChild();

		List<JsonNode> lines = voiceLines(scenario(f));

		assertThat(lines).isNotEmpty().allSatisfy(line -> assertThat(line.path("audioUrl").asText())
				.isEqualTo("/api/audio/" + currentHash(line.get("text").asText())));
	}

	@Test
	void lineWithoutAudioHasNoAudioUrl() throws Exception {
		Fixture f = voicedLessonForAChild();
		String unvoiced = "Watch me " + f.scenarioJson().split("Watch me ")[1].substring(0, 8);
		audioAssetRepository.delete(audioAssetRepository.findByTextHash(currentHash(unvoiced)).orElseThrow());

		List<JsonNode> lines = voiceLines(scenario(f));

		assertThat(lines).filteredOn(l -> l.get("text").asText().equals(unvoiced))
				.singleElement()
				.satisfies(l -> assertThat(l.has("audioUrl")).isFalse());
		assertThat(lines).filteredOn(l -> !l.get("text").asText().equals(unvoiced))
				.allSatisfy(l -> assertThat(l.has("audioUrl")).isTrue());
	}

	@Test
	void fallsBackToAudioOfTheSameTextUnderAnotherVoice() throws Exception {
		Fixture f = voicedLessonForAChild();
		String text = "Remember " + f.scenarioJson().split("Remember ")[1].substring(0, 8);
		AudioAsset current = audioAssetRepository.findByTextHash(currentHash(text)).orElseThrow();
		audioAssetRepository.delete(current);
		String otherVoiceHash = VoiceLines.hash("some-earlier-voice", text);
		audioAssetRepository.save(new AudioAsset(otherVoiceHash, text, "stub", "some-earlier-voice", "memory://x", 900));

		JsonNode line = voiceLines(scenario(f)).stream()
				.filter(l -> l.get("text").asText().equals(text))
				.findFirst()
				.orElseThrow();

		assertThat(line.path("audioUrl").asText()).isEqualTo("/api/audio/" + otherVoiceHash);
	}

	@Test
	void storedScenarioIsNotModifiedByEnrichment() throws Exception {
		Fixture f = voicedLessonForAChild();
		scenario(f);

		assertThat(lessonService.getVersion(f.versionId()).getScenario()).doesNotContain("audioUrl");
	}

	@Test
	void anotherParentsChildIsStill404() throws Exception {
		Fixture f = voicedLessonForAChild();
		MockHttpSession stranger = login(saveUser(Role.PARENT));

		mockMvc.perform(get("/api/children/" + f.childId() + "/lessons/" + f.versionId() + "/scenario").session(stranger))
				.andExpect(status().isNotFound());
	}

	// ---- GET /api/audio/{textHash} -------------------------------------------------------------

	@Test
	void anonymousGets401() throws Exception {
		mockMvc.perform(get("/api/audio/" + "a".repeat(64))).andExpect(status().isUnauthorized());
	}

	@Test
	void malformedHashIs400() throws Exception {
		MockHttpSession parent = login(saveUser(Role.PARENT));
		for (String bad : List.of("abc", "A".repeat(64), "g".repeat(64), "a".repeat(63), "a".repeat(65), "..%2Ftts")) {
			mockMvc.perform(get("/api/audio/" + bad).session(parent)).andExpect(status().isBadRequest());
		}
	}

	@Test
	void unknownHashIs404() throws Exception {
		MockHttpSession parent = login(saveUser(Role.PARENT));
		mockMvc.perform(get("/api/audio/" + "0".repeat(64)).session(parent)).andExpect(status().isNotFound());
	}

	@Test
	void assetWithoutStoredObjectIs404() throws Exception {
		MockHttpSession parent = login(saveUser(Role.PARENT));
		String hash = VoiceLines.hash("v", "orphan " + UUID.randomUUID());
		audioAssetRepository.save(new AudioAsset(hash, "orphan", "stub", "v", "memory://missing", 500));

		mockMvc.perform(get("/api/audio/" + hash).session(parent)).andExpect(status().isNotFound());
	}

	@Test
	void servesTheStoredBytesWithForeverCacheHeadersAndHonoursIfNoneMatch() throws Exception {
		Fixture f = voicedLessonForAChild();
		String url = voiceLines(scenario(f)).get(0).get("audioUrl").asText();
		String hash = url.substring("/api/audio/".length());
		byte[] stored = audioStorage.load(VoiceLines.objectKey(hash)).orElseThrow().bytes();

		MvcResult result = mockMvc.perform(get(url).session(f.parent()))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "audio/mpeg"))
				.andExpect(header().string("Content-Length", String.valueOf(stored.length)))
				.andExpect(header().string("ETag", "\"" + hash + "\""))
				.andExpect(header().string("Cache-Control", "max-age=31536000, private, immutable"))
				.andReturn();
		assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(stored);

		mockMvc.perform(get(url).session(f.parent()).header("If-None-Match", "\"" + hash + "\""))
				.andExpect(status().isNotModified());
	}

	@Test
	void anySignedInRoleMayFetchAudioAndRangeRequestsAreSupported() throws Exception {
		Fixture f = voicedLessonForAChild();
		String url = voiceLines(scenario(f)).get(0).get("audioUrl").asText();

		mockMvc.perform(get(url).session(login(saveUser(Role.TEACHER)))).andExpect(status().isOk());
		mockMvc.perform(get(url).session(f.parent()).header("Range", "bytes=0-3"))
				.andExpect(status().isPartialContent())
				.andExpect(header().string("Content-Length", "4"));
	}

	// ---- stub audio is real, decodable MP3 ----------------------------------------------------

	@Test
	void stubAudioIsAWholeNumberOfMp3FramesMatchingItsDuration() {
		byte[] mp3 = StubTtsProvider.silentMp3(1000);

		assertThat(mp3.length % 417).isZero();
		assertThat(mp3.length / 417).isEqualTo(39); // ceil(1000 ms / 26.12 ms per frame)
		for (int offset = 0; offset < mp3.length; offset += 417) {
			assertThat(mp3[offset]).isEqualTo((byte) 0xFF);
			assertThat(mp3[offset + 1]).isEqualTo((byte) 0xFB);
		}
	}

}
