package ua.kidlearn.attempts;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.aipipeline.ScenarioAudioEnricher;
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
import ua.kidlearn.kidplay.PlayStateResponse;
import ua.kidlearn.kidplay.PlayStateService;
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
	private final ObjectMapper objectMapper;
	private final ScenarioAudioEnricher scenarioAudioEnricher;
	private final PlayStateService playStateService;
	private final Clock clock;

	public AttemptService(LessonAttemptRepository lessonAttemptRepository, SceneAnswerRepository sceneAnswerRepository,
			LessonAssignmentRepository lessonAssignmentRepository, LessonVersionRepository lessonVersionRepository,
			LessonRepository lessonRepository, ModuleRepository moduleRepository,
			ParentChildRepository parentChildRepository, ChildRepository childRepository,
			GroupRepository groupRepository, GroupMemberRepository groupMemberRepository, ObjectMapper objectMapper,
			ScenarioAudioEnricher scenarioAudioEnricher, PlayStateService playStateService, Clock clock) {
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
		this.objectMapper = objectMapper;
		this.scenarioAudioEnricher = scenarioAudioEnricher;
		this.playStateService = playStateService;
		this.clock = clock;
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

	/**
	 * The playable scenario for an assigned, published lesson version: the stored jsonb with an
	 * "audioUrl" added to every voice line that has synthesized audio (see ScenarioAudioEnricher).
	 * Ownership and assignment checks come first and stay 404-not-403.
	 */
	@Transactional(readOnly = true)
	public JsonNode scenario(UUID parentId, UUID childId, UUID lessonVersionId) {
		requireChildOwnership(parentId, childId);
		boolean assigned = resolveAssignmentsForChild(childId).stream()
				.anyMatch(a -> a.getLessonVersionId().equals(lessonVersionId));
		if (!assigned) {
			throw notFound();
		}
		LessonVersion version = lessonVersionRepository.findById(lessonVersionId).orElseThrow(this::notFound);
		if (!version.isPublished()) {
			throw notFound();
		}
		return scenarioAudioEnricher.withAudioUrls(objectMapper.readTree(version.getScenario()));
	}

	@Transactional
	public StartAttemptResponse startAttempt(UUID parentId, UUID childId, UUID lessonVersionId) {
		requireChildOwnership(parentId, childId);
		LessonAssignment assignment = resolveAssignmentsForChild(childId).stream()
				.filter(a -> a.getLessonVersionId().equals(lessonVersionId))
				.findFirst()
				.orElseThrow(this::notFound);
		// After the ownership/assignment checks, so a stranger still gets 404, not CHILD_RESTING.
		playStateService.onLessonStart(childId);
		LessonAttempt attempt = lessonAttemptRepository.save(
				new LessonAttempt(childId, assignment.getId(), lessonVersionId, clock.instant()));
		UUID lessonId = lessonVersionRepository.findById(lessonVersionId).orElseThrow(this::notFound).getLessonId();
		return new StartAttemptResponse(attempt.getId(), lessonId);
	}

	@Transactional
	public SceneAnswer recordAnswer(UUID parentId, UUID attemptId, RecordAnswerRequest request) {
		LessonAttempt attempt = requireOwnedAttempt(parentId, attemptId);
		return sceneAnswerRepository.save(new SceneAnswer(attempt.getId(), request.sceneKey(), request.tryNo(),
				request.chosenOption(), request.isCorrect(), request.hintsUsed(), clock.instant()));
	}

	/**
	 * Ends the attempt and, with it, applies the play-session limit — the only point where it can
	 * send the child to rest, so a lesson is never cut short. Returns the resulting play state.
	 */
	@Transactional
	public PlayStateResponse complete(UUID parentId, UUID attemptId, String result, BigDecimal score) {
		LessonAttempt attempt = requireOwnedAttempt(parentId, attemptId);
		if (!VALID_RESULTS.contains(result)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid result: " + result);
		}
		attempt.complete(result, score, clock.instant());
		return playStateService.onLessonEnd(attempt.getChildId());
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

	/**
	 * One sticker per lesson the child has completed at least once, plus "topic stickers". A topic
	 * counts as completed when every lesson of that topic the child can play — published and
	 * assigned to them, directly or through a group (the same set the shelf shows) — has a sticker.
	 * A sticker, once earned, stays even if the assignment is later removed.
	 */
	@Transactional(readOnly = true)
	public StickersResponse stickers(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);

		Map<UUID, StickersResponse.Sticker> stickersByLesson = new LinkedHashMap<>();
		for (LessonAttempt attempt : lessonAttemptRepository
				.findByChildIdAndResultOrderByCompletedAtAsc(childId, LessonAttempt.RESULT_COMPLETED)) {
			LessonVersion version = lessonVersionRepository.findById(attempt.getLessonVersionId()).orElseThrow();
			if (stickersByLesson.containsKey(version.getLessonId())) {
				continue;
			}
			Lesson lesson = lessonRepository.findById(version.getLessonId()).orElseThrow();
			JsonNode scenario = objectMapper.readTree(version.getScenario());
			JsonNode bridge = scenarioAudioEnricher.withAudioUrls(scenario.path("reality_bridge"));
			stickersByLesson.put(lesson.getId(), new StickersResponse.Sticker(lesson.getId(), version.getId(),
					lesson.getTitle(), scenario.path("topic").asText(), attempt.getCompletedAt(),
					new StickersResponse.Bridge(bridge.path("key").asText(), bridge.path("text").asText(),
							bridge.hasNonNull("audioUrl") ? bridge.get("audioUrl").asText() : null)));
		}

		Map<UUID, String> playableTopicByLesson = new LinkedHashMap<>();
		for (LessonAssignment assignment : resolveAssignmentsForChild(childId)) {
			LessonVersion version = lessonVersionRepository.findById(assignment.getLessonVersionId()).orElseThrow();
			if (version.isPublished()) {
				playableTopicByLesson.putIfAbsent(version.getLessonId(),
						objectMapper.readTree(version.getScenario()).path("topic").asText());
			}
		}

		List<StickersResponse.LockedSticker> locked = new ArrayList<>();
		Set<String> topics = new LinkedHashSet<>(playableTopicByLesson.values());
		playableTopicByLesson.forEach((lessonId, topic) -> {
			if (!stickersByLesson.containsKey(lessonId)) {
				locked.add(new StickersResponse.LockedSticker(lessonId, topic));
				topics.remove(topic);
			}
		});
		return new StickersResponse(List.copyOf(stickersByLesson.values()), List.copyOf(topics), locked);
	}

	@Transactional(readOnly = true)
	public List<TeacherResultChild> groupResults(UUID teacherId, UUID groupId) {
		Group group = groupRepository.findById(groupId).orElseThrow(this::notFound);
		if (!group.getTeacherId().equals(teacherId)) {
			throw notFound();
		}
		// Only lessons this teacher gave: the group's assignments and their own individual ones. A
		// child in several groups has other teachers' lessons too, and those aren't this teacher's.
		Set<UUID> groupAssignments = lessonAssignmentRepository.findByGroupId(groupId).stream()
				.map(LessonAssignment::getId)
				.collect(Collectors.toSet());
		List<TeacherResultChild> results = new ArrayList<>();
		for (var member : groupMemberRepository.findById_GroupId(groupId)) {
			UUID childId = member.getId().getChildId();
			Child child = childRepository.findById(childId).orElseThrow();
			Set<UUID> ownAssignments = new HashSet<>(groupAssignments);
			lessonAssignmentRepository.findByChildId(childId).stream()
					.filter(a -> teacherId.equals(a.getAssignedBy()))
					.forEach(a -> ownAssignments.add(a.getId()));
			List<TeacherResultAttempt> attempts = lessonAttemptRepository.findByChildIdOrderByStartedAtDesc(childId)
					.stream()
					.filter(attempt -> ownAssignments.contains(attempt.getAssignmentId()))
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
				attempt.getScore(), sceneResults(attempt, version));
	}

	private List<TeacherSceneResult> sceneResults(LessonAttempt attempt, LessonVersion version) {
		Map<String, JsonNode> scenes = new HashMap<>();
		for (JsonNode scene : objectMapper.readTree(version.getScenario()).path("scenes")) {
			scenes.put(scene.path("key").asText(), scene);
		}
		Map<String, List<SceneAnswer>> byScene = new LinkedHashMap<>();
		sceneAnswerRepository.findByAttemptId(attempt.getId()).stream()
				.sorted(Comparator.comparing(SceneAnswer::getAnsweredAt))
				.forEach(answer -> byScene.computeIfAbsent(answer.getSceneKey(), k -> new ArrayList<>()).add(answer));
		List<TeacherSceneResult> results = new ArrayList<>();
		byScene.forEach((key, answers) -> {
			JsonNode scene = scenes.get(key);
			List<SceneAnswer> firstTries = answers.stream().filter(a -> a.getTryNo() == 1).toList();
			results.add(new TeacherSceneResult(key,
					scene == null ? null : scene.path("type").asText(null),
					scene != null && scene.path("is_control").asBoolean(false),
					!firstTries.isEmpty() && firstTries.stream().allMatch(SceneAnswer::isCorrect),
					answers.stream().mapToInt(SceneAnswer::getTryNo).max().orElse(0),
					answers.stream().mapToInt(SceneAnswer::getHintsUsed).max().orElse(0)));
		});
		return results;
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
