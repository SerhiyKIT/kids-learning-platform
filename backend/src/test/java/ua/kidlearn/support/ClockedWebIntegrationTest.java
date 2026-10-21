package ua.kidlearn.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ua.kidlearn.ratelimit.MutableClockTestConfig;
import ua.kidlearn.ratelimit.MutableTestClock;
import ua.kidlearn.support.WebIntegrationTest;

/**
 * WebIntegrationTest with a MutableTestClock as the application Clock, for tests that move time
 * (rate-limit windows, TTLs, play sessions, retention). Separate from the base on purpose: that
 * clock stands still unless advanced, which tests relying on real time passing can't use.
 */
@Import(MutableClockTestConfig.class)
public abstract class ClockedWebIntegrationTest extends WebIntegrationTest {

	@Autowired
	protected MutableTestClock testClock;

}
