package com.kids.ai.domain;

import static com.kids.ai.domain.GeneratedStoryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.ai.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GeneratedStoryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GeneratedStory.class);
        GeneratedStory generatedStory1 = getGeneratedStorySample1();
        GeneratedStory generatedStory2 = new GeneratedStory();
        assertThat(generatedStory1).isNotEqualTo(generatedStory2);

        generatedStory2.setId(generatedStory1.getId());
        assertThat(generatedStory1).isEqualTo(generatedStory2);

        generatedStory2 = getGeneratedStorySample2();
        assertThat(generatedStory1).isNotEqualTo(generatedStory2);
    }
}
