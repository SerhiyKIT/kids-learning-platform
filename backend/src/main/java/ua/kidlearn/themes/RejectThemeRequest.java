package ua.kidlearn.themes;

import jakarta.validation.constraints.NotBlank;

public record RejectThemeRequest(@NotBlank String reason) {
}
