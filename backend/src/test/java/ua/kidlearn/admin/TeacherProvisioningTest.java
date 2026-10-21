package ua.kidlearn.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditEvent;
import ua.kidlearn.audit.AuditLogRepository;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.UserTokenRepository;
import ua.kidlearn.groups.GroupRepository;
import ua.kidlearn.support.WebIntegrationTest;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Invite-based teacher provisioning end to end. Deliberately NOT @Transactional: the invite email
 * and the audit rows are both sent AFTER_COMMIT, which never happens in a rolled-back test
 * transaction. The mail sender is mocked (no SMTP in CI) and captures the raw token exactly as the
 * email would carry it. Token expiry is covered separately in TeacherInviteExpiryTest (it needs
 * a test Clock, which doesn't mix with the system-clock TOTP helper used here). Every committed row is removed in @AfterEach; admins are soft-deleted like
 * in AuditLogTest so they never satisfy "an ADMIN exists" for later suites.
 */
class TeacherProvisioningTest extends WebIntegrationTest {

	private static final String PASSWORD = "supersecret1";
	private static final String TEACHER_PASSWORD = "teacher-chosen-pass-1";
	private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserTokenRepository userTokenRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private final List<User> createdAdmins = new ArrayList<>();
	private final List<UUID> createdUsers = new ArrayList<>();

	@AfterEach
	void cleanUpCommittedData() {
		for (UUID userId : createdUsers) {
			groupRepository.deleteAll(groupRepository.findByTeacherId(userId));
		}
		// user_tokens cascade on delete; audit_log.actor_id is ON DELETE SET NULL and the target has
		// no FK, so the audit trail itself survives (by design).
		userRepository.deleteAllById(createdUsers);
		createdUsers.clear();
		createdAdmins.forEach(user -> user.markDeleted(Instant.now()));
		userRepository.saveAll(createdAdmins);
		createdAdmins.clear();
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private MockHttpSession loginAsNewAdmin() throws Exception {
		String email = uniqueEmail("admin");
		createdAdmins.add(userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk")));
		MockHttpSession session = login(email, PASSWORD);
		AdminTwoFactorTestSupport.completeSetup(mockMvc, session);
		return session;
	}

	private UUID adminId() {
		return createdAdmins.get(createdAdmins.size() - 1).getId();
	}

	private MockHttpSession loginAs(Role role) throws Exception {
		String email = uniqueEmail(role.name().toLowerCase());
		createdUsers.add(userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), role, "User", "uk")).getId());
		return login(email, PASSWORD);
	}

	private MockHttpSession login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(password))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private void expectLoginRefused(String email, String password) throws Exception {
		mockMvc.perform(formLogin().user(email).password(password)).andExpect(redirectedUrl("/login?error"));
	}

	private ResultActions invite(MockHttpSession admin, String email, String displayName) throws Exception {
		return mockMvc.perform(post("/api/admin/teachers").with(csrf())
				.session(admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"displayName\":\"%s\"}".formatted(email, displayName)));
	}

	/** Invites a teacher and returns {teacherId, rawToken-from-the-email}. */
	private String[] inviteAndCaptureToken(MockHttpSession admin, String email) throws Exception {
		MvcResult result = invite(admin, email, "Олена Вчителька").andExpect(status().isCreated()).andReturn();
		String teacherId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		createdUsers.add(UUID.fromString(teacherId));
		return new String[] { teacherId, awaitInviteToken(teacherId) };
	}

	private String awaitInviteToken(String teacherId) {
		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		verify(authMailSender, timeout(ASYNC_TIMEOUT.toMillis()).atLeastOnce())
				.sendTeacherInvite(argThat(u -> u.getId().toString().equals(teacherId)), token.capture());
		return token.getValue();
	}

	private ResultActions accept(String token, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/accept-invite").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, password)));
	}

	private String teacherStatus(MockHttpSession admin, String teacherId) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/admin/teachers").session(admin)).andExpect(status().isOk()).andReturn();
		List<String> statuses = JsonPath.read(result.getResponse().getContentAsString(),
				"$[?(@.id == '" + teacherId + "')].status");
		return statuses.isEmpty() ? null : statuses.get(0);
	}

	private List<AuditEvent> awaitAudit(String action, UUID targetId) {
		return Awaitility.await()
				.atMost(ASYNC_TIMEOUT)
				.until(() -> auditLogRepository.findByActorIdAndTargetTypeAndTargetId(adminId(), AuditTargetType.USER, targetId)
						.stream()
						.filter(e -> e.getAction().equals(action))
						.toList(), rows -> !rows.isEmpty());
	}

	@Test
	void invitedTeacherSetsOwnPasswordThenLogsInAndSeesGroups() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		String email = uniqueEmail("teacher");

		MvcResult created = invite(admin, "  " + email.toUpperCase() + " ", "Олена Вчителька")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email)) // trimmed + lowercased
				.andExpect(jsonPath("$.displayName").value("Олена Вчителька"))
				.andExpect(jsonPath("$.status").value("invited"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();
		String teacherId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		createdUsers.add(UUID.fromString(teacherId));
		String rawToken = awaitInviteToken(teacherId);

		// Only the hash is stored; the raw token exists only in the email.
		assertThat(userTokenRepository.findAll()).noneMatch(t -> t.getTokenHash().equals(rawToken));
		User invited = userRepository.findById(UUID.fromString(teacherId)).orElseThrow();
		assertThat(invited.getRole()).isEqualTo(Role.TEACHER);
		assertThat(invited.isEmailVerified()).isFalse();
		assertThat(teacherStatus(admin, teacherId)).isEqualTo("invited");
		expectLoginRefused(email, TEACHER_PASSWORD);

		accept(rawToken, TEACHER_PASSWORD).andExpect(status().isOk());

		assertThat(userRepository.findById(UUID.fromString(teacherId)).orElseThrow().isEmailVerified()).isTrue();
		assertThat(teacherStatus(admin, teacherId)).isEqualTo("active");
		MockHttpSession teacher = login(email, TEACHER_PASSWORD);
		mockMvc.perform(get("/api/groups").session(teacher)).andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/me").session(teacher)).andExpect(jsonPath("$.role").value("TEACHER"));
		assertThat(awaitAudit(AuditAction.INVITE_TEACHER, UUID.fromString(teacherId))).hasSize(1);
	}

	@Test
	void inviteTokenWorksOnlyOnce() throws Exception {
		String[] invite = inviteAndCaptureToken(loginAsNewAdmin(), uniqueEmail("teacher"));

		accept(invite[1], TEACHER_PASSWORD).andExpect(status().isOk());
		accept(invite[1], "another-password-2").andExpect(status().isBadRequest());
	}

	@Test
	void resendIssuesAFreshTokenThatInvalidatesTheOldOne() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		String[] invite = inviteAndCaptureToken(admin, uniqueEmail("teacher"));
		assertThat(teacherStatus(admin, invite[0])).isEqualTo("invited");

		// Admin re-sends: a new token arrives; every earlier unused one is dead.
		org.mockito.Mockito.clearInvocations(authMailSender);
		mockMvc.perform(post("/api/admin/teachers/" + invite[0] + "/resend-invite").with(csrf()).session(admin))
				.andExpect(status().isOk());
		String freshToken = awaitInviteToken(invite[0]);
		assertThat(freshToken).isNotEqualTo(invite[1]);
		accept(invite[1], TEACHER_PASSWORD).andExpect(status().isBadRequest());
		accept(freshToken, TEACHER_PASSWORD).andExpect(status().isOk());
		assertThat(awaitAudit(AuditAction.RESEND_TEACHER_INVITE, UUID.fromString(invite[0]))).hasSize(1);

		// Accepted invitations can't be re-sent.
		mockMvc.perform(post("/api/admin/teachers/" + invite[0] + "/resend-invite").with(csrf()).session(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVITE_ALREADY_ACCEPTED"));
	}

	@Test
	void acceptInviteEnforcesRegistrationPasswordRulesAndHidesWhyATokenFailed() throws Exception {
		String[] invite = inviteAndCaptureToken(loginAsNewAdmin(), uniqueEmail("teacher"));

		accept(invite[1], "short").andExpect(status().isBadRequest());
		accept("not-a-real-token", TEACHER_PASSWORD).andExpect(status().isBadRequest());
		// The short-password attempt didn't burn the token.
		accept(invite[1], TEACHER_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void emailAlreadyUsedByAnyAccountIs409WithoutRevealingTheRole() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		String parentEmail = uniqueEmail("parent");
		createdUsers.add(userRepository.save(new User(parentEmail, passwordEncoder.encode(PASSWORD), Role.PARENT, "P", "uk")).getId());

		MvcResult result = invite(admin, parentEmail.toUpperCase(), "Someone")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).doesNotContain("PARENT").doesNotContain("parent\"");
	}

	@Test
	void parentsAndTeachersCannotUseTeacherAdministration() throws Exception {
		String someId = UUID.randomUUID().toString();
		for (Role role : List.of(Role.PARENT, Role.TEACHER)) {
			MockHttpSession session = loginAs(role);
			mockMvc.perform(get("/api/admin/teachers").session(session)).andExpect(status().isForbidden());
			invite(session, uniqueEmail("x"), "X").andExpect(status().isForbidden());
			mockMvc.perform(post("/api/admin/teachers/" + someId + "/resend-invite").with(csrf()).session(session))
					.andExpect(status().isForbidden());
			mockMvc.perform(post("/api/admin/teachers/" + someId + "/deactivate").with(csrf()).session(session))
					.andExpect(status().isForbidden());
		}
	}

	@Test
	void deactivatedTeacherLosesOpenSessionCannotLogInAndKeepsGroups() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		String email = uniqueEmail("teacher");
		String[] invite = inviteAndCaptureToken(admin, email);
		accept(invite[1], TEACHER_PASSWORD).andExpect(status().isOk());
		MockHttpSession teacher = login(email, TEACHER_PASSWORD);
		mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacher)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Група Сонечко\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/admin/teachers").session(admin))
				.andExpect(jsonPath("$[?(@.id == '" + invite[0] + "')].groupCount").value(org.hamcrest.Matchers.contains(1)));

		mockMvc.perform(post("/api/admin/teachers/" + invite[0] + "/deactivate").with(csrf()).session(admin))
				.andExpect(status().isOk());

		// The session opened before deactivation is cut off on its very next request…
		mockMvc.perform(get("/api/groups").session(teacher)).andExpect(status().isUnauthorized());
		// …and a fresh login is refused.
		expectLoginRefused(email, TEACHER_PASSWORD);
		// Their group (and with it the children's assignments/results) is untouched.
		assertThat(groupRepository.findByTeacherId(UUID.fromString(invite[0]))).hasSize(1);
		assertThat(teacherStatus(admin, invite[0])).isEqualTo("deactivated");
		assertThat(awaitAudit(AuditAction.DEACTIVATE_TEACHER, UUID.fromString(invite[0]))).hasSize(1);
	}

	@Test
	void pendingInviteOfDeactivatedTeacherNoLongerWorks() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		String[] invite = inviteAndCaptureToken(admin, uniqueEmail("teacher"));

		mockMvc.perform(post("/api/admin/teachers/" + invite[0] + "/deactivate").with(csrf()).session(admin))
				.andExpect(status().isOk());

		accept(invite[1], TEACHER_PASSWORD).andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/admin/teachers/" + invite[0] + "/resend-invite").with(csrf()).session(admin))
				.andExpect(status().isNotFound());
	}

	@Test
	void deactivateOnlyAppliesToTeachers() throws Exception {
		MockHttpSession admin = loginAsNewAdmin();
		UUID parentId = userRepository.save(new User(uniqueEmail("parent"), passwordEncoder.encode(PASSWORD), Role.PARENT,
				"P", "uk")).getId();
		createdUsers.add(parentId);

		mockMvc.perform(post("/api/admin/teachers/" + parentId + "/deactivate").with(csrf()).session(admin))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/admin/teachers/" + adminId() + "/deactivate").with(csrf()).session(admin))
				.andExpect(status().isNotFound());
		assertThat(userRepository.findById(parentId).orElseThrow().isDeleted()).isFalse();
		verify(authMailSender, org.mockito.Mockito.never()).sendTeacherInvite(argThat(u -> u.getId().equals(parentId)), any());
	}

}
