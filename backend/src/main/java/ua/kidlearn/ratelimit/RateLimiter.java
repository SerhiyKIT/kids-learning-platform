package ua.kidlearn.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Hand-rolled fixed-window request counter (no external library — see the task that added this).
 * Each key has its own window, starting on the first {@link #tryAcquire} call after the previous
 * window (if any) expired. Windows past expiry are evicted opportunistically wherever they're
 * touched, so memory stays bounded without a background sweeper.
 */
@Component
public class RateLimiter {

	private final Clock clock;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public RateLimiter(Clock clock) {
		this.clock = clock;
	}

	/** Records one use of {@code key} and reports whether it's still within {@code maxInWindow}. */
	public boolean tryAcquire(String key, int maxInWindow, Duration window) {
		Instant now = clock.instant();
		Window w = windows.compute(key, (k, existing) -> {
			if (existing == null || existing.isExpired(now, window)) {
				return new Window(now, new AtomicInteger(1));
			}
			existing.count.incrementAndGet();
			return existing;
		});
		return w.count.get() <= maxInWindow;
	}

	/** Read-only: has {@code key} already reached {@code maxInWindow}, without recording a use? */
	public boolean isBlocked(String key, int maxInWindow, Duration window) {
		Instant now = clock.instant();
		Window w = windows.get(key);
		if (w == null) {
			return false;
		}
		if (w.isExpired(now, window)) {
			windows.remove(key, w);
			return false;
		}
		return w.count.get() >= maxInWindow;
	}

	/** Seconds until {@code key}'s current window resets (0 if it has none or already expired). */
	public long retryAfterSeconds(String key, Duration window) {
		Instant now = clock.instant();
		Window w = windows.get(key);
		if (w == null || w.isExpired(now, window)) {
			return 0;
		}
		Duration remaining = Duration.between(now, w.windowStart.plus(window));
		return remaining.isNegative() ? 0 : remaining.toSeconds() + 1;
	}

	public void reset(String key) {
		windows.remove(key);
	}

	private static final class Window {

		private final Instant windowStart;
		private final AtomicInteger count;

		private Window(Instant windowStart, AtomicInteger count) {
			this.windowStart = windowStart;
			this.count = count;
		}

		private boolean isExpired(Instant now, Duration window) {
			return now.isAfter(windowStart.plus(window));
		}

	}

}
