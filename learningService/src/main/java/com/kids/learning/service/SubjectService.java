package com.kids.learning.service;

import com.kids.learning.domain.Subject;
import com.kids.learning.repository.SubjectRepository;
import com.kids.learning.service.dto.SubjectDTO;
import com.kids.learning.service.mapper.SubjectMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.kids.learning.domain.Subject}.
 */
@Service
@Transactional
public class SubjectService {

    private static final Logger LOG = LoggerFactory.getLogger(SubjectService.class);

    private final SubjectRepository subjectRepository;

    private final SubjectMapper subjectMapper;

    public SubjectService(SubjectRepository subjectRepository, SubjectMapper subjectMapper) {
        this.subjectRepository = subjectRepository;
        this.subjectMapper = subjectMapper;
    }

    /**
     * Save a subject.
     *
     * @param subjectDTO the entity to save.
     * @return the persisted entity.
     */
    public SubjectDTO save(SubjectDTO subjectDTO) {
        LOG.debug("Request to save Subject : {}", subjectDTO);
        Subject subject = subjectMapper.toEntity(subjectDTO);
        subject = subjectRepository.save(subject);
        return subjectMapper.toDto(subject);
    }

    /**
     * Update a subject.
     *
     * @param subjectDTO the entity to save.
     * @return the persisted entity.
     */
    public SubjectDTO update(SubjectDTO subjectDTO) {
        LOG.debug("Request to update Subject : {}", subjectDTO);
        Subject subject = subjectMapper.toEntity(subjectDTO);
        subject = subjectRepository.save(subject);
        return subjectMapper.toDto(subject);
    }

    /**
     * Partially update a subject.
     *
     * @param subjectDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SubjectDTO> partialUpdate(SubjectDTO subjectDTO) {
        LOG.debug("Request to partially update Subject : {}", subjectDTO);

        return subjectRepository
            .findById(subjectDTO.getId())
            .map(existingSubject -> {
                subjectMapper.partialUpdate(existingSubject, subjectDTO);

                return existingSubject;
            })
            .map(subjectRepository::save)
            .map(subjectMapper::toDto);
    }

    /**
     * Get all the subjects.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<SubjectDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Subjects");
        return subjectRepository.findAll(pageable).map(subjectMapper::toDto);
    }

    /**
     * Get one subject by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SubjectDTO> findOne(Long id) {
        LOG.debug("Request to get Subject : {}", id);
        return subjectRepository.findById(id).map(subjectMapper::toDto);
    }

    /**
     * Delete the subject by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Subject : {}", id);
        subjectRepository.deleteById(id);
    }
}
