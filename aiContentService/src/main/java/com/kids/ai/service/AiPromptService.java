package com.kids.ai.service;

import com.kids.ai.domain.AiPrompt;
import com.kids.ai.repository.AiPromptRepository;
import com.kids.ai.service.dto.AiPromptDTO;
import com.kids.ai.service.mapper.AiPromptMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing {@link com.kids.ai.domain.AiPrompt}.
 */
@Service
public class AiPromptService {

    private static final Logger LOG = LoggerFactory.getLogger(AiPromptService.class);

    private final AiPromptRepository aiPromptRepository;

    private final AiPromptMapper aiPromptMapper;

    public AiPromptService(AiPromptRepository aiPromptRepository, AiPromptMapper aiPromptMapper) {
        this.aiPromptRepository = aiPromptRepository;
        this.aiPromptMapper = aiPromptMapper;
    }

    /**
     * Save a aiPrompt.
     *
     * @param aiPromptDTO the entity to save.
     * @return the persisted entity.
     */
    public AiPromptDTO save(AiPromptDTO aiPromptDTO) {
        LOG.debug("Request to save AiPrompt : {}", aiPromptDTO);
        AiPrompt aiPrompt = aiPromptMapper.toEntity(aiPromptDTO);
        aiPrompt = aiPromptRepository.save(aiPrompt);
        return aiPromptMapper.toDto(aiPrompt);
    }

    /**
     * Update a aiPrompt.
     *
     * @param aiPromptDTO the entity to save.
     * @return the persisted entity.
     */
    public AiPromptDTO update(AiPromptDTO aiPromptDTO) {
        LOG.debug("Request to update AiPrompt : {}", aiPromptDTO);
        AiPrompt aiPrompt = aiPromptMapper.toEntity(aiPromptDTO);
        aiPrompt = aiPromptRepository.save(aiPrompt);
        return aiPromptMapper.toDto(aiPrompt);
    }

    /**
     * Partially update a aiPrompt.
     *
     * @param aiPromptDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AiPromptDTO> partialUpdate(AiPromptDTO aiPromptDTO) {
        LOG.debug("Request to partially update AiPrompt : {}", aiPromptDTO);

        return aiPromptRepository
            .findById(aiPromptDTO.getId())
            .map(existingAiPrompt -> {
                aiPromptMapper.partialUpdate(existingAiPrompt, aiPromptDTO);

                return existingAiPrompt;
            })
            .map(aiPromptRepository::save)
            .map(aiPromptMapper::toDto);
    }

    /**
     * Get all the aiPrompts.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<AiPromptDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all AiPrompts");
        return aiPromptRepository.findAll(pageable).map(aiPromptMapper::toDto);
    }

    /**
     * Get one aiPrompt by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<AiPromptDTO> findOne(String id) {
        LOG.debug("Request to get AiPrompt : {}", id);
        return aiPromptRepository.findById(id).map(aiPromptMapper::toDto);
    }

    /**
     * Delete the aiPrompt by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete AiPrompt : {}", id);
        aiPromptRepository.deleteById(id);
    }
}
