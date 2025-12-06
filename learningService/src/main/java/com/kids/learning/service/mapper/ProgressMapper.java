package com.kids.learning.service.mapper;

import com.kids.learning.domain.Lesson;
import com.kids.learning.domain.Progress;
import com.kids.learning.service.dto.LessonDTO;
import com.kids.learning.service.dto.ProgressDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Progress} and its DTO {@link ProgressDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProgressMapper extends EntityMapper<ProgressDTO, Progress> {
    @Mapping(target = "lesson", source = "lesson", qualifiedByName = "lessonId")
    ProgressDTO toDto(Progress s);

    @Named("lessonId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    LessonDTO toDtoLessonId(Lesson lesson);
}
