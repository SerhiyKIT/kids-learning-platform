package ua.kidlearn.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ua.kidlearn.config.AppProperties;
import ua.kidlearn.users.User;

@Component
class SmtpAuthMailSender implements AuthMailSender {

	private final JavaMailSender mailSender;
	private final AppProperties appProperties;

	SmtpAuthMailSender(JavaMailSender mailSender, AppProperties appProperties) {
		this.mailSender = mailSender;
		this.appProperties = appProperties;
	}

	@Override
	public void sendVerification(User user, String rawToken) {
		String link = appProperties.baseUrl() + "/verify-email?token=" + rawToken;
		send(user.getEmail(), "Confirm your email", "Verify your email: " + link);
	}

	@Override
	public void sendPasswordReset(User user, String rawToken) {
		String link = appProperties.baseUrl() + "/reset-password?token=" + rawToken;
		send(user.getEmail(), "Reset your password", "Reset your password: " + link);
	}

	@Override
	public void sendTeacherInvite(User user, String rawToken) {
		String link = appProperties.baseUrl() + "/accept-invite?token=" + rawToken;
		send(user.getEmail(), "You're invited to KidLearn as a teacher",
				"Hello " + user.getDisplayName() + ",\n\nAn administrator has invited you to KidLearn as a teacher. "
						+ "Set your password here (the link works once): " + link);
	}

	@Override
	public void sendAlreadyRegistered(User user) {
		String base = appProperties.baseUrl();
		send(user.getEmail(), "Ви вже зареєстровані в «Безпечному Просторі»",
				"Вітаємо!\n\nХтось (можливо, ви) щойно спробував зареєструватися з цією адресою, але акаунт із нею вже є."
						+ " Нічого не змінено.\n\nУвійти: " + base + "/login\nЗабули пароль? Відновіть його: " + base
						+ "/forgot-password\n\nЯкщо це були не ви — просто проігноруйте цей лист.");
	}

	private void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		if (appProperties.mailFrom() != null && !appProperties.mailFrom().isBlank()) {
			message.setFrom(appProperties.mailFrom());
		}
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		mailSender.send(message);
	}

}
