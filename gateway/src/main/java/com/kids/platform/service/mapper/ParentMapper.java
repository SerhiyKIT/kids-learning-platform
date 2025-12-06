package com.kids.platform.service.mapper;

import com.kids.platform.domain.Parent;
import com.kids.platform.service.dto.ParentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Parent} and its DTO {@link ParentDTO}.
 */
@Mapper(componentModel = "spring")
public interface ParentMapper extends EntityMapper<ParentDTO, Parent> {}
