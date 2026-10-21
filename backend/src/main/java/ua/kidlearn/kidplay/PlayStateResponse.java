package ua.kidlearn.kidplay;

import java.time.Instant;

/**
 * What child mode and the parent's cabinet need to know. Child mode only looks at {@code resting}
 * (the child never sees a clock); {@code restUntil} is for the parent's "rests until HH:MM".
 *
 * @param sessionLimitMinutes null = no limit
 */
public record PlayStateResponse(boolean resting, Instant restUntil, Integer sessionLimitMinutes) {
}
