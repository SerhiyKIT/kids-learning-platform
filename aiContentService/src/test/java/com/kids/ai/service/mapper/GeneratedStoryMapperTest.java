package com.kids.ai.service.mapper;

import static com.kids.ai.domain.GeneratedStoryAsserts.*;
import static com.kids.ai.domain.GeneratedStoryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneratedStoryMapperTest {

    private GeneratedStoryMapper generatedStoryMapper;

    @BeforeEach
    void setUp() {
        generatedStoryMapper = new GeneratedStoryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGeneratedStorySample1();
        var actual = generatedStoryMapper.toEntity(generatedStoryMapper.toDto(expected));
        assertGeneratedStoryAllPropertiesEquals(expected, actual);
    }
}
