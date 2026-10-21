package ua.kidlearn.themes;

import jakarta.validation.constraints.NotBlank;

public record RegenerateThemeRequest(@NotBlank String preferenceKey) {
}
