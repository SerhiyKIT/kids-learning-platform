package ua.kidlearn.themes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Generation is a side effect of saving the child's answers: after commit, off the request thread. */
@Component
class ThemeGenerationListener {

	private static final Logger log = LoggerFactory.getLogger(ThemeGenerationListener.class);

	private final ThemeGenerationService generationService;

	ThemeGenerationListener(ThemeGenerationService generationService) {
		this.generationService = generationService;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onGenerationRequested(ThemeGenerationRequested event) {
		try {
			generationService.generateIfAbsent(event.preferenceKey(), event.actorId(), event.actorRole());
		} catch (Exception e) {
			log.error("Theme generation for {} failed", event.preferenceKey(), e);
		}
	}

}
