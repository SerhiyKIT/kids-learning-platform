package ua.kidlearn.kidplay;

/** @param sessionLimitMinutes one of 10/15/20/30, or null for no limit (validated in PlayStateService) */
public record PlaySettingsRequest(Integer sessionLimitMinutes) {
}
