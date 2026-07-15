package ua.kidlearn.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends auth-flow emails only after the triggering DB transaction has
 * committed, and off the request thread, so a slow/broken SMTP server never
 * fails the HTTP request that issued the token.
 */
@Component
class AuthMailEventListener {

	private static final Logger log = LoggerFactory.getLogger(AuthMailEventListener.class);

	private final AuthMailSender authMailSender;

	AuthMailEventListener(AuthMailSender authMailSender) {
		this.authMailSender = authMailSender;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
		try {
			authMailSender.sendVerification(event.user(), event.rawToken());
		} catch (Exception e) {
			log.error("Failed to send verification email to {}", event.user().getEmail(), e);
		}
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
		try {
			authMailSender.sendPasswordReset(event.user(), event.rawToken());
		} catch (Exception e) {
			log.error("Failed to send password reset email to {}", event.user().getEmail(), e);
		}
	}

}
