package com.kids.ai.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.kids.ai.domain.GeneratedStory} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GeneratedStoryDTO implements Serializable {

    private String id;

    private String topic;

    private String content;

    private String audioUrl;

    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeneratedStoryDTO)) {
            return false;
        }

        GeneratedStoryDTO generatedStoryDTO = (GeneratedStoryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, generatedStoryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GeneratedStoryDTO{" +
            "id='" + getId() + "'" +
            ", topic='" + getTopic() + "'" +
            ", content='" + getContent() + "'" +
            ", audioUrl='" + getAudioUrl() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
