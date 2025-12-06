package com.kids.ai.web.rest;

import static com.kids.ai.domain.GeneratedStoryAsserts.*;
import static com.kids.ai.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kids.ai.IntegrationTest;
import com.kids.ai.domain.GeneratedStory;
import com.kids.ai.repository.GeneratedStoryRepository;
import com.kids.ai.service.dto.GeneratedStoryDTO;
import com.kids.ai.service.mapper.GeneratedStoryMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the {@link GeneratedStoryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GeneratedStoryResourceIT {

    private static final String DEFAULT_TOPIC = "AAAAAAAAAA";
    private static final String UPDATED_TOPIC = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_AUDIO_URL = "AAAAAAAAAA";
    private static final String UPDATED_AUDIO_URL = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/generated-stories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GeneratedStoryRepository generatedStoryRepository;

    @Autowired
    private GeneratedStoryMapper generatedStoryMapper;

    @Autowired
    private MockMvc restGeneratedStoryMockMvc;

    private GeneratedStory generatedStory;

    private GeneratedStory insertedGeneratedStory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GeneratedStory createEntity() {
        return new GeneratedStory().topic(DEFAULT_TOPIC).content(DEFAULT_CONTENT).audioUrl(DEFAULT_AUDIO_URL).createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GeneratedStory createUpdatedEntity() {
        return new GeneratedStory().topic(UPDATED_TOPIC).content(UPDATED_CONTENT).audioUrl(UPDATED_AUDIO_URL).createdAt(UPDATED_CREATED_AT);
    }

    @BeforeEach
    void initTest() {
        generatedStory = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedGeneratedStory != null) {
            generatedStoryRepository.delete(insertedGeneratedStory);
            insertedGeneratedStory = null;
        }
    }

    @Test
    void createGeneratedStory() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);
        var returnedGeneratedStoryDTO = om.readValue(
            restGeneratedStoryMockMvc
                .perform(
                    post(ENTITY_API_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsBytes(generatedStoryDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GeneratedStoryDTO.class
        );

        // Validate the GeneratedStory in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGeneratedStory = generatedStoryMapper.toEntity(returnedGeneratedStoryDTO);
        assertGeneratedStoryUpdatableFieldsEquals(returnedGeneratedStory, getPersistedGeneratedStory(returnedGeneratedStory));

        insertedGeneratedStory = returnedGeneratedStory;
    }

    @Test
    void createGeneratedStoryWithExistingId() throws Exception {
        // Create the GeneratedStory with an existing ID
        generatedStory.setId("existing_id");
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGeneratedStoryMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllGeneratedStories() throws Exception {
        // Initialize the database
        insertedGeneratedStory = generatedStoryRepository.save(generatedStory);

        // Get all the generatedStoryList
        restGeneratedStoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(generatedStory.getId())))
            .andExpect(jsonPath("$.[*].topic").value(hasItem(DEFAULT_TOPIC)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].audioUrl").value(hasItem(DEFAULT_AUDIO_URL)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())));
    }

    @Test
    void getGeneratedStory() throws Exception {
        // Initialize the database
        insertedGeneratedStory = generatedStoryRepository.save(generatedStory);

        // Get the generatedStory
        restGeneratedStoryMockMvc
            .perform(get(ENTITY_API_URL_ID, generatedStory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(generatedStory.getId()))
            .andExpect(jsonPath("$.topic").value(DEFAULT_TOPIC))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.audioUrl").value(DEFAULT_AUDIO_URL))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    void getNonExistingGeneratedStory() throws Exception {
        // Get the generatedStory
        restGeneratedStoryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingGeneratedStory() throws Exception {
        // Initialize the database
        insertedGeneratedStory = generatedStoryRepository.save(generatedStory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the generatedStory
        GeneratedStory updatedGeneratedStory = generatedStoryRepository.findById(generatedStory.getId()).orElseThrow();
        updatedGeneratedStory.topic(UPDATED_TOPIC).content(UPDATED_CONTENT).audioUrl(UPDATED_AUDIO_URL).createdAt(UPDATED_CREATED_AT);
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(updatedGeneratedStory);

        restGeneratedStoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, generatedStoryDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isOk());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGeneratedStoryToMatchAllProperties(updatedGeneratedStory);
    }

    @Test
    void putNonExistingGeneratedStory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        generatedStory.setId(UUID.randomUUID().toString());

        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGeneratedStoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, generatedStoryDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchGeneratedStory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        generatedStory.setId(UUID.randomUUID().toString());

        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGeneratedStoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamGeneratedStory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        generatedStory.setId(UUID.randomUUID().toString());

        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGeneratedStoryMockMvc
            .perform(
                put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateGeneratedStoryWithPatch() throws Exception {
        // Initialize the database
        insertedGeneratedStory = generatedStoryRepository.save(generatedStory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the generatedStory using partial update
        GeneratedStory partialUpdatedGeneratedStory = new GeneratedStory();
        partialUpdatedGeneratedStory.setId(generatedStory.getId());

        partialUpdatedGeneratedStory
            .topic(UPDATED_TOPIC)
            .content(UPDATED_CONTENT)
            .audioUrl(UPDATED_AUDIO_URL)
            .createdAt(UPDATED_CREATED_AT);

        restGeneratedStoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGeneratedStory.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGeneratedStory))
            )
            .andExpect(status().isOk());

        // Validate the GeneratedStory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGeneratedStoryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGeneratedStory, generatedStory),
            getPersistedGeneratedStory(generatedStory)
        );
    }

    @Test
    void fullUpdateGeneratedStoryWithPatch() throws Exception {
        // Initialize the database
        insertedGeneratedStory = generatedStoryRepository.save(generatedStory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the generatedStory using partial update
        GeneratedStory partialUpdatedGeneratedStory = new GeneratedStory();
        partialUpdatedGeneratedStory.setId(generatedStory.getId());

        partialUpdatedGeneratedStory
            .topic(UPDATED_TOPIC)
            .content(UPDATED_CONTENT)
            .audioUrl(UPDATED_AUDIO_URL)
            .createdAt(UPDATED_CREATED_AT);

        restGeneratedStoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGeneratedStory.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGeneratedStory))
            )
            .andExpect(status().isOk());

        // Validate the GeneratedStory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGeneratedStoryUpdatableFieldsEquals(partialUpdatedGeneratedStory, getPersistedGeneratedStory(partialUpdatedGeneratedStory));
    }

    @Test
    void patchNonExistingGeneratedStory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        generatedStory.setId(UUID.randomUUID().toString());

        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGeneratedStoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, generatedStoryDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchGeneratedStory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        generatedStory.setId(UUID.randomUUID().toString());

        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGeneratedStoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamGeneratedStory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        generatedStory.setId(UUID.randomUUID().toString());

        // Create the GeneratedStory
        GeneratedStoryDTO generatedStoryDTO = generatedStoryMapper.toDto(generatedStory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGeneratedStoryMockMvc
            .perform(
                patch(ENTITY_API_URL)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(generatedStoryDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the GeneratedStory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteGeneratedStory() throws Exception {
        // Initialize the database
        insertedGeneratedStory = generatedStoryRepository.save(generatedStory);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the generatedStory
        restGeneratedStoryMockMvc
            .perform(delete(ENTITY_API_URL_ID, generatedStory.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return generatedStoryRepository.count();
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

    protected GeneratedStory getPersistedGeneratedStory(GeneratedStory generatedStory) {
        return generatedStoryRepository.findById(generatedStory.getId()).orElseThrow();
    }

    protected void assertPersistedGeneratedStoryToMatchAllProperties(GeneratedStory expectedGeneratedStory) {
        assertGeneratedStoryAllPropertiesEquals(expectedGeneratedStory, getPersistedGeneratedStory(expectedGeneratedStory));
    }

    protected void assertPersistedGeneratedStoryToMatchUpdatableProperties(GeneratedStory expectedGeneratedStory) {
        assertGeneratedStoryAllUpdatablePropertiesEquals(expectedGeneratedStory, getPersistedGeneratedStory(expectedGeneratedStory));
    }
}
