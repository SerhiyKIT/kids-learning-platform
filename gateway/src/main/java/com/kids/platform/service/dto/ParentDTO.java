package com.kids.platform.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.kids.platform.domain.Parent} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ParentDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    private String firstName;

    @NotNull(message = "must not be null")
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private String email;

    private Boolean isPremium;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParentDTO)) {
            return false;
        }

        ParentDTO parentDTO = (ParentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, parentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ParentDTO{" +
            "id=" + getId() +
            ", firstName='" + getFirstName() + "'" +
            ", email='" + getEmail() + "'" +
            ", isPremium='" + getIsPremium() + "'" +
            "}";
    }
}
