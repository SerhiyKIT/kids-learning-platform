package com.kids.learning.service;

import com.kids.learning.domain.Achievement;
import com.kids.learning.repository.AchievementRepository;
import com.kids.learning.service.dto.AchievementDTO;
import com.kids.learning.service.mapper.AchievementMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.kids.learning.domain.Achievement}.
 */
@Service
@Transactional
public class AchievementService {

    private static final Logger LOG = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;

    private final AchievementMapper achievementMapper;

    public AchievementService(AchievementRepository achievementRepository, AchievementMapper achievementMapper) {
        this.achievementRepository = achievementRepository;
        this.achievementMapper = achievementMapper;
    }

    /**
     * Save a achievement.
     *
     * @param achievementDTO the entity to save.
     * @return the persisted entity.
     */
    public AchievementDTO save(AchievementDTO achievementDTO) {
        LOG.debug("Request to save Achievement : {}", achievementDTO);
        Achievement achievement = achievementMapper.toEntity(achievementDTO);
        achievement = achievementRepository.save(achievement);
        return achievementMapper.toDto(achievement);
    }

    /**
     * Update a achievement.
     *
     * @param achievementDTO the entity to save.
     * @return the persisted entity.
     */
    public AchievementDTO update(AchievementDTO achievementDTO) {
        LOG.debug("Request to update Achievement : {}", achievementDTO);
        Achievement achievement = achievementMapper.toEntity(achievementDTO);
        achievement = achievementRepository.save(achievement);
        return achievementMapper.toDto(achievement);
    }

    /**
     * Partially update a achievement.
     *
     * @param achievementDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AchievementDTO> partialUpdate(AchievementDTO achievementDTO) {
        LOG.debug("Request to partially update Achievement : {}", achievementDTO);

        return achievementRepository
            .findById(achievementDTO.getId())
            .map(existingAchievement -> {
                achievementMapper.partialUpdate(existingAchievement, achievementDTO);

                return existingAchievement;
            })
            .map(achievementRepository::save)
            .map(achievementMapper::toDto);
    }

    /**
     * Get all the achievements.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<AchievementDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Achievements");
        return achievementRepository.findAll(pageable).map(achievementMapper::toDto);
    }

    /**
     * Get one achievement by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AchievementDTO> findOne(Long id) {
        LOG.debug("Request to get Achievement : {}", id);
        return achievementRepository.findById(id).map(achievementMapper::toDto);
    }

    /**
     * Delete the achievement by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Achievement : {}", id);
        achievementRepository.deleteById(id);
    }
}
