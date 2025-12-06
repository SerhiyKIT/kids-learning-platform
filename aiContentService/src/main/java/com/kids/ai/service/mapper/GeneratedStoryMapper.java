package com.kids.ai.service.mapper;

import com.kids.ai.domain.GeneratedStory;
import com.kids.ai.service.dto.GeneratedStoryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GeneratedStory} and its DTO {@link GeneratedStoryDTO}.
 */
@Mapper(componentModel = "spring")
public interface GeneratedStoryMapper extends EntityMapper<GeneratedStoryDTO, GeneratedStory> {}
