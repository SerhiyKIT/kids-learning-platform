package com.kids.ai.service;

import com.kids.ai.domain.GeneratedStory;
import com.kids.ai.repository.GeneratedStoryRepository;
import com.kids.ai.service.dto.GeneratedStoryDTO;
import com.kids.ai.service.mapper.GeneratedStoryMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing {@link com.kids.ai.domain.GeneratedStory}.
 */
@Service
public class GeneratedStoryService {

    private static final Logger LOG = LoggerFactory.getLogger(GeneratedStoryService.class);

    private final GeneratedStoryRepository generatedStoryRepository;

    private final GeneratedStoryMapper generatedStoryMapper;

    public GeneratedStoryService(GeneratedStoryRepository generatedStoryRepository, GeneratedStoryMapper generatedStoryMapper) {
        this.generatedStoryRepository = generatedStoryRepository;
        this.generatedStoryMapper = generatedStoryMapper;
    }

    /**
     * Save a generatedStory.
     *
     * @param generatedStoryDTO the entity to save.
     * @return the persisted entity.
     */
    public GeneratedStoryDTO save(GeneratedStoryDTO generatedStoryDTO) {
        LOG.debug("Request to save GeneratedStory : {}", generatedStoryDTO);
        GeneratedStory generatedStory = generatedStoryMapper.toEntity(generatedStoryDTO);
        generatedStory = generatedStoryRepository.save(generatedStory);
        return generatedStoryMapper.toDto(generatedStory);
    }

    /**
     * Update a generatedStory.
     *
     * @param generatedStoryDTO the entity to save.
     * @return the persisted entity.
     */
    public GeneratedStoryDTO update(GeneratedStoryDTO generatedStoryDTO) {
        LOG.debug("Request to update GeneratedStory : {}", generatedStoryDTO);
        GeneratedStory generatedStory = generatedStoryMapper.toEntity(generatedStoryDTO);
        generatedStory = generatedStoryRepository.save(generatedStory);
        return generatedStoryMapper.toDto(generatedStory);
    }

    /**
     * Partially update a generatedStory.
     *
     * @param generatedStoryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<GeneratedStoryDTO> partialUpdate(GeneratedStoryDTO generatedStoryDTO) {
        LOG.debug("Request to partially update GeneratedStory : {}", generatedStoryDTO);

        return generatedStoryRepository
            .findById(generatedStoryDTO.getId())
            .map(existingGeneratedStory -> {
                generatedStoryMapper.partialUpdate(existingGeneratedStory, generatedStoryDTO);

                return existingGeneratedStory;
            })
            .map(generatedStoryRepository::save)
            .map(generatedStoryMapper::toDto);
    }

    /**
     * Get all the generatedStories.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<GeneratedStoryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all GeneratedStories");
        return generatedStoryRepository.findAll(pageable).map(generatedStoryMapper::toDto);
    }

    /**
     * Get one generatedStory by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<GeneratedStoryDTO> findOne(String id) {
        LOG.debug("Request to get GeneratedStory : {}", id);
        return generatedStoryRepository.findById(id).map(generatedStoryMapper::toDto);
    }

    /**
     * Delete the generatedStory by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete GeneratedStory : {}", id);
        generatedStoryRepository.deleteById(id);
    }
}
