package ua.kidlearn.account;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.attempts.LessonAttempt;
import ua.kidlearn.attempts.LessonAttemptRepository;
import ua.kidlearn.attempts.SceneAnswerRepository;
import ua.kidlearn.children.Child;
import ua.kidlearn.children.ChildRepository;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.consents.ConsentRepository;
import ua.kidlearn.groups.GroupMemberRepository;
import ua.kidlearn.groups.GroupRepository;
import ua.kidlearn.kidplay.PlayStateService;
import ua.kidlearn.lessons.LessonRepository;
import ua.kidlearn.lessons.LessonVersionRepository;
import ua.kidlearn.themes.StyleAnswers;
import ua.kidlearn.themes.StyleService;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/** Builds the data exports. Ownership first: another parent's child is 404, like everywhere else. */
@Service
public class DataExportService {

	private final ChildRepository childRepository;
	private final ParentChildRepository parentChildRepository;
	private final ConsentRepository consentRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final GroupRepository groupRepository;
	private final LessonAttemptRepository lessonAttemptRepository;
	private final SceneAnswerRepository sceneAnswerRepository;
	private final LessonVersionRepository lessonVersionRepository;
	private final LessonRepository lessonRepository;
	private final PlayStateService playStateService;
	private final StyleService styleService;
	private final UserRepository userRepository;
	private final Clock clock;

	public DataExportService(ChildRepository childRepository, ParentChildRepository parentChildRepository,
			ConsentRepository consentRepository, GroupMemberRepository groupMemberRepository,
			GroupRepository groupRepository, LessonAttemptRepository lessonAttemptRepository,
			SceneAnswerRepository sceneAnswerRepository, LessonVersionRepository lessonVersionRepository,
			LessonRepository lessonRepository, PlayStateService playStateService, StyleService styleService,
			UserRepository userRepository, Clock clock) {
		this.childRepository = childRepository;
		this.parentChildRepository = parentChildRepository;
		this.consentRepository = consentRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.groupRepository = groupRepository;
		this.lessonAttemptRepository = lessonAttemptRepository;
		this.sceneAnswerRepository = sceneAnswerRepository;
		this.lessonVersionRepository = lessonVersionRepository;
		this.lessonRepository = lessonRepository;
		this.playStateService = playStateService;
		this.styleService = styleService;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ChildDataExport exportChild(UUID parentId, UUID childId) {
		if (!parentChildRepository.existsById_ParentIdAndId_ChildId(parentId, childId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		Child child = childRepository.findById(childId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<ChildDataExport.ConsentRecord> consents = consentRepository.findByChildIdOrderByGrantedAtAsc(childId).stream()
				.map(c -> new ChildDataExport.ConsentRecord(c.getType(), c.getGrantedAt(), c.getRevokedAt()))
				.toList();
		List<ChildDataExport.GroupRecord> groups = groupMemberRepository.findById_ChildId(childId).stream()
				.map(m -> groupRepository.findById(m.getId().getGroupId()).orElseThrow())
				.map(g -> new ChildDataExport.GroupRecord(g.getName()))
				.toList();
		List<ChildDataExport.AttemptRecord> attempts = lessonAttemptRepository.findByChildIdOrderByStartedAtDesc(childId)
				.stream()
				.map(this::attempt)
				.toList();
		ChildDataExport.PlaySettings playSettings = new ChildDataExport.PlaySettings(
				playStateService.state(parentId, childId).sessionLimitMinutes());
		StyleAnswers answers = styleService.style(parentId, childId).answers();
		ChildDataExport.StylePreferences style = answers == null ? null
				: new ChildDataExport.StylePreferences(answers.colorId(), answers.worldId(), answers.moodId());

		return new ChildDataExport(ChildDataExport.SCHEMA_VERSION, clock.instant(),
				new ChildDataExport.Profile(child.getId(), child.getDisplayName(), child.getBirthYear(),
						child.getAvatarId(), child.getStatus(), child.getCreatedAt()),
				consents, groups, attempts, playSettings, style);
	}

	@Transactional(readOnly = true)
	public AccountDataExport exportAccount(UUID userId) {
		User user = userRepository.findById(userId).orElseThrow();
		List<AccountDataExport.GivenConsent> given = consentRepository.findByParentIdOrderByGrantedAtAsc(userId).stream()
				.map(c -> new AccountDataExport.GivenConsent(c.getChildId(), c.getType(), c.getGrantedAt(), c.getRevokedAt()))
				.toList();
		List<ChildDataExport> children = parentChildRepository.findById_ParentId(userId).stream()
				.map(link -> exportChild(userId, link.getId().getChildId()))
				.toList();
		return new AccountDataExport(ChildDataExport.SCHEMA_VERSION, clock.instant(),
				new AccountDataExport.Account(user.getEmail(), user.getDisplayName(), user.getRole().name(),
						user.getLocale(), user.getCreatedAt(), user.getEmailVerifiedAt()),
				given, children);
	}

	private ChildDataExport.AttemptRecord attempt(LessonAttempt attempt) {
		var version = lessonVersionRepository.findById(attempt.getLessonVersionId()).orElseThrow();
		String title = lessonRepository.findById(version.getLessonId()).orElseThrow().getTitle();
		List<ChildDataExport.AnswerRecord> answers = sceneAnswerRepository.findByAttemptId(attempt.getId()).stream()
				.map(a -> new ChildDataExport.AnswerRecord(a.getSceneKey(), a.getTryNo(), a.getChosenOption(),
						a.isCorrect(), a.getHintsUsed(), a.getAnsweredAt()))
				.toList();
		return new ChildDataExport.AttemptRecord(attempt.getId(), title, attempt.getLessonVersionId(),
				attempt.getStartedAt(), attempt.getCompletedAt(), attempt.getResult(), attempt.getScore(), answers);
	}

}
