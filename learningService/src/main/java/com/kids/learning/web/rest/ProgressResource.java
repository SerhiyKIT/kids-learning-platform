package com.kids.learning.web.rest;

import com.kids.learning.repository.ProgressRepository;
import com.kids.learning.service.ProgressService;
import com.kids.learning.service.dto.ProgressDTO;
import com.kids.learning.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.kids.learning.domain.Progress}.
 */
@RestController
@RequestMapping("/api/progresses")
public class ProgressResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressResource.class);

    private static final String ENTITY_NAME = "learningServiceProgress";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProgressService progressService;

    private final ProgressRepository progressRepository;

    public ProgressResource(ProgressService progressService, ProgressRepository progressRepository) {
        this.progressService = progressService;
        this.progressRepository = progressRepository;
    }

    /**
     * {@code POST  /progresses} : Create a new progress.
     *
     * @param progressDTO the progressDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new progressDTO, or with status {@code 400 (Bad Request)} if the progress has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ProgressDTO> createProgress(@Valid @RequestBody ProgressDTO progressDTO) throws URISyntaxException {
        LOG.debug("REST request to save Progress : {}", progressDTO);
        if (progressDTO.getId() != null) {
            throw new BadRequestAlertException("A new progress cannot already have an ID", ENTITY_NAME, "idexists");
        }
        progressDTO = progressService.save(progressDTO);
        return ResponseEntity.created(new URI("/api/progresses/" + progressDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, progressDTO.getId().toString()))
            .body(progressDTO);
    }

    /**
     * {@code PUT  /progresses/:id} : Updates an existing progress.
     *
     * @param id the id of the progressDTO to save.
     * @param progressDTO the progressDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated progressDTO,
     * or with status {@code 400 (Bad Request)} if the progressDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the progressDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProgressDTO> updateProgress(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ProgressDTO progressDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Progress : {}, {}", id, progressDTO);
        if (progressDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, progressDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!progressRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        progressDTO = progressService.update(progressDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, progressDTO.getId().toString()))
            .body(progressDTO);
    }

    /**
     * {@code PATCH  /progresses/:id} : Partial updates given fields of an existing progress, field will ignore if it is null
     *
     * @param id the id of the progressDTO to save.
     * @param progressDTO the progressDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated progressDTO,
     * or with status {@code 400 (Bad Request)} if the progressDTO is not valid,
     * or with status {@code 404 (Not Found)} if the progressDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the progressDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProgressDTO> partialUpdateProgress(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ProgressDTO progressDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Progress partially : {}, {}", id, progressDTO);
        if (progressDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, progressDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!progressRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProgressDTO> result = progressService.partialUpdate(progressDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, progressDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /progresses} : get all the progresses.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of progresses in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ProgressDTO>> getAllProgresses(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false) Long studentId
    ) {
        LOG.debug("REST request to get a page of Progresses, studentId={}", studentId);
        Page<ProgressDTO> page = studentId != null
            ? progressService.findByStudentId(studentId, pageable)
            : progressService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /progresses/:id} : get the "id" progress.
     *
     * @param id the id of the progressDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the progressDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProgressDTO> getProgress(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Progress : {}", id);
        Optional<ProgressDTO> progressDTO = progressService.findOne(id);
        return ResponseUtil.wrapOrNotFound(progressDTO);
    }

    /**
     * {@code DELETE  /progresses/:id} : delete the "id" progress.
     *
     * @param id the id of the progressDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgress(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Progress : {}", id);
        progressService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
