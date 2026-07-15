package ua.kidlearn.lessons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record CreateLessonVersionRequest(@NotNull JsonNode scenario, @NotBlank String generatedBy) {
}
