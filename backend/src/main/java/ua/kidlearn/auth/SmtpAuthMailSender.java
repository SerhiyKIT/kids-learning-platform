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

	private void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		mailSender.send(message);
	}

}
