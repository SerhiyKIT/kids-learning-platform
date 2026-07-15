package ua.kidlearn.lessons;

import jakarta.validation.constraints.NotBlank;

public record CreateLessonRequest(@NotBlank String moduleCode, @NotBlank String title) {
}
