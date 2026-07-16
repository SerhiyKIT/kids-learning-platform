package ua.kidlearn.attempts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.children.Child;
import ua.kidlearn.children.ChildRepository;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.groups.Group;
import ua.kidlearn.groups.GroupMemberRepository;
import ua.kidlearn.groups.GroupRepository;
import ua.kidlearn.lessons.Lesson;
import ua.kidlearn.lessons.LessonAssignment;
import ua.kidlearn.lessons.LessonAssignmentRepository;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.lessons.Module;
import ua.kidlearn.lessons.ModuleRepository;

@Service
public class AttemptService {

	private static final Set<String> VALID_RESULTS = Set.of(LessonAttempt.RESULT_COMPLETED,
			LessonAttempt.RESULT_ABANDONED);

	private final LessonAttemptRepository lessonAttemptRepository;
	private final SceneAnswerRepository sceneAnswerRepository;
	private final LessonAssignmentRepository lessonAssignmentRepository;
	private final LessonVersionRepository lessonVersionRepository;
	private final LessonRepository lessonRepository;
	private final ModuleRepository moduleRepository;
	private final ParentChildRepository parentChildRepository;
	private final ChildRepository childRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;

	public AttemptService(LessonAttemptRepository lessonAttemptRepository, SceneAnswerRepository sceneAnswerRepository,
			LessonAssignmentRepository lessonAssignmentRepository, LessonVersionRepository lessonVersionRepository,
			LessonRepository lessonRepository, ModuleRepository moduleRepository,
			ParentChildRepository parentChildRepository, ChildRepository childRepository,
			GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
		this.lessonAttemptRepository = lessonAttemptRepository;
		this.sceneAnswerRepository = sceneAnswerRepository;
		this.lessonAssignmentRepository = lessonAssignmentRepository;
		this.lessonVersionRepository = lessonVersionRepository;
		this.lessonRepository = lessonRepository;
		this.moduleRepository = moduleRepository;
		this.parentChildRepository = parentChildRepository;
		this.childRepository = childRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional(readOnly = true)
	public List<AvailableLessonEntry> availableLessons(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		List<AvailableLessonEntry> entries = new ArrayList<>();
		for (LessonAssignment assignment : resolveAssignmentsForChild(childId)) {
			LessonVersion version = lessonVersionRepository.findById(assignment.getLessonVersionId()).orElseThrow();
			Lesson lesson = lessonRepository.findById(version.getLessonId()).orElseThrow();
			Module module = moduleRepository.findById(lesson.getModuleId()).orElseThrow();
			entries.add(new AvailableLessonEntry(version.getId(), lesson.getId(), lesson.getTitle(),
					module.getCode(), assignment.getId(), assignment.getDueAt()));
		}
		return entries;
	}

	@Transactional
	public LessonAttempt startAttempt(UUID parentId, UUID childId, UUID lessonVersionId) {
		requireChildOwnership(parentId, childId);
		LessonAssignment assignment = resolveAssignmentsForChild(childId).stream()
				.filter(a -> a.getLessonVersionId().equals(lessonVersionId))
				.findFirst()
				.orElseThrow(this::notFound);
		return lessonAttemptRepository.save(
				new LessonAttempt(childId, assignment.getId(), lessonVersionId, Instant.now()));
	}

	@Transactional
	public SceneAnswer recordAnswer(UUID parentId, UUID attemptId, RecordAnswerRequest request) {
		LessonAttempt attempt = requireOwnedAttempt(parentId, attemptId);
		return sceneAnswerRepository.save(new SceneAnswer(attempt.getId(), request.sceneKey(), request.tryNo(),
				request.chosenOption(), request.isCorrect(), request.hintsUsed(), Instant.now()));
	}

	@Transactional
	public LessonAttempt complete(UUID parentId, UUID attemptId, String result, BigDecimal score) {
		LessonAttempt attempt = requireOwnedAttempt(parentId, attemptId);
		if (!VALID_RESULTS.contains(result)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid result: " + result);
		}
		attempt.complete(result, score);
		return attempt;
	}

	@Transactional(readOnly = true)
	public List<HistoryEntry> history(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		List<HistoryEntry> entries = new ArrayList<>();
		for (LessonAttempt attempt : lessonAttemptRepository.findByChildIdOrderByStartedAtDesc(childId)) {
			LessonVersion version = lessonVersionRepository.findById(attempt.getLessonVersionId()).orElseThrow();
			Lesson lesson = lessonRepository.findById(version.getLessonId()).orElseThrow();
			List<AnswerEntry> answers = sceneAnswerRepository.findByAttemptId(attempt.getId()).stream()
					.map(AnswerEntry::from)
					.toList();
			entries.add(new HistoryEntry(attempt.getId(), lesson.getTitle(), attempt.getStartedAt(),
					attempt.getCompletedAt(), attempt.getResult(), attempt.getScore(), answers));
		}
		return entries;
	}

	@Transactional(readOnly = true)
	public List<TeacherResultChild> groupResults(UUID teacherId, UUID groupId) {
		Group group = groupRepository.findById(groupId).orElseThrow(this::notFound);
		if (!group.getTeacherId().equals(teacherId)) {
			throw notFound();
		}
		List<TeacherResultChild> results = new ArrayList<>();
		for (var member : groupMemberRepository.findById_GroupId(groupId)) {
			UUID childId = member.getId().getChildId();
			Child child = childRepository.findById(childId).orElseThrow();
			List<TeacherResultAttempt> attempts = lessonAttemptRepository.findByChildIdOrderByStartedAtDesc(childId)
					.stream()
					.map(this::toTeacherResultAttempt)
					.toList();
			results.add(new TeacherResultChild(childId, child.getDisplayName(), attempts));
		}
		return results;
	}

	private TeacherResultAttempt toTeacherResultAttempt(LessonAttempt attempt) {
		LessonVersion version = lessonVersionRepository.findById(attempt.getLessonVersionId()).orElseThrow();
		Lesson lesson = lessonRepository.findById(version.getLessonId()).orElseThrow();
		return new TeacherResultAttempt(lesson.getTitle(), attempt.getCompletedAt(), attempt.getResult(),
				attempt.getScore());
	}

	private List<LessonAssignment> resolveAssignmentsForChild(UUID childId) {
		List<LessonAssignment> assignments = new ArrayList<>(lessonAssignmentRepository.findByChildId(childId));
		List<UUID> groupIds = groupMemberRepository.findById_ChildId(childId).stream()
				.map(m -> m.getId().getGroupId())
				.toList();
		if (!groupIds.isEmpty()) {
			assignments.addAll(lessonAssignmentRepository.findByGroupIdIn(groupIds));
		}
		return assignments;
	}

	private LessonAttempt requireOwnedAttempt(UUID parentId, UUID attemptId) {
		LessonAttempt attempt = lessonAttemptRepository.findById(attemptId).orElseThrow(this::notFound);
		requireChildOwnership(parentId, attempt.getChildId());
		return attempt;
	}

	private void requireChildOwnership(UUID parentId, UUID childId) {
		if (!parentChildRepository.existsById_ParentIdAndId_ChildId(parentId, childId)) {
			throw notFound();
		}
	}

	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

}
