package ua.kidlearn.attempts;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartAttemptRequest(@NotNull UUID lessonVersionId) {
}
