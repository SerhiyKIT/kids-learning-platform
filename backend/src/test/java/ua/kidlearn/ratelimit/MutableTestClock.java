package ua.kidlearn.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Test-only Clock whose instant is advanced explicitly, so rate-limit window tests never sleep. */
public class MutableTestClock extends Clock {

	private Instant instant;
	private final ZoneId zone;

	public MutableTestClock() {
		this(Instant.now(), ZoneOffset.UTC);
	}

	private MutableTestClock(Instant instant, ZoneId zone) {
		this.instant = instant;
		this.zone = zone;
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableTestClock(instant, zone);
	}

	@Override
	public Instant instant() {
		return instant;
	}

	public void advance(Duration duration) {
		this.instant = this.instant.plus(duration);
	}

}
