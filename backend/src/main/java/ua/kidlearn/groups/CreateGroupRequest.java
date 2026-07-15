package ua.kidlearn.groups;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest(@NotBlank String name) {
}
