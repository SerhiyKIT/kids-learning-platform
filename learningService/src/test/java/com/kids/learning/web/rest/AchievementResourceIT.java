package com.kids.learning.web.rest;

import static com.kids.learning.domain.AchievementAsserts.*;
import static com.kids.learning.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kids.learning.IntegrationTest;
import com.kids.learning.domain.Achievement;
import com.kids.learning.repository.AchievementRepository;
import com.kids.learning.service.dto.AchievementDTO;
import com.kids.learning.service.mapper.AchievementMapper;
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
 * Integration tests for the {@link AchievementResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AchievementResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_ICON_URL = "AAAAAAAAAA";
    private static final String UPDATED_ICON_URL = "BBBBBBBBBB";

    private static final Instant DEFAULT_DATE_EARNED = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE_EARNED = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/achievements";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private AchievementMapper achievementMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAchievementMockMvc;

    private Achievement achievement;

    private Achievement insertedAchievement;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Achievement createEntity() {
        return new Achievement().title(DEFAULT_TITLE).iconUrl(DEFAULT_ICON_URL).dateEarned(DEFAULT_DATE_EARNED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Achievement createUpdatedEntity() {
        return new Achievement().title(UPDATED_TITLE).iconUrl(UPDATED_ICON_URL).dateEarned(UPDATED_DATE_EARNED);
    }

    @BeforeEach
    void initTest() {
        achievement = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAchievement != null) {
            achievementRepository.delete(insertedAchievement);
            insertedAchievement = null;
        }
    }

    @Test
    @Transactional
    void createAchievement() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);
        var returnedAchievementDTO = om.readValue(
            restAchievementMockMvc
                .perform(
                    post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(achievementDTO))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AchievementDTO.class
        );

        // Validate the Achievement in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAchievement = achievementMapper.toEntity(returnedAchievementDTO);
        assertAchievementUpdatableFieldsEquals(returnedAchievement, getPersistedAchievement(returnedAchievement));

        insertedAchievement = returnedAchievement;
    }

    @Test
    @Transactional
    void createAchievementWithExistingId() throws Exception {
        // Create the Achievement with an existing ID
        achievement.setId(1L);
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAchievementMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAchievements() throws Exception {
        // Initialize the database
        insertedAchievement = achievementRepository.saveAndFlush(achievement);

        // Get all the achievementList
        restAchievementMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(achievement.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].iconUrl").value(hasItem(DEFAULT_ICON_URL)))
            .andExpect(jsonPath("$.[*].dateEarned").value(hasItem(DEFAULT_DATE_EARNED.toString())));
    }

    @Test
    @Transactional
    void getAchievement() throws Exception {
        // Initialize the database
        insertedAchievement = achievementRepository.saveAndFlush(achievement);

        // Get the achievement
        restAchievementMockMvc
            .perform(get(ENTITY_API_URL_ID, achievement.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(achievement.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.iconUrl").value(DEFAULT_ICON_URL))
            .andExpect(jsonPath("$.dateEarned").value(DEFAULT_DATE_EARNED.toString()));
    }

    @Test
    @Transactional
    void getNonExistingAchievement() throws Exception {
        // Get the achievement
        restAchievementMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAchievement() throws Exception {
        // Initialize the database
        insertedAchievement = achievementRepository.saveAndFlush(achievement);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the achievement
        Achievement updatedAchievement = achievementRepository.findById(achievement.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAchievement are not directly saved in db
        em.detach(updatedAchievement);
        updatedAchievement.title(UPDATED_TITLE).iconUrl(UPDATED_ICON_URL).dateEarned(UPDATED_DATE_EARNED);
        AchievementDTO achievementDTO = achievementMapper.toDto(updatedAchievement);

        restAchievementMockMvc
            .perform(
                put(ENTITY_API_URL_ID, achievementDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isOk());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAchievementToMatchAllProperties(updatedAchievement);
    }

    @Test
    @Transactional
    void putNonExistingAchievement() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        achievement.setId(longCount.incrementAndGet());

        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAchievementMockMvc
            .perform(
                put(ENTITY_API_URL_ID, achievementDTO.getId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAchievement() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        achievement.setId(longCount.incrementAndGet());

        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAchievementMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAchievement() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        achievement.setId(longCount.incrementAndGet());

        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAchievementMockMvc
            .perform(put(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(achievementDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAchievementWithPatch() throws Exception {
        // Initialize the database
        insertedAchievement = achievementRepository.saveAndFlush(achievement);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the achievement using partial update
        Achievement partialUpdatedAchievement = new Achievement();
        partialUpdatedAchievement.setId(achievement.getId());

        partialUpdatedAchievement.title(UPDATED_TITLE).dateEarned(UPDATED_DATE_EARNED);

        restAchievementMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAchievement.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAchievement))
            )
            .andExpect(status().isOk());

        // Validate the Achievement in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAchievementUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAchievement, achievement),
            getPersistedAchievement(achievement)
        );
    }

    @Test
    @Transactional
    void fullUpdateAchievementWithPatch() throws Exception {
        // Initialize the database
        insertedAchievement = achievementRepository.saveAndFlush(achievement);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the achievement using partial update
        Achievement partialUpdatedAchievement = new Achievement();
        partialUpdatedAchievement.setId(achievement.getId());

        partialUpdatedAchievement.title(UPDATED_TITLE).iconUrl(UPDATED_ICON_URL).dateEarned(UPDATED_DATE_EARNED);

        restAchievementMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAchievement.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAchievement))
            )
            .andExpect(status().isOk());

        // Validate the Achievement in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAchievementUpdatableFieldsEquals(partialUpdatedAchievement, getPersistedAchievement(partialUpdatedAchievement));
    }

    @Test
    @Transactional
    void patchNonExistingAchievement() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        achievement.setId(longCount.incrementAndGet());

        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAchievementMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, achievementDTO.getId())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAchievement() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        achievement.setId(longCount.incrementAndGet());

        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAchievementMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAchievement() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        achievement.setId(longCount.incrementAndGet());

        // Create the Achievement
        AchievementDTO achievementDTO = achievementMapper.toDto(achievement);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAchievementMockMvc
            .perform(
                patch(ENTITY_API_URL).with(csrf()).contentType("application/merge-patch+json").content(om.writeValueAsBytes(achievementDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the Achievement in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAchievement() throws Exception {
        // Initialize the database
        insertedAchievement = achievementRepository.saveAndFlush(achievement);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the achievement
        restAchievementMockMvc
            .perform(delete(ENTITY_API_URL_ID, achievement.getId()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return achievementRepository.count();
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

    protected Achievement getPersistedAchievement(Achievement achievement) {
        return achievementRepository.findById(achievement.getId()).orElseThrow();
    }

    protected void assertPersistedAchievementToMatchAllProperties(Achievement expectedAchievement) {
        assertAchievementAllPropertiesEquals(expectedAchievement, getPersistedAchievement(expectedAchievement));
    }

    protected void assertPersistedAchievementToMatchUpdatableProperties(Achievement expectedAchievement) {
        assertAchievementAllUpdatablePropertiesEquals(expectedAchievement, getPersistedAchievement(expectedAchievement));
    }
}
