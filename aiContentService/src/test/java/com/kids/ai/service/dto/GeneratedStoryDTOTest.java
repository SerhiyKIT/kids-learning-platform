package com.kids.ai.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.kids.ai.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GeneratedStoryDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GeneratedStoryDTO.class);
        GeneratedStoryDTO generatedStoryDTO1 = new GeneratedStoryDTO();
        generatedStoryDTO1.setId("id1");
        GeneratedStoryDTO generatedStoryDTO2 = new GeneratedStoryDTO();
        assertThat(generatedStoryDTO1).isNotEqualTo(generatedStoryDTO2);
        generatedStoryDTO2.setId(generatedStoryDTO1.getId());
        assertThat(generatedStoryDTO1).isEqualTo(generatedStoryDTO2);
        generatedStoryDTO2.setId("id2");
        assertThat(generatedStoryDTO1).isNotEqualTo(generatedStoryDTO2);
        generatedStoryDTO1.setId(null);
        assertThat(generatedStoryDTO1).isNotEqualTo(generatedStoryDTO2);
    }
}
