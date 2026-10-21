package ua.kidlearn.kidplay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.config.KidPlayProperties;

/**
 * The play-session rules. A session starts with the first lesson after a pause longer than
 * app.kid.session-gap; the limit is checked only when a lesson ends (it never cuts a lesson short),
 * and once reached the child rests for app.kid.rest-duration or until a parent wakes them.
 */
@Service
public class PlayStateService {

	/** Parent-selectable limits (docs/UX_гайд_дитячого_режиму.md §6); null means no limit. */
	private static final Set<Integer> ALLOWED_LIMITS = Set.of(10, 15, 20, 30);
	// Same as the column's DB default — what a child without a row yet gets.
	private static final int DEFAULT_LIMIT_MINUTES = 15;

	private final ChildPlayStateRepository repository;
	private final ParentChildRepository parentChildRepository;
	private final KidPlayProperties properties;
	private final Clock clock;

	public PlayStateService(ChildPlayStateRepository repository, ParentChildRepository parentChildRepository,
			KidPlayProperties properties, Clock clock) {
		this.repository = repository;
		this.parentChildRepository = parentChildRepository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PlayStateResponse state(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		return repository.findById(childId)
				.map(this::toResponse)
				.orElseGet(() -> new PlayStateResponse(false, null, DEFAULT_LIMIT_MINUTES));
	}

	@Transactional
	public PlayStateResponse updateSettings(UUID parentId, UUID childId, Integer sessionLimitMinutes) {
		requireChildOwnership(parentId, childId);
		if (sessionLimitMinutes != null && !ALLOWED_LIMITS.contains(sessionLimitMinutes)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionLimitMinutes must be 10, 15, 20, 30 or null");
		}
		ChildPlayState state = lockedState(childId);
		state.setSessionLimitMinutes(sessionLimitMinutes == null ? null : sessionLimitMinutes.shortValue());
		return toResponse(state);
	}

	@Transactional
	public PlayStateResponse wake(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		ChildPlayState state = lockedState(childId);
		state.wake();
		return toResponse(state);
	}

	/**
	 * A lesson is starting. The caller has already checked ownership. Refuses while resting;
	 * otherwise opens a new session if there's none or the last activity is older than the gap.
	 */
	@Transactional
	public void onLessonStart(UUID childId) {
		Instant now = clock.instant();
		ChildPlayState state = lockedState(childId);
		if (state.isResting(now)) {
			throw new ChildRestingException(state.getRestUntil());
		}
		Instant lastActivity = state.getLastActivityAt();
		boolean newSession = state.getSessionStartedAt() == null || lastActivity == null
				|| Duration.between(lastActivity, now).compareTo(properties.sessionGap()) > 0;
		if (newSession) {
			state.startSession(now);
		}
		state.touch(now);
	}

	/** A lesson has ended (the caller has already checked ownership): the only place the limit applies. */
	@Transactional
	public PlayStateResponse onLessonEnd(UUID childId) {
		Instant now = clock.instant();
		ChildPlayState state = lockedState(childId);
		state.touch(now);
		Short limit = state.getSessionLimitMinutes();
		Instant sessionStart = state.getSessionStartedAt();
		if (limit != null && sessionStart != null
				&& Duration.between(sessionStart, now).compareTo(properties.limitUnit().multipliedBy(limit)) >= 0) {
			state.rest(now.plus(properties.restDuration()));
		}
		return toResponse(state);
	}

	private ChildPlayState lockedState(UUID childId) {
		repository.insertIfAbsent(childId);
		return repository.findForUpdate(childId).orElseThrow();
	}

	private PlayStateResponse toResponse(ChildPlayState state) {
		boolean resting = state.isResting(clock.instant());
		Short limit = state.getSessionLimitMinutes();
		return new PlayStateResponse(resting, resting ? state.getRestUntil() : null,
				limit == null ? null : limit.intValue());
	}

	private void requireChildOwnership(UUID parentId, UUID childId) {
		if (!parentChildRepository.existsById_ParentIdAndId_ChildId(parentId, childId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

}
