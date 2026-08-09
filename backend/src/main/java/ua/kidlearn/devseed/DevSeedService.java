package ua.kidlearn.devseed;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.children.Child;
import ua.kidlearn.children.ChildService;
import ua.kidlearn.children.CreateChildRequest;
import ua.kidlearn.groups.Group;
import ua.kidlearn.groups.GroupRepository;
import ua.kidlearn.groups.GroupService;
import ua.kidlearn.lessons.Lesson;
import ua.kidlearn.lessons.LessonAssignment;
import ua.kidlearn.lessons.LessonAssignmentRepository;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.ModuleRepository;
import ua.kidlearn.scenario.ScenarioFixtures;
import ua.kidlearn.scenario.ScenarioProblem;
import ua.kidlearn.scenario.ScenarioValidator;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Builds (or reuses) the fixed set of demo rows PART A/B need. Every "ensure" step looks up by a
 * stable key first, so repeated calls never create duplicates.
 */
@Service
@Profile("dev")
public class DevSeedService {

	private static final String ADMIN_EMAIL = "dev-seed-admin@kidlearn.local";
	private static final String TEACHER_EMAIL = "dev-seed-teacher@kidlearn.local";
	// Never used to log in with (these accounts aren't linked from any UI); only needs to satisfy
	// the password column's NOT NULL constraint.
	private static final String SEED_PASSWORD_PLACEHOLDER = "dev-seed-placeholder-password";
	private static final String DEMO_MODULE_CODE = "safety";
	private static final String DEMO_LESSON_TITLE = "Демо: перехід дороги";
	private static final String DEMO_GROUP_NAME = "Демо-група";
	private static final String DEMO_CHILD_NAME = "Демо-дитина";
	private static final String DEMO_CHILD_RELATION = "guardian";
	private static final int DEMO_CHILD_AGE_YEARS = 5;
	private static final String DEMO_AI_MODEL_MARKER = "dev-seed-fixture";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final LessonRepository lessonRepository;
	private final ModuleRepository moduleRepository;
	private final LessonService lessonService;
	private final ScenarioValidator scenarioValidator;
	private final ObjectMapper objectMapper;
	private final GroupRepository groupRepository;
	private final GroupService groupService;
	private final ChildService childService;
	private final LessonAssignmentRepository lessonAssignmentRepository;

	public DevSeedService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			LessonRepository lessonRepository, ModuleRepository moduleRepository, LessonService lessonService,
			ScenarioValidator scenarioValidator, ObjectMapper objectMapper, GroupRepository groupRepository,
			GroupService groupService, ChildService childService,
			LessonAssignmentRepository lessonAssignmentRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.lessonRepository = lessonRepository;
		this.moduleRepository = moduleRepository;
		this.lessonService = lessonService;
		this.scenarioValidator = scenarioValidator;
		this.objectMapper = objectMapper;
		this.groupRepository = groupRepository;
		this.groupService = groupService;
		this.childService = childService;
		this.lessonAssignmentRepository = lessonAssignmentRepository;
	}

	@Transactional
	public DevSeedResponse seed(UUID callerId, Role callerRole) {
		User admin = ensureUser(ADMIN_EMAIL, Role.ADMIN, "Демо Адмін");
		User teacher = ensureUser(TEACHER_EMAIL, Role.TEACHER, "Демо Вчитель");

		LessonVersion version = ensurePublishedDemoLessonVersion(admin.getId());
		Group group = ensureDemoGroup(teacher.getId());

		UUID childId = null;
		if (callerRole == Role.PARENT) {
			childId = ensureAssignedDemoChild(callerId, teacher.getId(), version.getId());
		}

		return new DevSeedResponse(version.getId(), group.getId(), group.getJoinCode(), childId);
	}

	private User ensureUser(String email, Role role, String displayName) {
		return userRepository.findByEmailAndDeletedAtIsNull(email).orElseGet(() -> {
			User user = new User(email, passwordEncoder.encode(SEED_PASSWORD_PLACEHOLDER), role, displayName, "uk");
			user.markEmailVerified();
			return userRepository.save(user);
		});
	}

	private LessonVersion ensurePublishedDemoLessonVersion(UUID adminId) {
		UUID moduleId = moduleRepository.findByCode(DEMO_MODULE_CODE)
				.orElseThrow(() -> new IllegalStateException("Unknown moduleCode: " + DEMO_MODULE_CODE))
				.getId();
		Lesson lesson = lessonRepository.findFirstByTitleAndModuleId(DEMO_LESSON_TITLE, moduleId)
				.orElseGet(() -> lessonService.createLesson(DEMO_MODULE_CODE, DEMO_LESSON_TITLE));
		if (lesson.getCurrentVersionId() != null) {
			return lessonService.getVersion(lesson.getCurrentVersionId());
		}
		JsonNode scenario = objectMapper.readTree(ScenarioFixtures.VALID_MINIMAL_SCENARIO);
		List<ScenarioProblem> problems = scenarioValidator.validate(scenario);
		if (!problems.isEmpty()) {
			// ScenarioFixtures.VALID_MINIMAL_SCENARIO is meant to stay schema-valid; if it ever
			// drifts from docs/lesson-scenario.schema.json this seed step should fail loudly
			// instead of silently leaving an unapprovable rejected_auto version behind.
			throw new IllegalStateException("ScenarioFixtures.VALID_MINIMAL_SCENARIO failed validation: " + problems);
		}
		// createGeneratedVersion (not the manual createVersion) is what actually lands a version in
		// auto_validated, the only status LessonService.approve accepts.
		LessonVersion version = lessonService.createGeneratedVersion(lesson.getId(), scenario, DEMO_AI_MODEL_MARKER,
				LessonVersion.STATUS_AUTO_VALIDATED);
		lessonService.approve(version.getId(), adminId);
		return lessonService.publish(version.getId());
	}

	private Group ensureDemoGroup(UUID teacherId) {
		return groupRepository.findByTeacherId(teacherId).stream().findFirst()
				.orElseGet(() -> groupService.create(teacherId, DEMO_GROUP_NAME));
	}

	/** Reuses the parent's first child (if any) rather than always minting a new one, so calling
	 * this endpoint repeatedly doesn't pile up demo children. */
	private UUID ensureAssignedDemoChild(UUID parentId, UUID teacherId, UUID lessonVersionId) {
		Child child = childService.listForParent(parentId).stream().findFirst()
				.orElseGet(() -> childService.create(parentId, new CreateChildRequest(DEMO_CHILD_NAME,
						Year.now().getValue() - DEMO_CHILD_AGE_YEARS, DEMO_CHILD_RELATION, null)));
		childService.grantAccountConsent(parentId, child.getId());

		boolean alreadyAssigned = lessonAssignmentRepository.findByChildId(child.getId()).stream()
				.anyMatch(a -> a.getLessonVersionId().equals(lessonVersionId));
		if (!alreadyAssigned) {
			lessonAssignmentRepository.save(new LessonAssignment(lessonVersionId, null, child.getId(), teacherId, null, null));
		}
		return child.getId();
	}

}
