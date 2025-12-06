package com.kids.ai.service.mapper;

import static com.kids.ai.domain.AiPromptAsserts.*;
import static com.kids.ai.domain.AiPromptTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiPromptMapperTest {

    private AiPromptMapper aiPromptMapper;

    @BeforeEach
    void setUp() {
        aiPromptMapper = new AiPromptMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAiPromptSample1();
        var actual = aiPromptMapper.toEntity(aiPromptMapper.toDto(expected));
        assertAiPromptAllPropertiesEquals(expected, actual);
    }
}
