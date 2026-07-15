package ua.kidlearn.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * No @Transactional here: mail is sent from an AFTER_COMMIT event listener,
 * which only fires on a real commit. Each test cleans up the users it
 * creates in @AfterEach (user_tokens cascade-delete with the user).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EmailVerificationAndPasswordResetTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;

	@Autowired
	private TokenGenerator tokenGenerator;

	@MockitoBean
	private AuthMailSender authMailSender;

	private final List<String> createdEmails = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		createdEmails.forEach(
				email -> userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(userRepository::delete));
	}

	private String uniqueEmail(String prefix) {
		String email = prefix + "-" + UUID.randomUUID() + "@example.test";
		createdEmails.add(email);
		return email;
	}

	private void register(String email) throws Exception {
		String body = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD);
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
	}

	@Test
	void verifyEmailHappyPathSetsEmailVerifiedAt() throws Exception {
		String email = uniqueEmail("verify-happy");
		register(email);

		ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(authMailSender, timeout(3000)).sendVerification(any(User.class), tokenCaptor.capture());

		mockMvc.perform(post("/api/auth/verify-email").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"%s\"}".formatted(tokenCaptor.getValue())))
				.andExpect(status().isOk());

		User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
		assertThat(user.isEmailVerified()).isTrue();
	}

	@Test
	void verifyEmailRejectsInvalidToken() throws Exception {
		mockMvc.perform(post("/api/auth/verify-email").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"not-a-real-token\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void verifyEmailRejectsExpiredToken() throws Exception {
		String email = uniqueEmail("verify-expired");
		register(email);
		User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();

		String rawToken = tokenGenerator.generateRawToken();
		userTokenRepository.save(new UserToken(user, TokenType.EMAIL_VERIFICATION, tokenGenerator.hash(rawToken),
				Instant.now().minusSeconds(10)));

		mockMvc.perform(post("/api/auth/verify-email").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"%s\"}".formatted(rawToken)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void verifyEmailRejectsAlreadyUsedToken() throws Exception {
		String email = uniqueEmail("verify-reused");
		register(email);

		ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(authMailSender, timeout(3000)).sendVerification(any(User.class), tokenCaptor.capture());
		String body = "{\"token\":\"%s\"}".formatted(tokenCaptor.getValue());

		mockMvc.perform(post("/api/auth/verify-email").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/verify-email").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void forgotPasswordReturnsOkForBothExistingAndUnknownEmailAndCreatesExactlyOneToken() throws Exception {
		String email = uniqueEmail("forgot");
		register(email);

		mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\"}".formatted(email)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"nobody-%s@example.test\"}".formatted(UUID.randomUUID())))
				.andExpect(status().isOk());

		User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
		verify(authMailSender, timeout(3000)).sendPasswordReset(any(User.class), any(String.class));
		assertThat(userTokenRepository.countByUserAndType(user, TokenType.PASSWORD_RESET)).isEqualTo(1);
	}

	@Test
	void resetPasswordChangesPasswordAndIsSingleUse() throws Exception {
		String email = uniqueEmail("reset");
		register(email);

		mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\"}".formatted(email)))
				.andExpect(status().isOk());

		ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
		verify(authMailSender, timeout(3000)).sendPasswordReset(any(User.class), tokenCaptor.capture());
		String rawToken = tokenCaptor.getValue();

		String newPassword = "brandnewpassword1";
		String resetBody = "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(rawToken, newPassword);

		mockMvc.perform(post("/api/auth/reset-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(resetBody))
				.andExpect(status().isOk());

		mockMvc.perform(formLogin().user(email).password(PASSWORD)).andExpect(unauthenticated());
		mockMvc.perform(formLogin().user(email).password(newPassword)).andExpect(authenticated());

		mockMvc.perform(post("/api/auth/reset-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(resetBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	void resetPasswordRejectsShortPassword() throws Exception {
		mockMvc.perform(post("/api/auth/reset-password").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"whatever\",\"newPassword\":\"short\"}"))
				.andExpect(status().isBadRequest());
	}

}
