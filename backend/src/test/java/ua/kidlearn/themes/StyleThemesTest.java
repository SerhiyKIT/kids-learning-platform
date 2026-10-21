package ua.kidlearn.themes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import ua.kidlearn.support.WebIntegrationTest;
import ua.kidlearn.twofa.AdminTwoFactorTestSupport;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Style personalization end to end on the stub generator: consent gate, one generated theme per
 * preference key, the child only ever seeing approved themes, moderation, revocation and deletion.
 * NOT @Transactional: theme generation runs after commit on another thread (like audit), so it
 * would never happen inside a rolled-back test transaction. Everything created is removed in
 * {@link #cleanUp()}; the preference keys below are used by no other test.
 */
class StyleThemesTest extends WebIntegrationTest {

	private static final String PASSWORD = "supersecret1";
	private static final List<String> TEST_KEYS = List.of("green|forest|bright", "purple|dinosaurs|calm",
			"orange|cars|bright", "red|sea|calm");
	private static final String DEFAULT_THEME_ID = Theme.DEFAULT_ID.toString();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<User> createdUsers = new ArrayList<>();
	private final List<UUID> createdChildren = new ArrayList<>();

	@BeforeEach
	@AfterEach
	void cleanUp() {
		createdChildren.forEach(id -> jdbcTemplate.update("DELETE FROM children WHERE id = ?", id));
		createdChildren.clear();
		for (String key : TEST_KEYS) {
			jdbcTemplate.update("""
					UPDATE child_style_prefs SET theme_id = NULL
					WHERE theme_id IN (SELECT id FROM themes WHERE preference_key = ?)""", key);
			jdbcTemplate.update("DELETE FROM themes WHERE preference_key = ?", key);
		}
		// Soft delete, like AuditLogTest: an ADMIN left behind would satisfy "an admin exists" checks
		// elsewhere, and approved_by may still reference it.
		createdUsers.forEach(user -> user.markDeleted(Instant.now()));
		userRepository.saveAll(createdUsers);
		createdUsers.clear();
	}

	private MockHttpSession login(Role role) throws Exception {
		String email = "style-" + role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.test";
		User user = new User(email, passwordEncoder.encode(PASSWORD), role, "Test", "uk");
		user.markEmailVerified(Instant.now());
		createdUsers.add(userRepository.save(user));
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession admin() throws Exception {
		MockHttpSession session = login(Role.ADMIN);
		AdminTwoFactorTestSupport.completeSetup(mockMvc, session);
		return session;
	}

	private UUID child(MockHttpSession parent) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/children").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Соломійка\",\"birthYear\":%d,\"relation\":\"mother\"}"
								.formatted(Year.now().getValue() - 5)))
				.andExpect(status().isCreated())
				.andReturn();
		UUID id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
		createdChildren.add(id);
		mockMvc.perform(post("/api/children/" + id + "/consent").with(csrf()).session(parent))
				.andExpect(status().isOk());
		return id;
	}

	private ResultActions consent(MockHttpSession parent, UUID childId, boolean grant) throws Exception {
		String path = "/api/children/" + childId + "/consents/style_personalization";
		return mockMvc.perform((grant ? put(path) : delete(path)).with(csrf()).session(parent));
	}

	private ResultActions choose(MockHttpSession parent, UUID childId, String key) throws Exception {
		String[] ids = key.split("\\|");
		return mockMvc.perform(put("/api/children/" + childId + "/style").with(csrf())
				.session(parent)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"colorId\":\"%s\",\"worldId\":\"%s\",\"moodId\":\"%s\"}".formatted(ids[0], ids[1], ids[2])));
	}

	private ResultActions style(MockHttpSession parent, UUID childId) throws Exception {
		return mockMvc.perform(get("/api/children/" + childId + "/style").session(parent));
	}

	/** Waits for the async generation; returns the theme id. */
	private UUID awaitTheme(String key) {
		Awaitility.await().atMost(Duration.ofSeconds(10))
				.until(() -> jdbcTemplate.queryForObject("SELECT count(*) FROM themes WHERE preference_key = ?",
						Integer.class, key) == 1);
		return jdbcTemplate.queryForObject("SELECT id FROM themes WHERE preference_key = ?", UUID.class, key);
	}

	private int generationAudits(UUID themeId) {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM audit_log WHERE action = 'GENERATE_THEME' AND target_id = ?", Integer.class, themeId);
	}

	private int prefsRows(UUID childId) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM child_style_prefs WHERE child_id = ?", Integer.class,
				childId);
	}

	@Test
	void withoutConsentAnswersAreRefusedAndTheChildHasTheDefaultTheme() throws Exception {
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);

		choose(parent, childId, "green|forest|bright")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("STYLE_CONSENT_REQUIRED"));
		style(parent, childId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.consented").value(false))
				.andExpect(jsonPath("$.answers").doesNotExist())
				.andExpect(jsonPath("$.theme.id").value(DEFAULT_THEME_ID))
				.andExpect(jsonPath("$.theme.palette.sun").value("#ffc13b"))
				.andExpect(jsonPath("$.pending").value(false));
		assertThat(prefsRows(childId)).isZero();
	}

	@Test
	void unknownIdsAreRejected() throws Exception {
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);
		consent(parent, childId, true).andExpect(status().isNoContent());

		choose(parent, childId, "pink|forest|calm").andExpect(status().isBadRequest());
		choose(parent, childId, "green|jungle|calm").andExpect(status().isBadRequest());
		choose(parent, childId, "green|forest|sad").andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/children/" + childId + "/style").with(csrf())
						.session(parent)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"colorId\":\"green\",\"worldId\":\"forest\"}"))
				.andExpect(status().isBadRequest());
		assertThat(prefsRows(childId)).isZero();
	}

	@Test
	void oneThemePerKeyOnlyApprovedIsShownAndApprovalReachesEveryChildWithThatKey() throws Exception {
		String key = "purple|dinosaurs|calm";
		MockHttpSession parentA = login(Role.PARENT);
		UUID childA = child(parentA);
		consent(parentA, childA, true).andExpect(status().isNoContent());

		choose(parentA, childA, key)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.answers.colorId").value("purple"))
				.andExpect(jsonPath("$.pending").value(true))
				.andExpect(jsonPath("$.theme.id").value(DEFAULT_THEME_ID));
		UUID themeId = awaitTheme(key);
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM themes WHERE id = ?", String.class, themeId))
				.isEqualTo("draft");

		// A second child (another family) with the same answers: no second generation.
		MockHttpSession parentB = login(Role.PARENT);
		UUID childB = child(parentB);
		consent(parentB, childB, true).andExpect(status().isNoContent());
		choose(parentB, childB, key).andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(true));
		choose(parentA, childA, key).andExpect(status().isOk());

		// The draft is never shown to a child.
		style(parentA, childA).andExpect(jsonPath("$.theme.id").value(DEFAULT_THEME_ID));

		MockHttpSession admin = admin();
		mockMvc.perform(get("/api/admin/themes").param("status", "draft").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '%s')].preferenceKey".formatted(themeId)).value(key))
				.andExpect(jsonPath("$[?(@.id == '%s')].contrast.length()".formatted(themeId)).value(10));
		mockMvc.perform(post("/api/admin/themes/" + themeId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("approved"));

		for (var pair : List.of(List.of(parentA, childA), List.of(parentB, childB))) {
			style((MockHttpSession) pair.get(0), (UUID) pair.get(1))
					.andExpect(jsonPath("$.theme.id").value(themeId.toString()))
					.andExpect(jsonPath("$.theme.worldId").value("dinosaurs"))
					.andExpect(jsonPath("$.pending").value(false))
					.andExpect(jsonPath("$.announce").value(true));
		}
		mockMvc.perform(post("/api/children/" + childA + "/style/seen").with(csrf()).session(parentA))
				.andExpect(status().isNoContent());
		style(parentA, childA).andExpect(jsonPath("$.announce").value(false));

		// After approval a newcomer with the same answers gets the theme straight away.
		MockHttpSession parentC = login(Role.PARENT);
		UUID childC = child(parentC);
		consent(parentC, childC, true).andExpect(status().isNoContent());
		choose(parentC, childC, key)
				.andExpect(jsonPath("$.pending").value(false))
				.andExpect(jsonPath("$.theme.id").value(themeId.toString()));

		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> generationAudits(themeId) == 1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'APPROVE_THEME' AND target_id = ?",
				Integer.class, themeId)).isEqualTo(1);
	}

	@Test
	void changingTheAnswersKeepsTheApprovedThemeUntilTheNewOneIsApproved() throws Exception {
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);
		consent(parent, childId, true).andExpect(status().isNoContent());
		choose(parent, childId, "orange|cars|bright");
		UUID first = awaitTheme("orange|cars|bright");
		MockHttpSession admin = admin();
		mockMvc.perform(post("/api/admin/themes/" + first + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk());

		choose(parent, childId, "red|sea|calm")
				.andExpect(jsonPath("$.answers.colorId").value("red"))
				.andExpect(jsonPath("$.pending").value(true))
				.andExpect(jsonPath("$.theme.id").value(first.toString()));
		awaitTheme("red|sea|calm");
	}

	@Test
	void moderationRejectsRegeneratesAndAuditsEveryDecision() throws Exception {
		String key = "green|forest|bright";
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);
		consent(parent, childId, true).andExpect(status().isNoContent());
		choose(parent, childId, key);
		UUID themeId = awaitTheme(key);
		MockHttpSession admin = admin();

		mockMvc.perform(post("/api/admin/themes/" + themeId + "/reject").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"reason\":\"Занадто темний\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("rejected"))
				.andExpect(jsonPath("$.rejectReason").value("Занадто темний"));
		mockMvc.perform(get("/api/admin/themes").param("status", "rejected").session(admin))
				.andExpect(jsonPath("$[?(@.id == '%s')].rejectReason".formatted(themeId)).value("Занадто темний"));
		mockMvc.perform(post("/api/admin/themes/" + themeId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isConflict());
		// Rejected: the child stays on theme_default and no new generation starts by itself.
		choose(parent, childId, key).andExpect(jsonPath("$.theme.id").value(DEFAULT_THEME_ID));

		mockMvc.perform(post("/api/admin/themes/regenerate").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"preferenceKey\":\"%s\"}".formatted(key)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(themeId.toString()))
				.andExpect(jsonPath("$.status").value("draft"));
		mockMvc.perform(post("/api/admin/themes/regenerate").with(csrf())
						.session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"preferenceKey\":\"%s\"}".formatted(key)))
				.andExpect(status().isConflict());

		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> jdbcTemplate.queryForObject(
				"SELECT count(*) FROM audit_log WHERE target_type = 'theme' AND target_id = ? AND action IN ('REJECT_THEME', 'GENERATE_THEME')",
				Integer.class, themeId) == 3);

		MockHttpSession teacher = login(Role.TEACHER);
		mockMvc.perform(get("/api/admin/themes").session(teacher)).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/themes").param("status", "bogus").session(admin))
				.andExpect(status().isBadRequest());
	}

	@Test
	void revokingConsentDeletesTheAnswersAndTheChildGoesBackToTheDefault() throws Exception {
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);
		consent(parent, childId, true).andExpect(status().isNoContent());
		consent(parent, childId, true).andExpect(status().isNoContent()); // idempotent
		choose(parent, childId, "red|sea|calm").andExpect(status().isOk());
		awaitTheme("red|sea|calm");
		assertThat(prefsRows(childId)).isEqualTo(1);

		consent(parent, childId, false).andExpect(status().isNoContent());

		assertThat(prefsRows(childId)).isZero();
		style(parent, childId)
				.andExpect(jsonPath("$.consented").value(false))
				.andExpect(jsonPath("$.answers").doesNotExist())
				.andExpect(jsonPath("$.theme.id").value(DEFAULT_THEME_ID));
		choose(parent, childId, "red|sea|calm").andExpect(status().isForbidden());
		// The parent sees the consent's history: granted once (idempotent), then revoked.
		String consents = mockMvc.perform(get("/api/children/" + childId + "/consents").session(parent))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		List<String> styleRevokedAt = JsonPath.read(consents, "$[?(@.type == 'style_personalization')].revokedAt");
		List<String> accountRevokedAt = JsonPath.read(consents, "$[?(@.type == 'account')].revokedAt");
		assertThat(styleRevokedAt).hasSize(1).doesNotContainNull();
		assertThat(accountRevokedAt).containsExactly((String) null);
	}

	@Test
	void deletingTheChildDeletesTheirAnswers() throws Exception {
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);
		consent(parent, childId, true).andExpect(status().isNoContent());
		choose(parent, childId, "red|sea|calm").andExpect(status().isOk());
		assertThat(prefsRows(childId)).isEqualTo(1);

		mockMvc.perform(delete("/api/children/" + childId).with(csrf()).session(parent))
				.andExpect(status().isNoContent());

		assertThat(prefsRows(childId)).isZero();
		awaitTheme("red|sea|calm");
	}

	@Test
	void onlyThisChildsParentsAndOnlyToggleableConsentTypes() throws Exception {
		MockHttpSession parent = login(Role.PARENT);
		UUID childId = child(parent);
		MockHttpSession stranger = login(Role.PARENT);

		style(stranger, childId).andExpect(status().isNotFound());
		choose(stranger, childId, "red|sea|calm").andExpect(status().isNotFound());
		mockMvc.perform(post("/api/children/" + childId + "/style/seen").with(csrf()).session(stranger))
				.andExpect(status().isNotFound());
		consent(stranger, childId, true).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/children/" + childId + "/consents").session(stranger))
				.andExpect(status().isNotFound());

		MockHttpSession teacher = login(Role.TEACHER);
		style(teacher, childId).andExpect(status().isForbidden());

		for (String type : List.of("account", "microphone", "nonsense")) {
			mockMvc.perform(put("/api/children/" + childId + "/consents/" + type).with(csrf()).session(parent))
					.andExpect(status().isBadRequest());
			mockMvc.perform(delete("/api/children/" + childId + "/consents/" + type).with(csrf()).session(parent))
					.andExpect(status().isBadRequest());
		}
	}

}
