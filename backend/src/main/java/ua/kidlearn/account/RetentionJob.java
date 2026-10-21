package ua.kidlearn.account;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs RetentionService once a day (app.retention.cron). The pilot runs ONE backend instance, so
 * there is no distributed lock: with several instances each would run the same idempotent deletes
 * — harmless, but a lock (e.g. ShedLock) should be added before scaling out (docs/DEPLOY.md).
 */
@Component
@ConditionalOnProperty(prefix = "app.retention", name = "enabled", havingValue = "true", matchIfMissing = true)
class RetentionJob {

	private final RetentionService retentionService;

	RetentionJob(RetentionService retentionService) {
		this.retentionService = retentionService;
	}

	@Scheduled(cron = "${app.retention.cron:0 30 3 * * *}")
	void run() {
		retentionService.purge();
	}

}
