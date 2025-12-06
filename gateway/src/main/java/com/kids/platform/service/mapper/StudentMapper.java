package com.kids.platform.service.mapper;

import com.kids.platform.domain.Parent;
import com.kids.platform.domain.Student;
import com.kids.platform.service.dto.ParentDTO;
import com.kids.platform.service.dto.StudentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Student} and its DTO {@link StudentDTO}.
 */
@Mapper(componentModel = "spring")
public interface StudentMapper extends EntityMapper<StudentDTO, Student> {
    @Mapping(target = "parent", source = "parent", qualifiedByName = "parentId")
    StudentDTO toDto(Student s);

    @Named("parentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ParentDTO toDtoParentId(Parent parent);
}
