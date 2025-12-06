package com.kids.ai.web.rest;

import com.kids.ai.repository.GeneratedStoryRepository;
import com.kids.ai.service.GeneratedStoryService;
import com.kids.ai.service.dto.GeneratedStoryDTO;
import com.kids.ai.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link com.kids.ai.domain.GeneratedStory}.
 */
@RestController
@RequestMapping("/api/generated-stories")
public class GeneratedStoryResource {

    private static final Logger LOG = LoggerFactory.getLogger(GeneratedStoryResource.class);

    private static final String ENTITY_NAME = "aiContentServiceGeneratedStory";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GeneratedStoryService generatedStoryService;

    private final GeneratedStoryRepository generatedStoryRepository;

    public GeneratedStoryResource(GeneratedStoryService generatedStoryService, GeneratedStoryRepository generatedStoryRepository) {
        this.generatedStoryService = generatedStoryService;
        this.generatedStoryRepository = generatedStoryRepository;
    }

    /**
     * {@code POST  /generated-stories} : Create a new generatedStory.
     *
     * @param generatedStoryDTO the generatedStoryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new generatedStoryDTO, or with status {@code 400 (Bad Request)} if the generatedStory has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GeneratedStoryDTO> createGeneratedStory(@RequestBody GeneratedStoryDTO generatedStoryDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save GeneratedStory : {}", generatedStoryDTO);
        if (generatedStoryDTO.getId() != null) {
            throw new BadRequestAlertException("A new generatedStory cannot already have an ID", ENTITY_NAME, "idexists");
        }
        generatedStoryDTO = generatedStoryService.save(generatedStoryDTO);
        return ResponseEntity.created(new URI("/api/generated-stories/" + generatedStoryDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, generatedStoryDTO.getId()))
            .body(generatedStoryDTO);
    }

    /**
     * {@code PUT  /generated-stories/:id} : Updates an existing generatedStory.
     *
     * @param id the id of the generatedStoryDTO to save.
     * @param generatedStoryDTO the generatedStoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated generatedStoryDTO,
     * or with status {@code 400 (Bad Request)} if the generatedStoryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the generatedStoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GeneratedStoryDTO> updateGeneratedStory(
        @PathVariable(value = "id", required = false) final String id,
        @RequestBody GeneratedStoryDTO generatedStoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GeneratedStory : {}, {}", id, generatedStoryDTO);
        if (generatedStoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, generatedStoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!generatedStoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        generatedStoryDTO = generatedStoryService.update(generatedStoryDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, generatedStoryDTO.getId()))
            .body(generatedStoryDTO);
    }

    /**
     * {@code PATCH  /generated-stories/:id} : Partial updates given fields of an existing generatedStory, field will ignore if it is null
     *
     * @param id the id of the generatedStoryDTO to save.
     * @param generatedStoryDTO the generatedStoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated generatedStoryDTO,
     * or with status {@code 400 (Bad Request)} if the generatedStoryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the generatedStoryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the generatedStoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GeneratedStoryDTO> partialUpdateGeneratedStory(
        @PathVariable(value = "id", required = false) final String id,
        @RequestBody GeneratedStoryDTO generatedStoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update GeneratedStory partially : {}, {}", id, generatedStoryDTO);
        if (generatedStoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, generatedStoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!generatedStoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GeneratedStoryDTO> result = generatedStoryService.partialUpdate(generatedStoryDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, generatedStoryDTO.getId())
        );
    }

    /**
     * {@code GET  /generated-stories} : get all the generatedStories.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of generatedStories in body.
     */
    @GetMapping("")
    public ResponseEntity<List<GeneratedStoryDTO>> getAllGeneratedStories(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of GeneratedStories");
        Page<GeneratedStoryDTO> page = generatedStoryService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /generated-stories/:id} : get the "id" generatedStory.
     *
     * @param id the id of the generatedStoryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the generatedStoryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GeneratedStoryDTO> getGeneratedStory(@PathVariable("id") String id) {
        LOG.debug("REST request to get GeneratedStory : {}", id);
        Optional<GeneratedStoryDTO> generatedStoryDTO = generatedStoryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(generatedStoryDTO);
    }

    /**
     * {@code DELETE  /generated-stories/:id} : delete the "id" generatedStory.
     *
     * @param id the id of the generatedStoryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGeneratedStory(@PathVariable("id") String id) {
        LOG.debug("REST request to delete GeneratedStory : {}", id);
        generatedStoryService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }
}
