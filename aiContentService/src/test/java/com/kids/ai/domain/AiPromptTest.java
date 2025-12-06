package com.kids.ai.domain;

import static com.kids.ai.domain.AiPromptTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.ai.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AiPromptTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AiPrompt.class);
        AiPrompt aiPrompt1 = getAiPromptSample1();
        AiPrompt aiPrompt2 = new AiPrompt();
        assertThat(aiPrompt1).isNotEqualTo(aiPrompt2);

        aiPrompt2.setId(aiPrompt1.getId());
        assertThat(aiPrompt1).isEqualTo(aiPrompt2);

        aiPrompt2 = getAiPromptSample2();
        assertThat(aiPrompt1).isNotEqualTo(aiPrompt2);
    }
}
