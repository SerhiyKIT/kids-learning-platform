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
		send(user.getEmail(), "Підтвердіть пошту — Безпечний Простір",
				"Вітаємо, " + user.getDisplayName() + "!\n\nЩоб завершити реєстрацію, підтвердіть адресу за посиланням"
						+ " (діє 24 години):\n" + link + "\n\nЯкщо ви не реєструвалися — просто проігноруйте цей лист.");
	}

	@Override
	public void sendPasswordReset(User user, String rawToken) {
		String link = appProperties.baseUrl() + "/reset-password?token=" + rawToken;
		send(user.getEmail(), "Новий пароль — Безпечний Простір",
				"Вітаємо!\n\nЩоб задати новий пароль, перейдіть за посиланням (діє 1 годину):\n" + link
						+ "\n\nПісля зміни пароля всі пристрої вийдуть з акаунта. Якщо ви не просили новий пароль —"
						+ " проігноруйте цей лист, старий пароль діє й далі.");
	}

	@Override
	public void sendTeacherInvite(User user, String rawToken) {
		String link = appProperties.baseUrl() + "/accept-invite?token=" + rawToken;
		send(user.getEmail(), "Запрошення для вчителя — Безпечний Простір",
				"Вітаємо, " + user.getDisplayName() + "!\n\nАдміністратор запросив вас на платформу «Безпечний Простір»"
						+ " як вчителя. Створіть пароль за посиланням (працює один раз):\n" + link);
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
