package ua.kidlearn.attempts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.aipipeline.AudioAsset;
import ua.kidlearn.aipipeline.AudioAssetRepository;
import ua.kidlearn.aipipeline.VoiceLines;
import ua.kidlearn.config.TtsProperties;
import ua.kidlearn.ratelimit.MutableClockTestConfig;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * GET /api/children/{childId}/stickers — the sticker album: one sticker per completed lesson (never
 * duplicated, never graded), its reality_bridge with audio once voiced, and topic stickers. Fixture:
 * the demo child with the two dev-seed lessons, both topic road_safety. Same context configuration
 * as PlayStateTest, so the cached context is reused.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(MutableClockTestConfig.class)
@Transactional
class StickersTest {

	private static final String PASSWORD = "supersecret1";
	// ScenarioFixtures.VALID_MINIMAL_SCENARIO's reality_bridge (the "Демо: перехід дороги" lesson).
	private static final String MINIMAL_BRIDGE_TEXT = "Remember to look both ways";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AudioAssetRepository audioAssetRepository;

	@Autowired
	private TtsProperties ttsProperties;

	private MockHttpSession parent;
	private UUID childId;
	private UUID minimalVersionId;
	private UUID allScenesVersionId;

	private MockHttpSession verifiedParent() throws Exception {
		String email = "stickers-" + UUID.randomUUID() + "@example.test";
		User user = new User(email, passwordEncoder.encode(PASSWORD), Role.PARENT, "Test", "uk");
		user.markEmailVerified();
		userRepository.save(user);
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	@BeforeEach
	void seedChildWithLessons() throws Exception {
		parent = verifiedParent();
		String body = mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(parent))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		childId = UUID.fromString(JsonPath.read(body, "$.childId"));
		minimalVersionId = UUID.fromString(JsonPath.read(body, "$.lessonVersionId"));
		allScenesVersionId = UUID.fromString(JsonPath.read(body, "$.allScenesLessonVersionId"));
	}

	private void play(UUID versionId, String result) throws Exception {
		String attemptId = JsonPath.read(mockMvc.perform(post("/api/children/" + childId + "/attempts").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(versionId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.attemptId");
		mockMvc.perform(post("/api/attempts/" + attemptId + "/complete").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\":\"%s\",\"score\":0.25}".formatted(result)))
				.andExpect(status().isOk());
	}

	private ResultActions stickers(MockHttpSession session) throws Exception {
		return mockMvc.perform(get("/api/children/" + childId + "/stickers").session(session));
	}

	@Test
	void noStickersYetShowsEveryAssignedLessonAsASilhouette() throws Exception {
		stickers(parent)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stickers.length()").value(0))
				.andExpect(jsonPath("$.topicsCompleted.length()").value(0))
				.andExpect(jsonPath("$.locked.length()").value(2))
				.andExpect(jsonPath("$.locked[0].topic").value("road_safety"));
	}

	@Test
	void abandonedLessonEarnsNothing() throws Exception {
		play(minimalVersionId, "abandoned");
		stickers(parent).andExpect(jsonPath("$.stickers.length()").value(0));
	}

	@Test
	void repeatedLessonKeepsOneStickerFromTheFirstCompletionWithoutAnyGrade() throws Exception {
		play(minimalVersionId, "completed");
		String firstEarnedAt = JsonPath.read(stickers(parent).andReturn().getResponse().getContentAsString(),
				"$.stickers[0].earnedAt");
		play(minimalVersionId, "completed");

		stickers(parent)
				.andExpect(jsonPath("$.stickers.length()").value(1))
				.andExpect(jsonPath("$.stickers[0].lessonVersionId").value(minimalVersionId.toString()))
				.andExpect(jsonPath("$.stickers[0].topic").value("road_safety"))
				.andExpect(jsonPath("$.stickers[0].earnedAt").value(firstEarnedAt))
				.andExpect(jsonPath("$.stickers[0].bridge.key").value("bridge1"))
				.andExpect(jsonPath("$.stickers[0].bridge.text").value(MINIMAL_BRIDGE_TEXT))
				.andExpect(jsonPath("$.stickers[0].score").doesNotExist())
				.andExpect(jsonPath("$.stickers[0].result").doesNotExist())
				.andExpect(jsonPath("$.locked.length()").value(1))
				.andExpect(jsonPath("$.topicsCompleted.length()").value(0));
	}

	@Test
	void bridgeCarriesItsAudioUrlOnceVoiced() throws Exception {
		play(minimalVersionId, "completed");
		stickers(parent).andExpect(jsonPath("$.stickers[0].bridge.audioUrl").doesNotExist());

		String hash = VoiceLines.hash(ttsProperties.voiceId(), MINIMAL_BRIDGE_TEXT);
		if (audioAssetRepository.findByTextHash(hash).isEmpty()) {
			audioAssetRepository.save(new AudioAsset(hash, MINIMAL_BRIDGE_TEXT, "stub", ttsProperties.voiceId(),
					"memory://" + hash, 900));
		}

		stickers(parent).andExpect(jsonPath("$.stickers[0].bridge.audioUrl").value(VoiceLines.audioUrl(hash)));
	}

	@Test
	void completingEveryAssignedLessonOfATopicEarnsTheTopicSticker() throws Exception {
		play(minimalVersionId, "completed");
		play(allScenesVersionId, "completed");

		stickers(parent)
				.andExpect(jsonPath("$.stickers.length()").value(2))
				.andExpect(jsonPath("$.locked.length()").value(0))
				.andExpect(jsonPath("$.topicsCompleted.length()").value(1))
				.andExpect(jsonPath("$.topicsCompleted[0]").value("road_safety"));
	}

	@Test
	void anotherParentGets404() throws Exception {
		stickers(verifiedParent()).andExpect(status().isNotFound());
	}

}
