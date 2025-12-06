package com.kids.platform.service;

import com.kids.platform.repository.StudentRepository;
import com.kids.platform.service.dto.StudentDTO;
import com.kids.platform.service.mapper.StudentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.kids.platform.domain.Student}.
 */
@Service
@Transactional
public class StudentService {

    private static final Logger LOG = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;

    private final StudentMapper studentMapper;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    /**
     * Save a student.
     *
     * @param studentDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<StudentDTO> save(StudentDTO studentDTO) {
        LOG.debug("Request to save Student : {}", studentDTO);
        return studentRepository.save(studentMapper.toEntity(studentDTO)).map(studentMapper::toDto);
    }

    /**
     * Update a student.
     *
     * @param studentDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<StudentDTO> update(StudentDTO studentDTO) {
        LOG.debug("Request to update Student : {}", studentDTO);
        return studentRepository.save(studentMapper.toEntity(studentDTO)).map(studentMapper::toDto);
    }

    /**
     * Partially update a student.
     *
     * @param studentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Mono<StudentDTO> partialUpdate(StudentDTO studentDTO) {
        LOG.debug("Request to partially update Student : {}", studentDTO);

        return studentRepository
            .findById(studentDTO.getId())
            .map(existingStudent -> {
                studentMapper.partialUpdate(existingStudent, studentDTO);

                return existingStudent;
            })
            .flatMap(studentRepository::save)
            .map(studentMapper::toDto);
    }

    /**
     * Get all the students.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Flux<StudentDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Students");
        return studentRepository.findAllBy(pageable).map(studentMapper::toDto);
    }

    /**
     * Returns the number of students available.
     * @return the number of entities in the database.
     *
     */
    public Mono<Long> countAll() {
        return studentRepository.count();
    }

    /**
     * Get one student by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Mono<StudentDTO> findOne(Long id) {
        LOG.debug("Request to get Student : {}", id);
        return studentRepository.findById(id).map(studentMapper::toDto);
    }

    /**
     * Delete the student by id.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Student : {}", id);
        return studentRepository.deleteById(id);
    }
}
