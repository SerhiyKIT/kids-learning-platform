package ua.kidlearn.lessons;

import jakarta.validation.constraints.NotBlank;

public record RejectVersionRequest(@NotBlank String reason) {
}
