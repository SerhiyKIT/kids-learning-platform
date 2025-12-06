package com.kids.ai.domain;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A AiPrompt.
 */
@Document(collection = "ai_prompt")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AiPrompt implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("style_name")
    private String styleName;

    @Field("system_prompt")
    private String systemPrompt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public String getId() {
        return this.id;
    }

    public AiPrompt id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStyleName() {
        return this.styleName;
    }

    public AiPrompt styleName(String styleName) {
        this.setStyleName(styleName);
        return this;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String getSystemPrompt() {
        return this.systemPrompt;
    }

    public AiPrompt systemPrompt(String systemPrompt) {
        this.setSystemPrompt(systemPrompt);
        return this;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AiPrompt)) {
            return false;
        }
        return getId() != null && getId().equals(((AiPrompt) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AiPrompt{" +
            "id=" + getId() +
            ", styleName='" + getStyleName() + "'" +
            ", systemPrompt='" + getSystemPrompt() + "'" +
            "}";
    }
}
