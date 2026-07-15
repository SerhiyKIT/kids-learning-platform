package ua.kidlearn.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
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
class GroupControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@PersistenceContext
	private EntityManager entityManager;

	private record GroupInfo(UUID id, String joinCode) {
	}

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@example.test";
	}

	private void registerTeacher(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.TEACHER, "Teacher", "uk"));
	}

	private User registerParent(String email) throws Exception {
		String body = "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"Test\"}".formatted(email, PASSWORD);
		mockMvc.perform(post("/api/auth/register").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());
		return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
	}

	private void registerVerifiedParent(String email) throws Exception {
		User user = registerParent(email);
		user.markEmailVerified();
		userRepository.save(user);
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult result = mockMvc.perform(formLogin().user(email).password(PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private static String childBody(String displayName) {
		return "{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}"
				.formatted(displayName, Year.now().getValue() - 3);
	}

	private UUID createPendingChild(MockHttpSession parentSession, String displayName) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/children").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(childBody(displayName)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID createActiveChild(MockHttpSession parentSession, String displayName) throws Exception {
		UUID childId = createPendingChild(parentSession, displayName);
		mockMvc.perform(post("/api/children/" + childId + "/consent").with(csrf()).session(parentSession))
				.andExpect(status().isOk());
		return childId;
	}

	private GroupInfo createGroup(MockHttpSession teacherSession, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"%s\"}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		return new GroupInfo(UUID.fromString(JsonPath.read(body, "$.id")), JsonPath.read(body, "$.joinCode"));
	}

	private static String joinBody(String joinCode, UUID childId) {
		return "{\"joinCode\":\"%s\",\"childId\":\"%s\"}".formatted(joinCode, childId);
	}

	@Test
	void teacherCreatesGroupAndNonTeacherIsForbidden() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class A\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.joinCode").isNotEmpty())
				.andExpect(jsonPath("$.isActive").value(true));

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);

		mockMvc.perform(post("/api/groups").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class B\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void parentJoinsActiveChildAndTeacherSeesItInMemberList() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		GroupInfo group = createGroup(teacherSession, "Class A");

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createActiveChild(parentSession, "Kid");

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(group.joinCode(), childId)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/groups/" + group.id() + "/members").session(teacherSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].childId").value(childId.toString()))
				.andExpect(jsonPath("$[0].displayName").value("Kid"));
	}

	@Test
	void joiningWithUnownedChildReturns404() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		GroupInfo group = createGroup(teacherSession, "Class A");

		String ownerEmail = uniqueEmail("owner");
		registerVerifiedParent(ownerEmail);
		MockHttpSession ownerSession = login(ownerEmail);
		UUID childId = createActiveChild(ownerSession, "Kid");

		String otherEmail = uniqueEmail("other");
		registerVerifiedParent(otherEmail);
		MockHttpSession otherSession = login(otherEmail);

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(otherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(group.joinCode(), childId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void joiningPendingConsentChildReturnsChildNotActive() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		GroupInfo group = createGroup(teacherSession, "Class A");

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createPendingChild(parentSession, "Kid");

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(group.joinCode(), childId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CHILD_NOT_ACTIVE"));
	}

	@Test
	void teacherCannotManageAnotherTeachersGroup() throws Exception {
		String teacherAEmail = uniqueEmail("teacherA");
		registerTeacher(teacherAEmail);
		MockHttpSession teacherASession = login(teacherAEmail);
		GroupInfo group = createGroup(teacherASession, "Class A");

		String teacherBEmail = uniqueEmail("teacherB");
		registerTeacher(teacherBEmail);
		MockHttpSession teacherBSession = login(teacherBEmail);

		mockMvc.perform(get("/api/groups/" + group.id() + "/members").session(teacherBSession))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/groups/" + group.id() + "/archive").with(csrf()).session(teacherBSession))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/groups/" + group.id() + "/regenerate-code").with(csrf()).session(teacherBSession))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/groups/" + group.id() + "/members/" + UUID.randomUUID()).with(csrf())
						.session(teacherBSession))
				.andExpect(status().isNotFound());
	}

	@Test
	void memberListNeverContainsParentEmail() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		GroupInfo group = createGroup(teacherSession, "Class A");

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createActiveChild(parentSession, "Kid");

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(group.joinCode(), childId)))
				.andExpect(status().isOk());

		MvcResult result = mockMvc.perform(get("/api/groups/" + group.id() + "/members").session(teacherSession))
				.andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		assertThat(body).doesNotContain(parentEmail);
		assertThat(body).doesNotContainIgnoringCase("email");
		assertThat(body).contains("displayName");
	}

	@Test
	void joinIsIdempotent() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		GroupInfo group = createGroup(teacherSession, "Class A");

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createActiveChild(parentSession, "Kid");

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(group.joinCode(), childId)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(group.joinCode(), childId)))
				.andExpect(status().isOk());

		Number count = (Number) entityManager
				.createQuery("select count(m) from GroupMember m where m.id.childId = :childId and m.id.groupId = :groupId")
				.setParameter("childId", childId)
				.setParameter("groupId", group.id())
				.getSingleResult();
		assertThat(count.longValue()).isEqualTo(1);
	}

	@Test
	void parentAndTeacherCanBothRemoveMembership() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		GroupInfo groupA = createGroup(teacherSession, "Class A");
		GroupInfo groupB = createGroup(teacherSession, "Class B");

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createActiveChild(parentSession, "Kid");

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(groupA.joinCode(), childId)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody(groupB.joinCode(), childId)))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/children/" + childId + "/groups/" + groupA.id()).with(csrf())
						.session(parentSession))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/groups/" + groupB.id() + "/members/" + childId).with(csrf())
						.session(teacherSession))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/children/" + childId + "/groups").session(parentSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void nonParentCannotHitJoinEndpoint() throws Exception {
		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);

		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody("ANYCODE1", UUID.randomUUID())))
				.andExpect(status().isForbidden());
	}

}
