package ua.kidlearn.ratelimit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Swaps in a MutableTestClock (as the primary Clock bean) so rate-limit window tests can advance
 * time explicitly instead of sleeping. Different bean name than the production ClockConfig's
 * "clock" to avoid a bean-definition clash; @Primary resolves the by-type ambiguity — any
 * Clock-typed injection point (e.g. RateLimiter's constructor) then resolves to this bean. */
@TestConfiguration
public class MutableClockTestConfig {

	@Bean
	@Primary
	public MutableTestClock testClock() {
		return new MutableTestClock();
	}

}
