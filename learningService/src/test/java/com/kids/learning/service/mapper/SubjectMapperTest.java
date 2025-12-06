package com.kids.learning.service.mapper;

import static com.kids.learning.domain.SubjectAsserts.*;
import static com.kids.learning.domain.SubjectTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubjectMapperTest {

    private SubjectMapper subjectMapper;

    @BeforeEach
    void setUp() {
        subjectMapper = new SubjectMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSubjectSample1();
        var actual = subjectMapper.toEntity(subjectMapper.toDto(expected));
        assertSubjectAllPropertiesEquals(expected, actual);
    }
}
