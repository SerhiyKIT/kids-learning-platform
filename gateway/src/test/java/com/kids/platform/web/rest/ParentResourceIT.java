package com.kids.platform.web.rest;

import static com.kids.platform.domain.ParentAsserts.*;
import static com.kids.platform.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kids.platform.IntegrationTest;
import com.kids.platform.domain.Parent;
import com.kids.platform.repository.EntityManager;
import com.kids.platform.repository.ParentRepository;
import com.kids.platform.service.dto.ParentDTO;
import com.kids.platform.service.mapper.ParentMapper;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link ParentResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class ParentResourceIT {

    private static final String DEFAULT_FIRST_NAME = "AAAAAAAAAA";
    private static final String UPDATED_FIRST_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL = "OxNj{@;x-TD.e]5hk";
    private static final String UPDATED_EMAIL = "mgKy@$W6?{q.tT$,#";

    private static final Boolean DEFAULT_IS_PREMIUM = false;
    private static final Boolean UPDATED_IS_PREMIUM = true;

    private static final String ENTITY_API_URL = "/api/parents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentMapper parentMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Parent parent;

    private Parent insertedParent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Parent createEntity() {
        return new Parent().firstName(DEFAULT_FIRST_NAME).email(DEFAULT_EMAIL).isPremium(DEFAULT_IS_PREMIUM);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Parent createUpdatedEntity() {
        return new Parent().firstName(UPDATED_FIRST_NAME).email(UPDATED_EMAIL).isPremium(UPDATED_IS_PREMIUM);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Parent.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        parent = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedParent != null) {
            parentRepository.delete(insertedParent).block();
            insertedParent = null;
        }
        deleteEntities(em);
    }

    @Test
    void createParent() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);
        var returnedParentDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(ParentDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Parent in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedParent = parentMapper.toEntity(returnedParentDTO);
        assertParentUpdatableFieldsEquals(returnedParent, getPersistedParent(returnedParent));

        insertedParent = returnedParent;
    }

    @Test
    void createParentWithExistingId() throws Exception {
        // Create the Parent with an existing ID
        parent.setId(1L);
        ParentDTO parentDTO = parentMapper.toDto(parent);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkFirstNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        parent.setFirstName(null);

        // Create the Parent, which fails.
        ParentDTO parentDTO = parentMapper.toDto(parent);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkEmailIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        parent.setEmail(null);

        // Create the Parent, which fails.
        ParentDTO parentDTO = parentMapper.toDto(parent);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllParents() {
        // Initialize the database
        insertedParent = parentRepository.save(parent).block();

        // Get all the parentList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(parent.getId().intValue()))
            .jsonPath("$.[*].firstName")
            .value(hasItem(DEFAULT_FIRST_NAME))
            .jsonPath("$.[*].email")
            .value(hasItem(DEFAULT_EMAIL))
            .jsonPath("$.[*].isPremium")
            .value(hasItem(DEFAULT_IS_PREMIUM));
    }

    @Test
    void getParent() {
        // Initialize the database
        insertedParent = parentRepository.save(parent).block();

        // Get the parent
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, parent.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(parent.getId().intValue()))
            .jsonPath("$.firstName")
            .value(is(DEFAULT_FIRST_NAME))
            .jsonPath("$.email")
            .value(is(DEFAULT_EMAIL))
            .jsonPath("$.isPremium")
            .value(is(DEFAULT_IS_PREMIUM));
    }

    @Test
    void getNonExistingParent() {
        // Get the parent
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingParent() throws Exception {
        // Initialize the database
        insertedParent = parentRepository.save(parent).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the parent
        Parent updatedParent = parentRepository.findById(parent.getId()).block();
        updatedParent.firstName(UPDATED_FIRST_NAME).email(UPDATED_EMAIL).isPremium(UPDATED_IS_PREMIUM);
        ParentDTO parentDTO = parentMapper.toDto(updatedParent);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, parentDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedParentToMatchAllProperties(updatedParent);
    }

    @Test
    void putNonExistingParent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        parent.setId(longCount.incrementAndGet());

        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, parentDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchParent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        parent.setId(longCount.incrementAndGet());

        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamParent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        parent.setId(longCount.incrementAndGet());

        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateParentWithPatch() throws Exception {
        // Initialize the database
        insertedParent = parentRepository.save(parent).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the parent using partial update
        Parent partialUpdatedParent = new Parent();
        partialUpdatedParent.setId(parent.getId());

        partialUpdatedParent.firstName(UPDATED_FIRST_NAME).email(UPDATED_EMAIL);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedParent.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedParent))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Parent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertParentUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedParent, parent), getPersistedParent(parent));
    }

    @Test
    void fullUpdateParentWithPatch() throws Exception {
        // Initialize the database
        insertedParent = parentRepository.save(parent).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the parent using partial update
        Parent partialUpdatedParent = new Parent();
        partialUpdatedParent.setId(parent.getId());

        partialUpdatedParent.firstName(UPDATED_FIRST_NAME).email(UPDATED_EMAIL).isPremium(UPDATED_IS_PREMIUM);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedParent.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedParent))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Parent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertParentUpdatableFieldsEquals(partialUpdatedParent, getPersistedParent(partialUpdatedParent));
    }

    @Test
    void patchNonExistingParent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        parent.setId(longCount.incrementAndGet());

        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, parentDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchParent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        parent.setId(longCount.incrementAndGet());

        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamParent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        parent.setId(longCount.incrementAndGet());

        // Create the Parent
        ParentDTO parentDTO = parentMapper.toDto(parent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(parentDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Parent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteParent() {
        // Initialize the database
        insertedParent = parentRepository.save(parent).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the parent
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, parent.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return parentRepository.count().block();
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

    protected Parent getPersistedParent(Parent parent) {
        return parentRepository.findById(parent.getId()).block();
    }

    protected void assertPersistedParentToMatchAllProperties(Parent expectedParent) {
        // Test fails because reactive api returns an empty object instead of null
        // assertParentAllPropertiesEquals(expectedParent, getPersistedParent(expectedParent));
        assertParentUpdatableFieldsEquals(expectedParent, getPersistedParent(expectedParent));
    }

    protected void assertPersistedParentToMatchUpdatableProperties(Parent expectedParent) {
        // Test fails because reactive api returns an empty object instead of null
        // assertParentAllUpdatablePropertiesEquals(expectedParent, getPersistedParent(expectedParent));
        assertParentUpdatableFieldsEquals(expectedParent, getPersistedParent(expectedParent));
    }
}
