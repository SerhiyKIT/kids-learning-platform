package ua.kidlearn.children;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * @Transactional is safe here: unlike the mail-flow tests, nothing in this
 * class depends on an AFTER_COMMIT listener actually firing, so the whole
 * class rolls back cleanly with no manual cleanup needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ChildControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@PersistenceContext
	private EntityManager entityManager;

	private User registerParent(String email) throws Exception {
		String body = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD);
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
		return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
	}

	private User registerVerifiedParent(String email) throws Exception {
		User user = registerParent(email);
		user.markEmailVerified();
		return userRepository.save(user);
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static String createChildBody(String displayName) {
		return "{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}"
				.formatted(displayName, Year.now().getValue() - 3);
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	@Test
	void createRequiresVerifiedEmail() throws Exception {
		String email = uniqueEmail("unverified");
		registerParent(email);
		MockHttpSession session = login(email);

		mockMvc.perform(post("/api/children").with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createChildBody("Kid")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	void createSucceedsForVerifiedParent() throws Exception {
		String email = uniqueEmail("verified");
		registerVerifiedParent(email);
		MockHttpSession session = login(email);

		mockMvc.perform(post("/api/children").with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createChildBody("Kid")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("pending_consent"));
	}

	@Test
	void teacherCannotCreateChild() throws Exception {
		User teacher = userRepository.save(new User(uniqueEmail("teacher"), passwordEncoder.encode(PASSWORD),
				Role.TEACHER, "Teacher", "uk"));
		AppUserPrincipal principal = new AppUserPrincipal(teacher);
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
				principal.getAuthorities());

		mockMvc.perform(post("/api/children").with(csrf())
						.with(authentication(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(createChildBody("Kid")))
				.andExpect(status().isForbidden());
	}

	@Test
	void consentActivatesChildAndIsIdempotent() throws Exception {
		String email = uniqueEmail("consent");
		registerVerifiedParent(email);
		MockHttpSession session = login(email);

		MvcResult createResult = mockMvc.perform(post("/api/children").with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createChildBody("Kid")))
				.andExpect(status().isCreated())
				.andReturn();
		String childId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/children/" + childId + "/consent").with(csrf()).session(session))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/children/" + childId + "/consent").with(csrf()).session(session))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/children/" + childId).session(session))
				.andExpect(jsonPath("$.status").value("active"));

		Number consentCount = (Number) entityManager
				.createQuery("select count(c) from Consent c where c.childId = :childId and c.type = 'account'")
				.setParameter("childId", UUID.fromString(childId))
				.getSingleResult();
		assertThat(consentCount.longValue()).isEqualTo(1);
	}

	@Test
	void parentCannotSeeAnotherParentsChild() throws Exception {
		String emailA = uniqueEmail("parentA");
		registerVerifiedParent(emailA);
		MockHttpSession sessionA = login(emailA);

		MvcResult createResult = mockMvc.perform(post("/api/children").with(csrf())
						.session(sessionA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createChildBody("Kid A")))
				.andExpect(status().isCreated())
				.andReturn();
		String childId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

		String emailB = uniqueEmail("parentB");
		registerVerifiedParent(emailB);
		MockHttpSession sessionB = login(emailB);

		mockMvc.perform(get("/api/children").session(sessionB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/children/" + childId).session(sessionB))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletingChildCascadesToAttemptsAndAnswers() throws Exception {
		String email = uniqueEmail("delete");
		registerVerifiedParent(email);
		MockHttpSession session = login(email);

		MvcResult createResult = mockMvc.perform(post("/api/children").with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createChildBody("Kid")))
				.andExpect(status().isCreated())
				.andReturn();
		UUID childId = UUID.fromString(JsonPath.read(createResult.getResponse().getContentAsString(), "$.id"));

		UUID lessonId = UUID.randomUUID();
		UUID lessonVersionId = UUID.randomUUID();
		UUID attemptId = UUID.randomUUID();
		UUID answerId = UUID.randomUUID();

		entityManager.createNativeQuery("""
				INSERT INTO lessons (id, module_id, title)
				SELECT :lessonId, id, 'Cascade Test Lesson' FROM modules WHERE code = 'safety'
				""")
				.setParameter("lessonId", lessonId)
				.executeUpdate();
		entityManager.createNativeQuery("""
				INSERT INTO lesson_versions (id, lesson_id, version_no, scenario, generated_by, status)
				VALUES (:id, :lessonId, 1, '{}'::jsonb, 'human', 'draft')
				""")
				.setParameter("id", lessonVersionId)
				.setParameter("lessonId", lessonId)
				.executeUpdate();
		entityManager.createNativeQuery("""
				INSERT INTO lesson_attempts (id, child_id, lesson_version_id, started_at)
				VALUES (:id, :childId, :lessonVersionId, now())
				""")
				.setParameter("id", attemptId)
				.setParameter("childId", childId)
				.setParameter("lessonVersionId", lessonVersionId)
				.executeUpdate();
		entityManager.createNativeQuery("""
				INSERT INTO scene_answers (id, attempt_id, scene_key, try_no, chosen_option, is_correct, hints_used, answered_at)
				VALUES (:id, :attemptId, 'scene1', 1, 'a', true, 0, now())
				""")
				.setParameter("id", answerId)
				.setParameter("attemptId", attemptId)
				.executeUpdate();
		entityManager.flush();

		mockMvc.perform(delete("/api/children/" + childId).with(csrf()).session(session))
				.andExpect(status().isNoContent());

		Number attemptCount = (Number) entityManager
				.createNativeQuery("SELECT count(*) FROM lesson_attempts WHERE id = :id")
				.setParameter("id", attemptId)
				.getSingleResult();
		Number answerCount = (Number) entityManager
				.createNativeQuery("SELECT count(*) FROM scene_answers WHERE id = :id")
				.setParameter("id", answerId)
				.getSingleResult();
		assertThat(attemptCount.longValue()).isEqualTo(0);
		assertThat(answerCount.longValue()).isEqualTo(0);
	}

}
