package ua.kidlearn.config;

import java.time.Duration;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Storage periods (docs/Ролі_та_приватність.md, «Зафіксовані рішення»).
 *
 * @param detailedHistory per-scene answers (and assistant dialogs) older than this are deleted;
 *   attempts with their scores stay as aggregates
 * @param spentTokens used or expired email/password tokens older than this are deleted
 * @param unverifiedAccounts parent accounts that never verified their email (and so have no
 *   children) are deleted after this
 * @param cron when the daily job runs (server time zone)
 */
@ConfigurationProperties(prefix = "app.retention")
public record RetentionProperties(@DefaultValue("true") boolean enabled, @DefaultValue("P2Y") Period detailedHistory,
		@DefaultValue("30d") Duration spentTokens, @DefaultValue("30d") Duration unverifiedAccounts,
		@DefaultValue("0 30 3 * * *") String cron) {
}
