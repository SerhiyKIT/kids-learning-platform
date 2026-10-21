package ua.kidlearn.admin;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.groups.GroupRepository;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Admin-side teacher management: listing and deactivation. Invitations themselves (account
 * creation, tokens, the email) live in AuthService, next to the other token-based auth flows.
 */
@Service
public class AdminTeacherService {

	static final String STATUS_INVITED = "invited";
	static final String STATUS_ACTIVE = "active";
	static final String STATUS_DEACTIVATED = "deactivated";

	private final UserRepository userRepository;
	private final GroupRepository groupRepository;
	private final Clock clock;

	public AdminTeacherService(UserRepository userRepository, GroupRepository groupRepository, Clock clock) {
		this.userRepository = userRepository;
		this.groupRepository = groupRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<TeacherEntry> list() {
		List<User> teachers = userRepository.findByRoleOrderByCreatedAtDescIdDesc(Role.TEACHER);
		Map<UUID, Long> groupCounts = new HashMap<>();
		if (!teachers.isEmpty()) {
			for (Object[] row : groupRepository.countByTeacherIds(teachers.stream().map(User::getId).toList())) {
				groupCounts.put((UUID) row[0], ((Number) row[1]).longValue());
			}
		}
		return teachers.stream()
				.map(t -> new TeacherEntry(t.getId(), t.getEmail(), t.getDisplayName(), status(t),
						groupCounts.getOrDefault(t.getId(), 0L), t.getCreatedAt()))
				.toList();
	}

	/**
	 * Soft delete: the teacher can no longer log in (and an open session ends — see
	 * ActiveAccountFilter), while their groups, assignments and every child's results stay put.
	 * Idempotent. Anything that isn't a TEACHER is a 404, so this can't touch parents or admins.
	 */
	@Transactional
	public void deactivate(UUID teacherId) {
		User teacher = userRepository.findById(teacherId)
				.filter(u -> u.getRole() == Role.TEACHER)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
		if (!teacher.isDeleted()) {
			teacher.markDeleted(clock.instant());
		}
	}

	static String status(User teacher) {
		if (teacher.isDeleted()) {
			return STATUS_DEACTIVATED;
		}
		return teacher.isEmailVerified() ? STATUS_ACTIVE : STATUS_INVITED;
	}

}
