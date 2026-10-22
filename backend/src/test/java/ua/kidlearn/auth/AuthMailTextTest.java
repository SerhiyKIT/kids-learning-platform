package ua.kidlearn.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ua.kidlearn.config.AppProperties;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;

/** QA2-001: the emails adults get are in Ukrainian, like the rest of the platform. No Spring context. */
class AuthMailTextTest {

	private final JavaMailSender mailSender = mock(JavaMailSender.class);
	private final SmtpAuthMailSender sender = new SmtpAuthMailSender(mailSender,
			new AppProperties("https://kids.example.test", "no-reply@example.test", new AppProperties.Security(false)));
	private final User user = new User("mama@example.test", "hash", Role.PARENT, "Оксана", "uk");

	private SimpleMailMessage sent(Consumer<SmtpAuthMailSender> send) {
		send.accept(sender);
		ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(message.capture());
		return message.getValue();
	}

	@Test
	void emailVerification() {
		assertUkrainian(sent(s -> s.sendVerification(user, "t1")), "https://kids.example.test/verify-email?token=t1");
	}

	@Test
	void passwordReset() {
		assertUkrainian(sent(s -> s.sendPasswordReset(user, "t2")), "https://kids.example.test/reset-password?token=t2");
	}

	@Test
	void teacherInvite() {
		assertUkrainian(sent(s -> s.sendTeacherInvite(user, "t3")), "https://kids.example.test/accept-invite?token=t3");
	}

	@Test
	void alreadyRegistered() {
		assertUkrainian(sent(s -> s.sendAlreadyRegistered(user)), "https://kids.example.test/forgot-password");
	}

	private static void assertUkrainian(SimpleMailMessage message, String link) {
		assertThat(message.getSubject()).containsPattern("[А-ЯІЇЄҐа-яіїєґ]").contains("Безпечн");
		assertThat(message.getText()).contains(link).containsPattern("[А-ЯІЇЄҐа-яіїєґ]{4,}")
				.doesNotContain("Verify", "Reset your", "invited", "Hello");
	}

}
