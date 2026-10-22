package ua.kidlearn.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.support.WebIntegrationTest;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;

/**
 * One account per email whatever the case or surrounding spaces: registration, login, forgot
 * password and teacher invites all go through EmailAddress.normalize.
 */
@Transactional
class EmailNormalizationTest extends WebIntegrationTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ua.kidlearn.auth.UserTokenRepository userTokenRepository;

	private static String local() {
		return "mama-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private ResultActions register(String email) throws Exception {
		return mockMvc.perform(post("/api/auth/register").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Мама\"}".formatted(email, PASSWORD)));
	}

	@Test
	void mixedCaseRegistrationIsStoredNormalizedAndLogsInWithAnyCase() throws Exception {
		String name = local();
		register("  " + name.toUpperCase() + "@Gmail.COM ").andExpect(status().isAccepted());

		MvcResult login = mockMvc.perform(formLogin().user(name + "@gmail.com").password(PASSWORD)).andReturn();
		assertThat(login.getResponse().getRedirectedUrl()).isEqualTo("/");
		mockMvc.perform(get("/api/auth/me").session((MockHttpSession) login.getRequest().getSession(false)))
				.andExpect(jsonPath("$.email").value(name + "@gmail.com"));
		MvcResult upper = mockMvc.perform(formLogin().user(" " + name.toUpperCase() + "@GMAIL.com").password(PASSWORD)).andReturn();
		assertThat(upper.getResponse().getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void theSameEmailInAnotherCaseIsTaken() throws Exception {
		String name = local();
		register(name + "@gmail.com").andExpect(status().isAccepted());
		// Taken in another case: the same answer as for a new address (QA1-006), no second account.
		register(name.toUpperCase() + "@GMAIL.com").andExpect(status().isAccepted());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(email) = ?", Integer.class,
				name + "@gmail.com")).isEqualTo(1);
	}

	@Test
	void forgotPasswordFindsTheAccountInAnyCaseAndAnswersLikeForAnUnknownEmail() throws Exception {
		String name = local();
		register(name + "@gmail.com").andExpect(status().isAccepted());
		UUID userId = userRepository.findActiveByEmail(name + "@gmail.com").orElseThrow().getId();

		String known = mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\"}".formatted(name.toUpperCase() + "@Gmail.com")))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String unknown = mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"nobody-%s@gmail.com\"}".formatted(UUID.randomUUID())))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		assertThat(known).isEqualTo(unknown);
		// Through JPA (flushes the request's pending insert first), not raw SQL.
		assertThat(userTokenRepository.countByUserAndType(userRepository.findById(userId).orElseThrow(),
				ua.kidlearn.auth.TokenType.PASSWORD_RESET)).isEqualTo(1);
	}

	@Test
	void invitingATeacherWithAnExistingEmailInAnotherCaseIsEmailTaken() throws Exception {
		String name = local();
		register(name + "@gmail.com").andExpect(status().isAccepted());
		String adminEmail = "admin-" + UUID.randomUUID() + "@example.test";
		userRepository.saveAndFlush(new User(adminEmail, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
		MockHttpSession admin = (MockHttpSession) mockMvc.perform(formLogin().user(adminEmail).password(PASSWORD))
				.andReturn().getRequest().getSession(false);
		AdminTwoFactorTestSupport.completeSetup(mockMvc, admin);

		mockMvc.perform(post("/api/admin/teachers").with(csrf()).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\" %s@GMAIL.COM\",\"displayName\":\"Вчителька\"}".formatted(name.toUpperCase())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
	}

	@Test
	void theDatabaseRefusesANonNormalizedEmail() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
						"INSERT INTO users (email, role, display_name) VALUES (?, 'parent', 'X')", "Upper-" + UUID.randomUUID() + "@x.org"))
				.hasMessageContaining("chk_users_email_normalized");
	}

}
