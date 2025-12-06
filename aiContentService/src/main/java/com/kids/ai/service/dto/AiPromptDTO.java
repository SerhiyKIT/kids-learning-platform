package com.kids.ai.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.kids.ai.domain.AiPrompt} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AiPromptDTO implements Serializable {

    private String id;

    @NotNull
    private String styleName;

    private String systemPrompt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AiPromptDTO)) {
            return false;
        }

        AiPromptDTO aiPromptDTO = (AiPromptDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, aiPromptDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AiPromptDTO{" +
            "id='" + getId() + "'" +
            ", styleName='" + getStyleName() + "'" +
            ", systemPrompt='" + getSystemPrompt() + "'" +
            "}";
    }
}
