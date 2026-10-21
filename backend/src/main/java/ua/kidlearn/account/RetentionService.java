package ua.kidlearn.account;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.config.RetentionProperties;

/**
 * Deletes data past its storage period. Only counts are logged — never what was deleted.
 * <ul>
 * <li>Per-scene answers and assistant dialogs of attempts older than app.retention.detailed-history
 * (by completed_at, else started_at). The attempts themselves (lesson, result, score) stay: they are
 * the aggregates the parent and teacher keep seeing.</li>
 * <li>Used or expired one-time tokens (email verification, password reset, invites) older than
 * app.retention.spent-tokens.</li>
 * <li>Parent accounts that never verified their email within app.retention.unverified-accounts.
 * They can't have children (creating one requires a verified email); the query checks it anyway.</li>
 * </ul>
 */
@Service
public class RetentionService {

	private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

	public record Result(int sceneAnswers, int assistantDialogs, int tokens, int unverifiedAccounts) {
	}

	private final JdbcTemplate jdbcTemplate;
	private final RetentionProperties properties;
	private final Clock clock;

	public RetentionService(JdbcTemplate jdbcTemplate, RetentionProperties properties, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public Result purge() {
		Instant now = clock.instant();
		Timestamp historyCutoff = Timestamp.from(
				ZonedDateTime.ofInstant(now, clock.getZone()).minus(properties.detailedHistory()).toInstant());
		Timestamp tokenCutoff = Timestamp.from(now.minus(properties.spentTokens()));
		Timestamp accountCutoff = Timestamp.from(now.minus(properties.unverifiedAccounts()));

		int answers = jdbcTemplate.update("""
				DELETE FROM scene_answers sa USING lesson_attempts la
				WHERE sa.attempt_id = la.id AND COALESCE(la.completed_at, la.started_at) < ?""", historyCutoff);
		int dialogs = jdbcTemplate.update("""
				DELETE FROM assistant_dialogs d USING lesson_attempts la
				WHERE d.attempt_id = la.id AND COALESCE(la.completed_at, la.started_at) < ?""", historyCutoff);
		int tokens = jdbcTemplate.update("""
				DELETE FROM user_tokens
				WHERE (used_at IS NOT NULL OR expires_at < ?) AND created_at < ?""", Timestamp.from(now), tokenCutoff);
		int accounts = jdbcTemplate.update("""
				DELETE FROM users u
				WHERE u.role = 'parent' AND u.email_verified_at IS NULL AND u.created_at < ?
				  AND NOT EXISTS (SELECT 1 FROM parent_child pc WHERE pc.parent_id = u.id)
				  AND NOT EXISTS (SELECT 1 FROM children c WHERE c.created_by = u.id)""", accountCutoff);

		Result result = new Result(answers, dialogs, tokens, accounts);
		log.info("Retention: deleted {} scene answers, {} assistant dialogs, {} spent tokens, {} unverified accounts",
				answers, dialogs, tokens, accounts);
		return result;
	}

}
