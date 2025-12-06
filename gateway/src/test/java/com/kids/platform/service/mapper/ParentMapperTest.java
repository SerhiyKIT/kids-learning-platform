package com.kids.platform.service.mapper;

import static com.kids.platform.domain.ParentAsserts.*;
import static com.kids.platform.domain.ParentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParentMapperTest {

    private ParentMapper parentMapper;

    @BeforeEach
    void setUp() {
        parentMapper = new ParentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getParentSample1();
        var actual = parentMapper.toEntity(parentMapper.toDto(expected));
        assertParentAllPropertiesEquals(expected, actual);
    }
}
