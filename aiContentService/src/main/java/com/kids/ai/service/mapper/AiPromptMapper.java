package com.kids.ai.service.mapper;

import com.kids.ai.domain.AiPrompt;
import com.kids.ai.service.dto.AiPromptDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AiPrompt} and its DTO {@link AiPromptDTO}.
 */
@Mapper(componentModel = "spring")
public interface AiPromptMapper extends EntityMapper<AiPromptDTO, AiPrompt> {}
