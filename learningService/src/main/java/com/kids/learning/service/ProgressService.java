package com.kids.learning.service;

import com.kids.learning.domain.Progress;
import com.kids.learning.repository.ProgressRepository;
import com.kids.learning.service.dto.ProgressDTO;
import com.kids.learning.service.mapper.ProgressMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.kids.learning.domain.Progress}.
 */
@Service
@Transactional
public class ProgressService {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressService.class);

    private final ProgressRepository progressRepository;

    private final ProgressMapper progressMapper;

    public ProgressService(ProgressRepository progressRepository, ProgressMapper progressMapper) {
        this.progressRepository = progressRepository;
        this.progressMapper = progressMapper;
    }

    /**
     * Save a progress.
     *
     * @param progressDTO the entity to save.
     * @return the persisted entity.
     */
    public ProgressDTO save(ProgressDTO progressDTO) {
        LOG.debug("Request to save Progress : {}", progressDTO);
        Progress progress = progressMapper.toEntity(progressDTO);
        progress = progressRepository.save(progress);
        return progressMapper.toDto(progress);
    }

    /**
     * Update a progress.
     *
     * @param progressDTO the entity to save.
     * @return the persisted entity.
     */
    public ProgressDTO update(ProgressDTO progressDTO) {
        LOG.debug("Request to update Progress : {}", progressDTO);
        Progress progress = progressMapper.toEntity(progressDTO);
        progress = progressRepository.save(progress);
        return progressMapper.toDto(progress);
    }

    /**
     * Partially update a progress.
     *
     * @param progressDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProgressDTO> partialUpdate(ProgressDTO progressDTO) {
        LOG.debug("Request to partially update Progress : {}", progressDTO);

        return progressRepository
            .findById(progressDTO.getId())
            .map(existingProgress -> {
                progressMapper.partialUpdate(existingProgress, progressDTO);

                return existingProgress;
            })
            .map(progressRepository::save)
            .map(progressMapper::toDto);
    }

    /**
     * Get all the progresses.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ProgressDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Progresses");
        return progressRepository.findAll(pageable).map(progressMapper::toDto);
    }

    /**
     * Get one progress by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProgressDTO> findOne(Long id) {
        LOG.debug("Request to get Progress : {}", id);
        return progressRepository.findById(id).map(progressMapper::toDto);
    }

    /**
     * Delete the progress by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Progress : {}", id);
        progressRepository.deleteById(id);
    }
}
