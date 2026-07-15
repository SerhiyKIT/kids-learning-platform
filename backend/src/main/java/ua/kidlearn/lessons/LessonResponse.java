package ua.kidlearn.lessons;

import java.util.UUID;

public record LessonResponse(UUID id, String moduleCode, String title) {
}
