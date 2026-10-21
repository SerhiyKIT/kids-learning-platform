package ua.kidlearn.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Child-mode play session (docs/UX_гайд_дитячого_режиму.md §6).
 *
 * @param restDuration how long the child "rests" once the session limit is reached
 * @param sessionGap a pause between lessons longer than this starts a new session
 * @param limitUnit what one "minute" of a parent's session limit lasts — a minute, except in the
 *   e2e profile, where the limit has to be reachable within a test run
 */
@ConfigurationProperties(prefix = "app.kid")
public record KidPlayProperties(@DefaultValue("60m") Duration restDuration, @DefaultValue("30m") Duration sessionGap,
		@DefaultValue("1m") Duration limitUnit) {
}
