package ua.kidlearn.devseed;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class DevSeedControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private User registerParent(String email) throws Exception {
		String body = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD);
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
		return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession registerAndLoginVerifiedParent(String email) throws Exception {
		User user = registerParent(email);
		user.markEmailVerified();
		userRepository.save(user);
		return login(email);
	}

	@Test
	void seedingAsParentMakesExactlyOneLessonAvailableAndIsIdempotent() throws Exception {
		MockHttpSession session = registerAndLoginVerifiedParent(uniqueEmail("seed-parent"));

		MvcResult firstSeed = mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lessonVersionId").exists())
				.andExpect(jsonPath("$.groupId").exists())
				.andExpect(jsonPath("$.joinCode").exists())
				.andExpect(jsonPath("$.childId").exists())
				.andReturn();
		String childId = JsonPath.read(firstSeed.getResponse().getContentAsString(), "$.childId");

		mockMvc.perform(get("/api/children/" + childId + "/available-lessons").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		// Calling again must reuse the same child and lesson assignment, not create duplicates.
		mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.childId").value(childId));

		mockMvc.perform(get("/api/children/" + childId + "/available-lessons").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void seedingAsNonParentSkipsChildAssignmentButStillSeedsSharedDemoContent() throws Exception {
		MockHttpSession adminSession = login(bootstrapAdmin());

		MvcResult result = mockMvc.perform(post("/api/dev/seed-demo").with(csrf()).session(adminSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lessonVersionId").exists())
				.andExpect(jsonPath("$.groupId").exists())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		Assertions.assertNull(JsonPath.read(body, "$.childId"));
	}

	private String bootstrapAdmin() {
		String email = uniqueEmail("seed-admin-caller");
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
		return email;
	}

}
