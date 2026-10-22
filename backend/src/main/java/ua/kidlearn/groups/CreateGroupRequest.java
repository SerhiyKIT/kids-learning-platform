package ua.kidlearn.groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A group's name: 1–80 characters (QA1-016). */
public record CreateGroupRequest(@NotBlank @Size(max = 80) String name) {
}
