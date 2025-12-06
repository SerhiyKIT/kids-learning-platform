package com.kids.learning.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.kids.learning.domain.Achievement} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AchievementDTO implements Serializable {

    private Long id;

    private String title;

    private String iconUrl;

    private Instant dateEarned;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Instant getDateEarned() {
        return dateEarned;
    }

    public void setDateEarned(Instant dateEarned) {
        this.dateEarned = dateEarned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AchievementDTO)) {
            return false;
        }

        AchievementDTO achievementDTO = (AchievementDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, achievementDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AchievementDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", iconUrl='" + getIconUrl() + "'" +
            ", dateEarned='" + getDateEarned() + "'" +
            "}";
    }
}
