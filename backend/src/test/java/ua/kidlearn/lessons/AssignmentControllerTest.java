package ua.kidlearn.lessons;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class AssignmentControllerTest {

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

	private void registerAdmin(String email) {
		userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), Role.ADMIN, "Admin", "uk"));
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

	/** Registers a fresh admin and returns a published lesson version's id. */
	private UUID createPublishedVersion() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);

		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Crossing the street\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");

		return createVersion(adminSession, lessonId, true);
	}

	private UUID createDraftVersionForExistingLesson(MockHttpSession adminSession, String lessonId) throws Exception {
		return createVersion(adminSession, lessonId, false);
	}

	private UUID createVersion(MockHttpSession adminSession, String lessonId, boolean publish) throws Exception {
		MvcResult versionResult = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/versions").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scenario\":{\"learning_goal\":\"test\"},\"generatedBy\":\"human\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String versionId = JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id");
		if (publish) {
			mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
							.session(adminSession))
					.andExpect(status().isOk());
		}
		return UUID.fromString(versionId);
	}

	private static String childBody(String displayName) {
		return "{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}"
				.formatted(displayName, Year.now().getValue() - 3);
	}

	private UUID createGroup(MockHttpSession teacherSession) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class A\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(body, "$.id"));
	}

	private String createGroupWithJoinCode(MockHttpSession teacherSession, UUID[] groupIdOut) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/groups").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Class A\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		groupIdOut[0] = UUID.fromString(JsonPath.read(body, "$.id"));
		return JsonPath.read(body, "$.joinCode");
	}

	private UUID createActiveChildInGroup(MockHttpSession parentSession, String joinCode) throws Exception {
		MvcResult createResult = mockMvc.perform(post("/api/children").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(childBody("Kid")))
				.andExpect(status().isCreated())
				.andReturn();
		String childId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/children/" + childId + "/consent").with(csrf()).session(parentSession))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/groups/join").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"joinCode\":\"%s\",\"childId\":\"%s\"}".formatted(joinCode, childId)))
				.andExpect(status().isOk());
		return UUID.fromString(childId);
	}

	private static String assignGroupBody(UUID lessonVersionId, UUID groupId) {
		return "{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\"}".formatted(lessonVersionId, groupId);
	}

	private static String assignChildBody(UUID lessonVersionId, UUID childId) {
		return "{\"lessonVersionId\":\"%s\",\"childId\":\"%s\"}".formatted(lessonVersionId, childId);
	}

	@Test
	void teacherAssignsPublishedVersionToOwnGroupButNotToAnotherTeachersGroup() throws Exception {
		UUID versionId = createPublishedVersion();

		String teacherAEmail = uniqueEmail("teacherA");
		registerTeacher(teacherAEmail);
		MockHttpSession teacherASession = login(teacherAEmail);
		UUID groupId = createGroup(teacherASession);

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherASession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignGroupBody(versionId, groupId)))
				.andExpect(status().isCreated());

		String teacherBEmail = uniqueEmail("teacherB");
		registerTeacher(teacherBEmail);
		MockHttpSession teacherBSession = login(teacherBEmail);

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherBSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignGroupBody(versionId, groupId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void assigningNonPublishedVersionReturnsNotPublished() throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);
		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"Draft Lesson\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");
		UUID draftVersionId = createDraftVersionForExistingLesson(adminSession, lessonId);

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		UUID groupId = createGroup(teacherSession);

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignGroupBody(draftVersionId, groupId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOT_PUBLISHED"));
	}

	@Test
	void teacherAssignsToChildInGroupButNotToChildOutsideGroup() throws Exception {
		UUID versionId = createPublishedVersion();

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		UUID[] groupIdHolder = new UUID[1];
		String joinCode = createGroupWithJoinCode(teacherSession, groupIdHolder);

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childInGroup = createActiveChildInGroup(parentSession, joinCode);

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignChildBody(versionId, childInGroup)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignChildBody(versionId, UUID.randomUUID())))
				.andExpect(status().isNotFound());
	}

	@Test
	void assignmentRequiresExactlyOneOfGroupIdOrChildId() throws Exception {
		UUID versionId = createPublishedVersion();

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		UUID groupId = createGroup(teacherSession);

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(versionId)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\",\"childId\":\"%s\"}"
								.formatted(versionId, groupId, UUID.randomUUID())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonTeacherCannotHitAssignmentsEndpoint() throws Exception {
		UUID versionId = createPublishedVersion();

		String adminEmail = uniqueEmail("admin2");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignGroupBody(versionId, UUID.randomUUID())))
				.andExpect(status().isForbidden());
	}

}
