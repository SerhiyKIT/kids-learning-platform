package ua.kidlearn.groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JoinGroupRequest(@NotBlank String joinCode, @NotNull UUID childId) {
}
