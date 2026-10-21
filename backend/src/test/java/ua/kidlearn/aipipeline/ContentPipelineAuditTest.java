package ua.kidlearn.aipipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditEvent;
import ua.kidlearn.audit.AuditLogRepository;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.support.InMemoryAudioWebIntegrationTest;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * GENERATE_VERSION / VOICE_VERSION audit rows, end to end on the stub providers. Deliberately NOT
 * @Transactional, same as AuditLogTest: the audit row is written AFTER_COMMIT, which never fires in
 * a rolled-back test transaction. Everything committed here (admins, lessons, versions, audio
 * cache rows) is cleaned up in @AfterEach; audit rows themselves are kept by design (the log must
 * outlive its targets — see V5__audit_log.sql).
 */
class ContentPipelineAuditTest extends InMemoryAudioWebIntegrationTest {

	private static final String PASSWORD = "supersecret1";
	private static final Duration AUDIT_TIMEOUT = Duration.ofSeconds(10);
	private static final String GENERATION_REQUEST_BODY = """
			{"topic":"road_safety","patternIds":["1.6"],"ageBand":"age_5_6",
			 "title":"Crossing the street","learningGoal":"Look both ways before crossing"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonVersionRepository lessonVersionRepository;

	@Autowired
	private AudioAssetRepository audioAssetRepository;

	private final List<User> createdAdmins = new ArrayList<>();
	private final List<UUID> createdLessons = new ArrayList<>();
	private final Set<String> voicedTexts = new HashSet<>();

	@AfterEach
	void cleanUpCommittedData() {
		// Versions first (lesson_versions.lesson_id -> lessons), none of them published here.
		lessonVersionRepository.deleteAll(lessonVersionRepository.findByLessonIdIn(createdLessons));
		lessonRepository.deleteAllById(createdLessons);
		createdLessons.clear();
		audioAssetRepository.deleteAll(
				audioAssetRepository.findAll().stream().filter(a -> voicedTexts.contains(a.getText())).toList());
		voicedTexts.clear();
		// Soft-deleted, not hard-deleted: approved_by references them (see AuditLogTest).
		createdAdmins.forEach(user -> user.markDeleted(Instant.now()));
		userRepository.saveAll(createdAdmins);
		createdAdmins.clear();
	}

	private MockHttpSession loginAsNewAdmin() throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@example.test";
		createdAdmins.add(userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk")));
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		AdminTwoFactorTestSupport.completeSetup(mockMvc, session);
		return session;
	}

	private UUID adminId() {
		return createdAdmins.get(createdAdmins.size() - 1).getId();
	}

	private UUID createLesson(MockHttpSession adminSession, String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"%s\"}".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		UUID id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
		createdLessons.add(id);
		return id;
	}

	private UUID generate(MockHttpSession adminSession, UUID lessonId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isOk())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.versionId"));
	}

	private List<AuditEvent> awaitAudit(String targetType, UUID targetId, String action) {
		return Awaitility.await()
				.atMost(AUDIT_TIMEOUT)
				.until(() -> auditLogRepository.findByActorIdAndTargetTypeAndTargetId(adminId(), targetType, targetId)
						.stream()
						.filter(e -> e.getAction().equals(action))
						.toList(), rows -> !rows.isEmpty());
	}

	@Test
	void generationIsAuditedAgainstTheCreatedVersion() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID versionId = generate(admin, createLesson(admin, "Generated " + UUID.randomUUID()));

		List<AuditEvent> rows = awaitAudit(AuditTargetType.LESSON_VERSION, versionId, AuditAction.GENERATE_VERSION);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getActorRole()).isEqualTo("ADMIN");
		assertThat(rows.get(0).getClientIp()).isNotBlank();
	}

	@Test
	void failedGenerationIsAuditedAgainstTheLesson() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID lessonId = createLesson(admin, "Failing " + UUID.randomUUID());
		doThrow(new LlmGenerationException(LlmGenerationException.Reason.UPSTREAM_ERROR, "test outage"))
				.when(llmProvider).generate(any());

		mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isBadGateway());

		assertThat(awaitAudit(AuditTargetType.LESSON, lessonId, AuditAction.GENERATE_VERSION)).hasSize(1);
	}

	@Test
	void voicingIsAuditedAndTheAuditLogCanBeFilteredByAction() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID versionId = generate(admin, createLesson(admin, "Voiced " + UUID.randomUUID()));
		voicedTexts.addAll(List.of("Remember to look both ways", "Watch me cross the street safely"));
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf()).session(admin))
				.andExpect(status().isOk());

		assertThat(awaitAudit(AuditTargetType.LESSON_VERSION, versionId, AuditAction.VOICE_VERSION)).hasSize(1);
		mockMvc.perform(get("/api/admin/audit").session(admin)
						.param("action", AuditAction.VOICE_VERSION)
						.param("targetId", versionId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].action").value(AuditAction.VOICE_VERSION))
				.andExpect(jsonPath("$[0].targetType").value(AuditTargetType.LESSON_VERSION));
	}

	@Test
	void voicingAnUnmoderatedVersionIsRejectedAndNotAudited() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID versionId = generate(admin, createLesson(admin, "Unmoderated " + UUID.randomUUID()));

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/voice").with(csrf()).session(admin))
				.andExpect(status().isConflict());

		// The generation row proves the async writer has caught up for this actor; no voice row beside it.
		awaitAudit(AuditTargetType.LESSON_VERSION, versionId, AuditAction.GENERATE_VERSION);
		assertThat(auditLogRepository.findByActorIdAndTargetTypeAndTargetId(adminId(), AuditTargetType.LESSON_VERSION,
				versionId)).extracting(AuditEvent::getAction).doesNotContain(AuditAction.VOICE_VERSION);
	}

	@Test
	void lessonListIsNewestFirst() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		String olderTitle = "Older " + UUID.randomUUID();
		String newerTitle = "Newer " + UUID.randomUUID();
		createLesson(admin, olderTitle);
		createLesson(admin, newerTitle); // separate request => separate transaction => later created_at

		MvcResult result = mockMvc.perform(get("/api/admin/lessons").session(admin))
				.andExpect(status().isOk())
				.andReturn();
		List<String> titles = JsonPath.read(result.getResponse().getContentAsString(), "$[*].title");
		assertThat(titles.indexOf(newerTitle)).isGreaterThanOrEqualTo(0).isLessThan(titles.indexOf(olderTitle));
	}

}
