package com.kids.learning.service.mapper;

import com.kids.learning.domain.Achievement;
import com.kids.learning.service.dto.AchievementDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Achievement} and its DTO {@link AchievementDTO}.
 */
@Mapper(componentModel = "spring")
public interface AchievementMapper extends EntityMapper<AchievementDTO, Achievement> {}
