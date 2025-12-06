package com.kids.ai.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.kids.ai.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AiPromptDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AiPromptDTO.class);
        AiPromptDTO aiPromptDTO1 = new AiPromptDTO();
        aiPromptDTO1.setId("id1");
        AiPromptDTO aiPromptDTO2 = new AiPromptDTO();
        assertThat(aiPromptDTO1).isNotEqualTo(aiPromptDTO2);
        aiPromptDTO2.setId(aiPromptDTO1.getId());
        assertThat(aiPromptDTO1).isEqualTo(aiPromptDTO2);
        aiPromptDTO2.setId("id2");
        assertThat(aiPromptDTO1).isNotEqualTo(aiPromptDTO2);
        aiPromptDTO1.setId(null);
        assertThat(aiPromptDTO1).isNotEqualTo(aiPromptDTO2);
    }
}
