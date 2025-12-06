package com.kids.learning.service.mapper;

import com.kids.learning.domain.Lesson;
import com.kids.learning.domain.Subject;
import com.kids.learning.service.dto.LessonDTO;
import com.kids.learning.service.dto.SubjectDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Lesson} and its DTO {@link LessonDTO}.
 */
@Mapper(componentModel = "spring")
public interface LessonMapper extends EntityMapper<LessonDTO, Lesson> {
    @Mapping(target = "subject", source = "subject", qualifiedByName = "subjectId")
    LessonDTO toDto(Lesson s);

    @Named("subjectId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    SubjectDTO toDtoSubjectId(Subject subject);
}
