package ua.kidlearn.twofa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.ratelimit.MutableClockTestConfig;
import ua.kidlearn.ratelimit.MutableTestClock;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Covers mandatory admin 2FA end-to-end: setup, enable (with backup codes), the enforced
 * two-step login gate, backup-code single-use, and rate limiting. Deterministic via the shared
 * test Clock (also used by RateLimiter) — no sleeps; TOTP codes are computed in-test with the
 * same secret + time index the server would use. Each test uses its own unique email/IP so the
 * shared in-memory RateLimiter singleton never leaks counts between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(MutableClockTestConfig.class)
@Transactional
class TwoFactorFlowTest {

	private static final String PASSWORD = "supersecret1";
	private static final DefaultCodeGenerator CODE_GENERATOR = new DefaultCodeGenerator();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MutableTestClock testClock;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private static String uniqueIp() {
		return "ip-" + UUID.randomUUID();
	}

	private void createAdmin(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
	}

	private void createTeacher(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.TEACHER, "Teacher", "uk"));
	}

	private void createVerifiedParent(String email) {
		User parent = new User(email, passwordEncoder.encode(PASSWORD), Role.PARENT, "Parent", "uk");
		parent.markEmailVerified();
		userRepository.save(parent);
	}

	private MockHttpSession login(String email, String ip) throws Exception {
		MvcResult result = mockMvc.perform(post("/login").with(csrf())
						.header("X-Forwarded-For", ip)
						.param("username", email)
						.param("password", PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private String currentTotpCode(String secretBase32) throws CodeGenerationException {
		long timeIndex = testClock.instant().getEpochSecond() / 30;
		return CODE_GENERATOR.generate(secretBase32, timeIndex);
	}

	private String setupTwoFactor(MockHttpSession session, String ip) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/admin/2fa/setup").with(csrf()).session(session)
						.header("X-Forwarded-For", ip))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secretBase32").exists())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.secretBase32");
	}

	private ResultActions enable(MockHttpSession session, String ip, String code) throws Exception {
		return mockMvc.perform(post("/api/admin/2fa/enable").with(csrf()).session(session)
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"%s\"}".formatted(code)));
	}

	private ResultActions verify(MockHttpSession session, String ip, String code) throws Exception {
		return mockMvc.perform(post("/api/admin/2fa/verify").with(csrf()).session(session)
				.header("X-Forwarded-For", ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"%s\"}".formatted(code)));
	}

	private ResultActions callProtectedAdminApi(MockHttpSession session) throws Exception {
		return mockMvc.perform(get("/api/admin/audit").session(session));
	}

	@Test
	void setupThenEnableWithValidCodeEnablesTwoFaAndReturnsBackupCodesOnce() throws Exception {
		String email = uniqueEmail("admin-2fa-enable");
		String ip = uniqueIp();
		createAdmin(email);
		MockHttpSession session = login(email, ip);

		String secret = setupTwoFactor(session, ip);
		String code = currentTotpCode(secret);

		MvcResult result = enable(session, ip, code)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.backupCodes", org.hamcrest.Matchers.hasSize(10)))
				.andReturn();
		List<String> backupCodes = JsonPath.read(result.getResponse().getContentAsString(), "$.backupCodes");
		assertThat(backupCodes).hasSize(10);

		User admin = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
		assertThat(admin.isTotpEnabled()).isTrue();
	}

	@Test
	void freshLoginAfterEnablingLandsInPreTwoFaUntilVerified() throws Exception {
		String email = uniqueEmail("admin-2fa-fresh");
		String setupIp = uniqueIp();
		createAdmin(email);
		MockHttpSession setupSession = login(email, setupIp);
		String secret = setupTwoFactor(setupSession, setupIp);
		enable(setupSession, setupIp, currentTotpCode(secret)).andExpect(status().isCreated());

		String loginIp = uniqueIp();
		MockHttpSession freshSession = login(email, loginIp);

		callProtectedAdminApi(freshSession).andExpect(status().isForbidden());

		verify(freshSession, loginIp, currentTotpCode(secret)).andExpect(status().isOk());

		callProtectedAdminApi(freshSession).andExpect(status().isOk());
	}

	@Test
	void backupCodeWorksExactlyOnce() throws Exception {
		String email = uniqueEmail("admin-2fa-backup");
		String setupIp = uniqueIp();
		createAdmin(email);
		MockHttpSession setupSession = login(email, setupIp);
		String secret = setupTwoFactor(setupSession, setupIp);
		MvcResult enableResult = enable(setupSession, setupIp, currentTotpCode(secret))
				.andExpect(status().isCreated())
				.andReturn();
		List<String> backupCodes = JsonPath.read(enableResult.getResponse().getContentAsString(), "$.backupCodes");
		String backupCode = backupCodes.get(0);

		String firstUseIp = uniqueIp();
		MockHttpSession firstSession = login(email, firstUseIp);
		verify(firstSession, firstUseIp, backupCode).andExpect(status().isOk());
		callProtectedAdminApi(firstSession).andExpect(status().isOk());

		String secondUseIp = uniqueIp();
		MockHttpSession secondSession = login(email, secondUseIp);
		verify(secondSession, secondUseIp, backupCode).andExpect(status().isBadRequest());
	}

	@Test
	void wrongCodeAtVerifyIsRejectedThenRateLimited() throws Exception {
		String email = uniqueEmail("admin-2fa-wrong");
		String setupIp = uniqueIp();
		createAdmin(email);
		MockHttpSession setupSession = login(email, setupIp);
		String secret = setupTwoFactor(setupSession, setupIp);
		enable(setupSession, setupIp, currentTotpCode(secret)).andExpect(status().isCreated());

		String loginIp = uniqueIp();
		MockHttpSession session = login(email, loginIp);

		for (int i = 0; i < 5; i++) {
			verify(session, loginIp, "000000").andExpect(status().isBadRequest());
		}
		verify(session, loginIp, "000000")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
	}

	@Test
	void adminWithoutTwoFaSetupIsBlockedFromAdminApisButCanReachSetup() throws Exception {
		String email = uniqueEmail("admin-2fa-notsetup");
		String ip = uniqueIp();
		createAdmin(email);
		MockHttpSession session = login(email, ip);

		callProtectedAdminApi(session).andExpect(status().isForbidden());

		mockMvc.perform(post("/api/admin/2fa/setup").with(csrf()).session(session).header("X-Forwarded-For", ip))
				.andExpect(status().isOk());
	}

	@Test
	void parentsAndTeachersAreUnaffectedByTwoFa() throws Exception {
		String parentEmail = uniqueEmail("parent-no-2fa");
		String parentIp = uniqueIp();
		createVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail, parentIp);
		mockMvc.perform(post("/api/children").with(csrf()).session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Kid\",\"birthYear\":2020,\"relation\":\"mother\"}"))
				.andExpect(status().isCreated());

		String teacherEmail = uniqueEmail("teacher-no-2fa");
		String teacherIp = uniqueIp();
		createTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail, teacherIp);
		mockMvc.perform(post("/api/groups").with(csrf()).session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class A\"}"))
				.andExpect(status().isCreated());
	}

}
