package com.kids.learning.web.rest;

import static com.kids.learning.domain.ProgressAsserts.*;
import static com.kids.learning.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kids.learning.IntegrationTest;
import com.kids.learning.domain.Progress;
import com.kids.learning.repository.ProgressRepository;
import com.kids.learning.service.dto.ProgressDTO;
import com.kids.learning.service.mapper.ProgressMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ProgressResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProgressResourceIT {

    private static final Long DEFAULT_STUDENT_ID = 1L;
    private static final Long UPDATED_STUDENT_ID = 2L;

    private static final Integer DEFAULT_SCORE = 1;
    private static final Integer UPDATED_SCORE = 2;

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_COMPLETED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_COMPLETED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/progresses";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private ProgressMapper progressMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProgressMockMvc;

    private Progress progress;

    private Progress insertedProgress;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Progress createEntity() {
        return new Progress().studentId(DEFAULT_STUDENT_ID).score(DEFAULT_SCORE).status(DEFAULT_STATUS).completedAt(DEFAULT_COMPLETED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Progress createUpdatedEntity() {
        return new Progress().studentId(UPDATED_STUDENT_ID).score(UPDATED_SCORE).status(UPDATED_STATUS).completedAt(UPDATED_COMPLETED_AT);
    }

    @BeforeEach
    void initTest() {
        progress = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedProgress != null) {
            progressRepository.delete(insertedProgress);
            insertedProgress = null;
        }
    }

    @Test
    @Transactional
    void createProgress() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);
        var returnedProgressDTO = om.readValue(
            restProgressMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ProgressDTO.class
        );

        // Validate the Progress in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedProgress = progressMapper.toEntity(returnedProgressDTO);
        assertProgressUpdatableFieldsEquals(returnedProgress, getPersistedProgress(returnedProgress));

        insertedProgress = returnedProgress;
    }

    @Test
    @Transactional
    void createProgressWithExistingId() throws Exception {
        // Create the Progress with an existing ID
        progress.setId(1L);
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProgressMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkStudentIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        progress.setStudentId(null);

        // Create the Progress, which fails.
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        restProgressMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllProgresses() throws Exception {
        // Initialize the database
        insertedProgress = progressRepository.saveAndFlush(progress);

        // Get all the progressList
        restProgressMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(progress.getId().intValue())))
            .andExpect(jsonPath("$.[*].studentId").value(hasItem(DEFAULT_STUDENT_ID.intValue())))
            .andExpect(jsonPath("$.[*].score").value(hasItem(DEFAULT_SCORE)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())));
    }

    @Test
    @Transactional
    void getProgress() throws Exception {
        // Initialize the database
        insertedProgress = progressRepository.saveAndFlush(progress);

        // Get the progress
        restProgressMockMvc
            .perform(get(ENTITY_API_URL_ID, progress.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(progress.getId().intValue()))
            .andExpect(jsonPath("$.studentId").value(DEFAULT_STUDENT_ID.intValue()))
            .andExpect(jsonPath("$.score").value(DEFAULT_SCORE))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.completedAt").value(DEFAULT_COMPLETED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingProgress() throws Exception {
        // Get the progress
        restProgressMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingProgress() throws Exception {
        // Initialize the database
        insertedProgress = progressRepository.saveAndFlush(progress);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the progress
        Progress updatedProgress = progressRepository.findById(progress.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedProgress are not directly saved in db
        em.detach(updatedProgress);
        updatedProgress.studentId(UPDATED_STUDENT_ID).score(UPDATED_SCORE).status(UPDATED_STATUS).completedAt(UPDATED_COMPLETED_AT);
        ProgressDTO progressDTO = progressMapper.toDto(updatedProgress);

        restProgressMockMvc
            .perform(
                put(ENTITY_API_URL_ID, progressDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(progressDTO))
            )
            .andExpect(status().isOk());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedProgressToMatchAllProperties(updatedProgress);
    }

    @Test
    @Transactional
    void putNonExistingProgress() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progress.setId(longCount.incrementAndGet());

        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProgressMockMvc
            .perform(
                put(ENTITY_API_URL_ID, progressDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(progressDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProgress() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progress.setId(longCount.incrementAndGet());

        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(progressDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProgress() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progress.setId(longCount.incrementAndGet());

        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(progressDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProgressWithPatch() throws Exception {
        // Initialize the database
        insertedProgress = progressRepository.saveAndFlush(progress);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the progress using partial update
        Progress partialUpdatedProgress = new Progress();
        partialUpdatedProgress.setId(progress.getId());

        partialUpdatedProgress.completedAt(UPDATED_COMPLETED_AT);

        restProgressMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProgress.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProgress))
            )
            .andExpect(status().isOk());

        // Validate the Progress in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProgressUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedProgress, progress), getPersistedProgress(progress));
    }

    @Test
    @Transactional
    void fullUpdateProgressWithPatch() throws Exception {
        // Initialize the database
        insertedProgress = progressRepository.saveAndFlush(progress);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the progress using partial update
        Progress partialUpdatedProgress = new Progress();
        partialUpdatedProgress.setId(progress.getId());

        partialUpdatedProgress.studentId(UPDATED_STUDENT_ID).score(UPDATED_SCORE).status(UPDATED_STATUS).completedAt(UPDATED_COMPLETED_AT);

        restProgressMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProgress.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedProgress))
            )
            .andExpect(status().isOk());

        // Validate the Progress in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertProgressUpdatableFieldsEquals(partialUpdatedProgress, getPersistedProgress(partialUpdatedProgress));
    }

    @Test
    @Transactional
    void patchNonExistingProgress() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progress.setId(longCount.incrementAndGet());

        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProgressMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, progressDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(progressDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProgress() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progress.setId(longCount.incrementAndGet());

        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(progressDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProgress() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        progress.setId(longCount.incrementAndGet());

        // Create the Progress
        ProgressDTO progressDTO = progressMapper.toDto(progress);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProgressMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(progressDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Progress in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProgress() throws Exception {
        // Initialize the database
        insertedProgress = progressRepository.saveAndFlush(progress);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the progress
        restProgressMockMvc
            .perform(delete(ENTITY_API_URL_ID, progress.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return progressRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Progress getPersistedProgress(Progress progress) {
        return progressRepository.findById(progress.getId()).orElseThrow();
    }

    protected void assertPersistedProgressToMatchAllProperties(Progress expectedProgress) {
        assertProgressAllPropertiesEquals(expectedProgress, getPersistedProgress(expectedProgress));
    }

    protected void assertPersistedProgressToMatchUpdatableProperties(Progress expectedProgress) {
        assertProgressAllUpdatablePropertiesEquals(expectedProgress, getPersistedProgress(expectedProgress));
    }
}
