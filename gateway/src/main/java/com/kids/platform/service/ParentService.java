package com.kids.platform.service;

import com.kids.platform.repository.ParentRepository;
import com.kids.platform.service.dto.ParentDTO;
import com.kids.platform.service.mapper.ParentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.kids.platform.domain.Parent}.
 */
@Service
@Transactional
public class ParentService {

    private static final Logger LOG = LoggerFactory.getLogger(ParentService.class);

    private final ParentRepository parentRepository;

    private final ParentMapper parentMapper;

    public ParentService(ParentRepository parentRepository, ParentMapper parentMapper) {
        this.parentRepository = parentRepository;
        this.parentMapper = parentMapper;
    }

    /**
     * Save a parent.
     *
     * @param parentDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<ParentDTO> save(ParentDTO parentDTO) {
        LOG.debug("Request to save Parent : {}", parentDTO);
        return parentRepository.save(parentMapper.toEntity(parentDTO)).map(parentMapper::toDto);
    }

    /**
     * Update a parent.
     *
     * @param parentDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<ParentDTO> update(ParentDTO parentDTO) {
        LOG.debug("Request to update Parent : {}", parentDTO);
        return parentRepository.save(parentMapper.toEntity(parentDTO)).map(parentMapper::toDto);
    }

    /**
     * Partially update a parent.
     *
     * @param parentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Mono<ParentDTO> partialUpdate(ParentDTO parentDTO) {
        LOG.debug("Request to partially update Parent : {}", parentDTO);

        return parentRepository
            .findById(parentDTO.getId())
            .map(existingParent -> {
                parentMapper.partialUpdate(existingParent, parentDTO);

                return existingParent;
            })
            .flatMap(parentRepository::save)
            .map(parentMapper::toDto);
    }

    /**
     * Get all the parents.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Flux<ParentDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Parents");
        return parentRepository.findAllBy(pageable).map(parentMapper::toDto);
    }

    /**
     * Returns the number of parents available.
     * @return the number of entities in the database.
     *
     */
    public Mono<Long> countAll() {
        return parentRepository.count();
    }

    /**
     * Get one parent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Mono<ParentDTO> findOne(Long id) {
        LOG.debug("Request to get Parent : {}", id);
        return parentRepository.findById(id).map(parentMapper::toDto);
    }

    /**
     * Delete the parent by id.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Parent : {}", id);
        return parentRepository.deleteById(id);
    }
}
