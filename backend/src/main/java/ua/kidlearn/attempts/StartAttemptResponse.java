package ua.kidlearn.attempts;

import java.util.UUID;

/**
 * @param lessonId the lesson this version belongs to — the child-mode screen needs it (sticker per
 * lesson) and would otherwise fetch the whole available-lessons list just for it (QA1-036)
 */
public record StartAttemptResponse(UUID attemptId, UUID lessonId) {
}
