package ua.kidlearn.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Deliberately NOT @Transactional: the audit write happens via
 * @TransactionalEventListener(AFTER_COMMIT), which never fires inside a test transaction that
 * always rolls back (Spring never actually commits it). So these writes are real, and every test
 * uses fully unique identifiers to avoid cross-test collisions rather than relying on rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuditLogTest {

	private static final String PASSWORD = "supersecret1";

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

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private User registerAdmin(String email) {
		return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
	}

	private User registerTeacher(String email) {
		return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.TEACHER, "Teacher", "uk"));
	}

	private User registerVerifiedParent(String email) throws Exception {
		String body = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD);
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
		User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
		user.markEmailVerified();
		return userRepository.save(user);
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private UUID createChild(MockHttpSession parentSession) throws Exception {
		String body = "{\"displayName\":\"Kid\",\"birthYear\":%d,\"relation\":\"mother\"}"
				.formatted(Year.now().getValue() - 3);
		MvcResult result = mockMvc.perform(post("/api/children").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID createGroup(MockHttpSession teacherSession) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class A\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID createLesson(MockHttpSession adminSession) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID generateAutoValidatedVersion(MockHttpSession adminSession, UUID lessonId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/generate").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(GENERATION_REQUEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("auto_validated"))
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.versionId"));
	}

	private List<AuditEvent> awaitAuditRows(UUID actorId, String targetType, UUID targetId) {
		Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(50)).untilAsserted(
				() -> assertThat(auditLogRepository.findByActorIdAndTargetTypeAndTargetId(actorId, targetType, targetId))
						.isNotEmpty());
		return auditLogRepository.findByActorIdAndTargetTypeAndTargetId(actorId, targetType, targetId);
	}

	@Test
	void parentViewingChildHistoryWritesExactlyOneAuditRow() throws Exception {
		String parentEmail = uniqueEmail("audit-parent");
		User parent = registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createChild(parentSession);

		mockMvc.perform(get("/api/children/" + childId + "/history").session(parentSession))
				.andExpect(status().isOk());

		List<AuditEvent> rows = awaitAuditRows(parent.getId(), AuditTargetType.CHILD, childId);
		assertThat(rows).hasSize(1);
		AuditEvent row = rows.get(0);
		assertThat(row.getAction()).isEqualTo(AuditAction.VIEW_CHILD_HISTORY);
		assertThat(row.getActorId()).isEqualTo(parent.getId());
		assertThat(row.getActorRole()).isEqualTo("PARENT");
		assertThat(row.getTargetId()).isEqualTo(childId);
	}

	@Test
	void deletingChildWritesAuditRowThatOutlivesTheChild() throws Exception {
		String parentEmail = uniqueEmail("audit-parent");
		User parent = registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createChild(parentSession);

		mockMvc.perform(delete("/api/children/" + childId).with(csrf()).session(parentSession))
				.andExpect(status().isNoContent());

		List<AuditEvent> rows = awaitAuditRows(parent.getId(), AuditTargetType.CHILD, childId);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getAction()).isEqualTo(AuditAction.DELETE_CHILD);
		// The row above being findable at all, after the delete, is the proof: the child row
		// itself is gone (hard-deleted), but audit_log has no FK/cascade tying it to children.
	}

	@Test
	void teacherViewingGroupResultsWritesAuditRow() throws Exception {
		String teacherEmail = uniqueEmail("audit-teacher");
		User teacher = registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		UUID groupId = createGroup(teacherSession);

		mockMvc.perform(get("/api/groups/" + groupId + "/results").session(teacherSession))
				.andExpect(status().isOk());

		List<AuditEvent> rows = awaitAuditRows(teacher.getId(), AuditTargetType.GROUP, groupId);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getAction()).isEqualTo(AuditAction.VIEW_GROUP_RESULTS);
	}

	@Test
	void approveAndPublishEachWriteTheirAuditRow() throws Exception {
		String adminEmail = uniqueEmail("audit-admin");
		User admin = registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID lessonId = createLesson(adminSession);
		UUID versionId = generateAutoValidatedVersion(adminSession, lessonId);

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());
		List<AuditEvent> approveRows = awaitAuditRows(admin.getId(), AuditTargetType.LESSON_VERSION, versionId);
		assertThat(approveRows).extracting(AuditEvent::getAction).contains(AuditAction.APPROVE_VERSION);

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());
		Awaitility.await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(50)).untilAsserted(() -> {
			List<AuditEvent> rows = auditLogRepository.findByActorIdAndTargetTypeAndTargetId(admin.getId(),
					AuditTargetType.LESSON_VERSION, versionId);
			assertThat(rows).extracting(AuditEvent::getAction).contains(AuditAction.PUBLISH_VERSION);
		});
	}

	@Test
	void adminCanReadAuditLogButTeacherCannot() throws Exception {
		String adminEmail = uniqueEmail("audit-admin");
		User admin = registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		UUID lessonId = createLesson(adminSession);
		UUID versionId = generateAutoValidatedVersion(adminSession, lessonId);
		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/approve").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());
		awaitAuditRows(admin.getId(), AuditTargetType.LESSON_VERSION, versionId);

		mockMvc.perform(get("/api/admin/audit").session(adminSession)
						.param("targetType", AuditTargetType.LESSON_VERSION)
						.param("targetId", versionId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].action").value(AuditAction.APPROVE_VERSION));

		String teacherEmail = uniqueEmail("audit-teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		mockMvc.perform(get("/api/admin/audit").session(teacherSession))
				.andExpect(status().isForbidden());
	}

}
