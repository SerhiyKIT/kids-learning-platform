package com.kids.ai.web.rest;

import com.kids.ai.repository.AiPromptRepository;
import com.kids.ai.service.AiPromptService;
import com.kids.ai.service.dto.AiPromptDTO;
import com.kids.ai.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link com.kids.ai.domain.AiPrompt}.
 */
@RestController
@RequestMapping("/api/ai-prompts")
public class AiPromptResource {

    private static final Logger LOG = LoggerFactory.getLogger(AiPromptResource.class);

    private static final String ENTITY_NAME = "aiContentServiceAiPrompt";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AiPromptService aiPromptService;

    private final AiPromptRepository aiPromptRepository;

    public AiPromptResource(AiPromptService aiPromptService, AiPromptRepository aiPromptRepository) {
        this.aiPromptService = aiPromptService;
        this.aiPromptRepository = aiPromptRepository;
    }

    /**
     * {@code POST  /ai-prompts} : Create a new aiPrompt.
     *
     * @param aiPromptDTO the aiPromptDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new aiPromptDTO, or with status {@code 400 (Bad Request)} if the aiPrompt has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AiPromptDTO> createAiPrompt(@Valid @RequestBody AiPromptDTO aiPromptDTO) throws URISyntaxException {
        LOG.debug("REST request to save AiPrompt : {}", aiPromptDTO);
        if (aiPromptDTO.getId() != null) {
            throw new BadRequestAlertException("A new aiPrompt cannot already have an ID", ENTITY_NAME, "idexists");
        }
        aiPromptDTO = aiPromptService.save(aiPromptDTO);
        return ResponseEntity.created(new URI("/api/ai-prompts/" + aiPromptDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, aiPromptDTO.getId()))
            .body(aiPromptDTO);
    }

    /**
     * {@code PUT  /ai-prompts/:id} : Updates an existing aiPrompt.
     *
     * @param id the id of the aiPromptDTO to save.
     * @param aiPromptDTO the aiPromptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated aiPromptDTO,
     * or with status {@code 400 (Bad Request)} if the aiPromptDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the aiPromptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AiPromptDTO> updateAiPrompt(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody AiPromptDTO aiPromptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AiPrompt : {}, {}", id, aiPromptDTO);
        if (aiPromptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, aiPromptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!aiPromptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        aiPromptDTO = aiPromptService.update(aiPromptDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, aiPromptDTO.getId()))
            .body(aiPromptDTO);
    }

    /**
     * {@code PATCH  /ai-prompts/:id} : Partial updates given fields of an existing aiPrompt, field will ignore if it is null
     *
     * @param id the id of the aiPromptDTO to save.
     * @param aiPromptDTO the aiPromptDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated aiPromptDTO,
     * or with status {@code 400 (Bad Request)} if the aiPromptDTO is not valid,
     * or with status {@code 404 (Not Found)} if the aiPromptDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the aiPromptDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AiPromptDTO> partialUpdateAiPrompt(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody AiPromptDTO aiPromptDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AiPrompt partially : {}, {}", id, aiPromptDTO);
        if (aiPromptDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, aiPromptDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!aiPromptRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AiPromptDTO> result = aiPromptService.partialUpdate(aiPromptDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, aiPromptDTO.getId())
        );
    }

    /**
     * {@code GET  /ai-prompts} : get all the aiPrompts.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of aiPrompts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AiPromptDTO>> getAllAiPrompts(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of AiPrompts");
        Page<AiPromptDTO> page = aiPromptService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /ai-prompts/:id} : get the "id" aiPrompt.
     *
     * @param id the id of the aiPromptDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the aiPromptDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AiPromptDTO> getAiPrompt(@PathVariable("id") String id) {
        LOG.debug("REST request to get AiPrompt : {}", id);
        Optional<AiPromptDTO> aiPromptDTO = aiPromptService.findOne(id);
        return ResponseUtil.wrapOrNotFound(aiPromptDTO);
    }

    /**
     * {@code DELETE  /ai-prompts/:id} : delete the "id" aiPrompt.
     *
     * @param id the id of the aiPromptDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAiPrompt(@PathVariable("id") String id) {
        LOG.debug("REST request to delete AiPrompt : {}", id);
        aiPromptService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }
}
