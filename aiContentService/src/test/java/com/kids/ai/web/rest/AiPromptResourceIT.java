package com.kids.ai.web.rest;

import static com.kids.ai.domain.AiPromptAsserts.*;
import static com.kids.ai.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kids.ai.IntegrationTest;
import com.kids.ai.domain.AiPrompt;
import com.kids.ai.repository.AiPromptRepository;
import com.kids.ai.service.dto.AiPromptDTO;
import com.kids.ai.service.mapper.AiPromptMapper;
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
 * Integration tests for the {@link AiPromptResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AiPromptResourceIT {

    private static final String DEFAULT_STYLE_NAME = "AAAAAAAAAA";
    private static final String UPDATED_STYLE_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SYSTEM_PROMPT = "AAAAAAAAAA";
    private static final String UPDATED_SYSTEM_PROMPT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/ai-prompts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AiPromptRepository aiPromptRepository;

    @Autowired
    private AiPromptMapper aiPromptMapper;

    @Autowired
    private MockMvc restAiPromptMockMvc;

    private AiPrompt aiPrompt;

    private AiPrompt insertedAiPrompt;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AiPrompt createEntity() {
        return new AiPrompt().styleName(DEFAULT_STYLE_NAME).systemPrompt(DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AiPrompt createUpdatedEntity() {
        return new AiPrompt().styleName(UPDATED_STYLE_NAME).systemPrompt(UPDATED_SYSTEM_PROMPT);
    }

    @BeforeEach
    void initTest() {
        aiPrompt = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAiPrompt != null) {
            aiPromptRepository.delete(insertedAiPrompt);
            insertedAiPrompt = null;
        }
    }

    @Test
    void createAiPrompt() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);
        var returnedAiPromptDTO = om.readValue(
            restAiPromptMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(aiPromptDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AiPromptDTO.class
        );

        // Validate the AiPrompt in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAiPrompt = aiPromptMapper.toEntity(returnedAiPromptDTO);
        assertAiPromptUpdatableFieldsEquals(returnedAiPrompt, getPersistedAiPrompt(returnedAiPrompt));

        insertedAiPrompt = returnedAiPrompt;
    }

    @Test
    void createAiPromptWithExistingId() throws Exception {
        // Create the AiPrompt with an existing ID
        aiPrompt.setId("existing_id");
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAiPromptMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(aiPromptDTO)))
            .andExpect(status().isBadRequest());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkStyleNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        aiPrompt.setStyleName(null);

        // Create the AiPrompt, which fails.
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        restAiPromptMockMvc
            .perform(post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(aiPromptDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllAiPrompts() throws Exception {
        // Initialize the database
        insertedAiPrompt = aiPromptRepository.save(aiPrompt);

        // Get all the aiPromptList
        restAiPromptMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(aiPrompt.getId())))
            .andExpect(jsonPath("$.[*].styleName").value(hasItem(DEFAULT_STYLE_NAME)))
            .andExpect(jsonPath("$.[*].systemPrompt").value(hasItem(DEFAULT_SYSTEM_PROMPT)));
    }

    @Test
    void getAiPrompt() throws Exception {
        // Initialize the database
        insertedAiPrompt = aiPromptRepository.save(aiPrompt);

        // Get the aiPrompt
        restAiPromptMockMvc
            .perform(get(ENTITY_API_URL_ID, aiPrompt.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(aiPrompt.getId()))
            .andExpect(jsonPath("$.styleName").value(DEFAULT_STYLE_NAME))
            .andExpect(jsonPath("$.systemPrompt").value(DEFAULT_SYSTEM_PROMPT));
    }

    @Test
    void getNonExistingAiPrompt() throws Exception {
        // Get the aiPrompt
        restAiPromptMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingAiPrompt() throws Exception {
        // Initialize the database
        insertedAiPrompt = aiPromptRepository.save(aiPrompt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the aiPrompt
        AiPrompt updatedAiPrompt = aiPromptRepository.findById(aiPrompt.getId()).orElseThrow();
        updatedAiPrompt.styleName(UPDATED_STYLE_NAME).systemPrompt(UPDATED_SYSTEM_PROMPT);
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(updatedAiPrompt);

        restAiPromptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, aiPromptDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(aiPromptDTO))
            )
            .andExpect(status().isOk());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAiPromptToMatchAllProperties(updatedAiPrompt);
    }

    @Test
    void putNonExistingAiPrompt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        aiPrompt.setId(UUID.randomUUID().toString());

        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAiPromptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, aiPromptDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(aiPromptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchAiPrompt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        aiPrompt.setId(UUID.randomUUID().toString());

        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAiPromptMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(aiPromptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamAiPrompt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        aiPrompt.setId(UUID.randomUUID().toString());

        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAiPromptMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(aiPromptDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateAiPromptWithPatch() throws Exception {
        // Initialize the database
        insertedAiPrompt = aiPromptRepository.save(aiPrompt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the aiPrompt using partial update
        AiPrompt partialUpdatedAiPrompt = new AiPrompt();
        partialUpdatedAiPrompt.setId(aiPrompt.getId());

        partialUpdatedAiPrompt.styleName(UPDATED_STYLE_NAME).systemPrompt(UPDATED_SYSTEM_PROMPT);

        restAiPromptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAiPrompt.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAiPrompt))
            )
            .andExpect(status().isOk());

        // Validate the AiPrompt in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAiPromptUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedAiPrompt, aiPrompt), getPersistedAiPrompt(aiPrompt));
    }

    @Test
    void fullUpdateAiPromptWithPatch() throws Exception {
        // Initialize the database
        insertedAiPrompt = aiPromptRepository.save(aiPrompt);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the aiPrompt using partial update
        AiPrompt partialUpdatedAiPrompt = new AiPrompt();
        partialUpdatedAiPrompt.setId(aiPrompt.getId());

        partialUpdatedAiPrompt.styleName(UPDATED_STYLE_NAME).systemPrompt(UPDATED_SYSTEM_PROMPT);

        restAiPromptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAiPrompt.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAiPrompt))
            )
            .andExpect(status().isOk());

        // Validate the AiPrompt in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAiPromptUpdatableFieldsEquals(partialUpdatedAiPrompt, getPersistedAiPrompt(partialUpdatedAiPrompt));
    }

    @Test
    void patchNonExistingAiPrompt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        aiPrompt.setId(UUID.randomUUID().toString());

        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAiPromptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, aiPromptDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(aiPromptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchAiPrompt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        aiPrompt.setId(UUID.randomUUID().toString());

        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAiPromptMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID().toString())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(aiPromptDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamAiPrompt() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        aiPrompt.setId(UUID.randomUUID().toString());

        // Create the AiPrompt
        AiPromptDTO aiPromptDTO = aiPromptMapper.toDto(aiPrompt);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAiPromptMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(aiPromptDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the AiPrompt in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteAiPrompt() throws Exception {
        // Initialize the database
        insertedAiPrompt = aiPromptRepository.save(aiPrompt);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the aiPrompt
        restAiPromptMockMvc
            .perform(delete(ENTITY_API_URL_ID, aiPrompt.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return aiPromptRepository.count();
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

    protected AiPrompt getPersistedAiPrompt(AiPrompt aiPrompt) {
        return aiPromptRepository.findById(aiPrompt.getId()).orElseThrow();
    }

    protected void assertPersistedAiPromptToMatchAllProperties(AiPrompt expectedAiPrompt) {
        assertAiPromptAllPropertiesEquals(expectedAiPrompt, getPersistedAiPrompt(expectedAiPrompt));
    }

    protected void assertPersistedAiPromptToMatchUpdatableProperties(AiPrompt expectedAiPrompt) {
        assertAiPromptAllUpdatablePropertiesEquals(expectedAiPrompt, getPersistedAiPrompt(expectedAiPrompt));
    }
}
