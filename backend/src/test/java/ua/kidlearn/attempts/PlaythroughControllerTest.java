package ua.kidlearn.attempts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class PlaythroughControllerTest {

	private static final String PASSWORD = "supersecret1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private record Fixture(UUID versionId, MockHttpSession teacherSession, UUID groupId, String joinCode,
			MockHttpSession parentSession, UUID childId) {
	}

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
	private UUID createPublishedVersion(String title) throws Exception {
		String adminEmail = uniqueEmail("admin");
		registerAdmin(adminEmail);
		MockHttpSession adminSession = login(adminEmail);

		MvcResult lessonResult = mockMvc.perform(post("/api/admin/lessons").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"moduleCode\":\"safety\",\"title\":\"%s\"}".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		String lessonId = JsonPath.read(lessonResult.getResponse().getContentAsString(), "$.id");

		MvcResult versionResult = mockMvc.perform(post("/api/admin/lessons/" + lessonId + "/versions").with(csrf())
						.session(adminSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scenario\":{\"learning_goal\":\"test\"},\"generatedBy\":\"human\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String versionId = JsonPath.read(versionResult.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/admin/lesson-versions/" + versionId + "/publish").with(csrf())
						.session(adminSession))
				.andExpect(status().isOk());
		return UUID.fromString(versionId);
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

	private static String childBody(String displayName) {
		return "{\"displayName\":\"%s\",\"birthYear\":%d,\"relation\":\"mother\"}"
				.formatted(displayName, Year.now().getValue() - 3);
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

	/** Published lesson, assigned to a fresh group, with a verified parent's active child in that group. */
	private Fixture buildAssignedFixture() throws Exception {
		UUID versionId = createPublishedVersion("Crossing the street");

		String teacherEmail = uniqueEmail("teacher");
		registerTeacher(teacherEmail);
		MockHttpSession teacherSession = login(teacherEmail);
		UUID[] groupIdHolder = new UUID[1];
		String joinCode = createGroupWithJoinCode(teacherSession, groupIdHolder);
		UUID groupId = groupIdHolder[0];

		mockMvc.perform(post("/api/assignments").with(csrf())
						.session(teacherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\",\"groupId\":\"%s\"}".formatted(versionId, groupId)))
				.andExpect(status().isCreated());

		String parentEmail = uniqueEmail("parent");
		registerVerifiedParent(parentEmail);
		MockHttpSession parentSession = login(parentEmail);
		UUID childId = createActiveChildInGroup(parentSession, joinCode);

		return new Fixture(versionId, teacherSession, groupId, joinCode, parentSession, childId);
	}

	private UUID startAttempt(MockHttpSession parentSession, UUID childId, UUID versionId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/children/" + childId + "/attempts").with(csrf())
						.session(parentSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(versionId)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.attemptId"));
	}

	@Test
	void startsAttemptOnAssignedLessonButNotOnNonAssignedLesson() throws Exception {
		Fixture fixture = buildAssignedFixture();

		mockMvc.perform(post("/api/children/" + fixture.childId() + "/attempts").with(csrf())
						.session(fixture.parentSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(fixture.versionId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").isNotEmpty());

		UUID unassignedVersionId = createPublishedVersion("Unassigned Lesson");
		mockMvc.perform(post("/api/children/" + fixture.childId() + "/attempts").with(csrf())
						.session(fixture.parentSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(unassignedVersionId)))
				.andExpect(status().isNotFound());
	}

	@Test
	void parentCannotStartAttemptForUnownedChild() throws Exception {
		Fixture fixture = buildAssignedFixture();

		String otherParentEmail = uniqueEmail("other");
		registerVerifiedParent(otherParentEmail);
		MockHttpSession otherSession = login(otherParentEmail);

		mockMvc.perform(post("/api/children/" + fixture.childId() + "/attempts").with(csrf())
						.session(otherSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(fixture.versionId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void recordingAnswersAndCompletingShowsUpInHistory() throws Exception {
		Fixture fixture = buildAssignedFixture();
		UUID attemptId = startAttempt(fixture.parentSession(), fixture.childId(), fixture.versionId());

		mockMvc.perform(post("/api/attempts/" + attemptId + "/answers").with(csrf())
						.session(fixture.parentSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sceneKey\":\"scene1\",\"tryNo\":1,\"chosenOption\":\"a\",\"isCorrect\":true,\"hintsUsed\":0}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/attempts/" + attemptId + "/answers").with(csrf())
						.session(fixture.parentSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sceneKey\":\"scene2\",\"tryNo\":1,\"chosenOption\":\"b\",\"isCorrect\":false,\"hintsUsed\":1}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/attempts/" + attemptId + "/complete").with(csrf())
						.session(fixture.parentSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\":\"completed\",\"score\":0.5}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/history").session(fixture.parentSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].attemptId").value(attemptId.toString()))
				.andExpect(jsonPath("$[0].title").value("Crossing the street"))
				.andExpect(jsonPath("$[0].result").value("completed"))
				.andExpect(jsonPath("$[0].score").value(0.5))
				.andExpect(jsonPath("$[0].answers.length()").value(2))
				.andExpect(jsonPath("$[0].answers[0].sceneKey").value("scene1"))
				.andExpect(jsonPath("$[0].answers[0].isCorrect").value(true))
				.andExpect(jsonPath("$[0].answers[1].sceneKey").value("scene2"))
				.andExpect(jsonPath("$[0].answers[1].isCorrect").value(false));
	}

	@Test
	void parentBCannotSeeParentAsChildHistory() throws Exception {
		Fixture fixture = buildAssignedFixture();
		startAttempt(fixture.parentSession(), fixture.childId(), fixture.versionId());

		String otherParentEmail = uniqueEmail("parentB");
		registerVerifiedParent(otherParentEmail);
		MockHttpSession otherSession = login(otherParentEmail);

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/history").session(otherSession))
				.andExpect(status().isNotFound());
	}

	@Test
	void teacherSeesOwnGroupResultsButNotAnotherTeachersGroup() throws Exception {
		Fixture fixture = buildAssignedFixture();
		UUID attemptId = startAttempt(fixture.parentSession(), fixture.childId(), fixture.versionId());
		mockMvc.perform(post("/api/attempts/" + attemptId + "/complete").with(csrf())
						.session(fixture.parentSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"result\":\"completed\",\"score\":1}"))
				.andExpect(status().isOk());

		MvcResult resultsResult = mockMvc.perform(get("/api/groups/" + fixture.groupId() + "/results")
						.session(fixture.teacherSession()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].displayName").value("Kid"))
				.andExpect(jsonPath("$[0].attempts[0].title").value("Crossing the street"))
				.andExpect(jsonPath("$[0].attempts[0].result").value("completed"))
				.andReturn();

		// No parent PII anywhere in the results body.
		assertThat(resultsResult.getResponse().getContentAsString()).doesNotContainIgnoringCase("email");

		String otherTeacherEmail = uniqueEmail("teacherB");
		registerTeacher(otherTeacherEmail);
		MockHttpSession otherTeacherSession = login(otherTeacherEmail);
		mockMvc.perform(get("/api/groups/" + fixture.groupId() + "/results").session(otherTeacherSession))
				.andExpect(status().isNotFound());
	}

	@Test
	void nonTeacherAndNonParentAreForbidden() throws Exception {
		Fixture fixture = buildAssignedFixture();

		mockMvc.perform(get("/api/groups/" + fixture.groupId() + "/results").session(fixture.parentSession()))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/children/" + fixture.childId() + "/available-lessons")
						.session(fixture.teacherSession()))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/children/" + fixture.childId() + "/attempts").with(csrf())
						.session(fixture.teacherSession())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lessonVersionId\":\"%s\"}".formatted(fixture.versionId())))
				.andExpect(status().isForbidden());
	}

}
