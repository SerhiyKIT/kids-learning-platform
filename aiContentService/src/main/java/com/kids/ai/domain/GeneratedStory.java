package com.kids.ai.domain;

import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A GeneratedStory.
 */
@Document(collection = "generated_story")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GeneratedStory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("topic")
    private String topic;

    @Field("content")
    private String content;

    @Field("audio_url")
    private String audioUrl;

    @Field("created_at")
    private Instant createdAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public String getId() {
        return this.id;
    }

    public GeneratedStory id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return this.topic;
    }

    public GeneratedStory topic(String topic) {
        this.setTopic(topic);
        return this;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getContent() {
        return this.content;
    }

    public GeneratedStory content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAudioUrl() {
        return this.audioUrl;
    }

    public GeneratedStory audioUrl(String audioUrl) {
        this.setAudioUrl(audioUrl);
        return this;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public GeneratedStory createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeneratedStory)) {
            return false;
        }
        return getId() != null && getId().equals(((GeneratedStory) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GeneratedStory{" +
            "id=" + getId() +
            ", topic='" + getTopic() + "'" +
            ", content='" + getContent() + "'" +
            ", audioUrl='" + getAudioUrl() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
