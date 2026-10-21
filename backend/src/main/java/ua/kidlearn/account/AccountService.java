package ua.kidlearn.account;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.ReauthenticationService;
import ua.kidlearn.children.ChildRepository;
import ua.kidlearn.children.ChildService;
import ua.kidlearn.children.ParentChild;
import ua.kidlearn.children.ParentChildRepository;
import ua.kidlearn.users.Role;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

/**
 * Account-level actions of the signed-in adult. Every destructive one re-checks the password
 * through ReauthenticationService, so it shares the login failure limits.
 */
@Service
public class AccountService {

	private final UserRepository userRepository;
	private final ParentChildRepository parentChildRepository;
	private final ChildRepository childRepository;
	private final ChildService childService;
	private final ReauthenticationService reauthenticationService;
	private final AuditService auditService;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public AccountService(UserRepository userRepository, ParentChildRepository parentChildRepository,
			ChildRepository childRepository, ChildService childService,
			ReauthenticationService reauthenticationService, AuditService auditService,
			PasswordEncoder passwordEncoder, Clock clock) {
		this.userRepository = userRepository;
		this.parentChildRepository = parentChildRepository;
		this.childRepository = childRepository;
		this.childService = childService;
		this.reauthenticationService = reauthenticationService;
		this.auditService = auditService;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DeletionPreview deletionPreview(UUID parentId) {
		List<DeletionPreview.ChildFate> fates = parentChildRepository.findById_ParentId(parentId).stream()
				.map(link -> {
					UUID childId = link.getId().getChildId();
					return new DeletionPreview.ChildFate(childId,
							childRepository.findById(childId).orElseThrow().getDisplayName(),
							parentChildRepository.countById_ChildId(childId) == 1);
				})
				.toList();
		return new DeletionPreview(fates);
	}

	/**
	 * Real deletion, one transaction ("right to be forgotten" is not a soft delete): children whose
	 * only parent this is are deleted with all their data (ChildService.delete, DB cascade); shared
	 * children stay with the other parent and only the link goes (V10: parent_child cascades,
	 * consents.parent_id and children.created_by become NULL). The audit entries are written first,
	 * inside the transaction, so they survive with actor_id NULL.
	 */
	@Transactional
	public void deleteParentAccount(UUID userId, Role role, String email, String password, String clientIp) {
		if (role == Role.TEACHER) {
			throw new AccountDeletionNotAllowedException("DELETION_VIA_ADMIN",
					"Акаунт вчителя видаляє адміністратор — зверніться до нього.");
		}
		if (role != Role.PARENT) {
			throw new AccountDeletionNotAllowedException("ADMIN_NOT_SELF_DELETABLE",
					"Акаунт адміністратора не можна видалити самостійно, щоб не втратити доступ.");
		}
		reauthenticationService.reauthenticate(userId, email, password, clientIp);

		auditService.recordWithinTransaction(userId, role, AuditAction.DELETE_ACCOUNT, AuditTargetType.USER, userId,
				clientIp);
		for (ParentChild link : parentChildRepository.findById_ParentId(userId)) {
			UUID childId = link.getId().getChildId();
			if (parentChildRepository.countById_ChildId(childId) == 1) {
				auditService.recordWithinTransaction(userId, role, AuditAction.DELETE_CHILD, AuditTargetType.CHILD,
						childId, clientIp);
				childService.delete(userId, childId);
			}
		}
		childRepository.flush();
		userRepository.deleteById(userId);
		userRepository.flush();
	}

	/** Signs out every session of this account, the current one included. */
	@Transactional
	public void logoutEverywhere(UUID userId) {
		userRepository.findById(userId).orElseThrow().invalidateSessionsBefore(clock.instant());
	}

	/**
	 * Same rules as registration; the current password is re-checked (with the login limits).
	 * Every other session is signed out; the returned instant re-marks the current session as
	 * authenticated now, so it alone stays valid.
	 */
	@Transactional
	public Instant changePassword(UUID userId, String email, String currentPassword, String newPassword,
			String clientIp) {
		reauthenticationService.reauthenticate(userId, email, currentPassword, clientIp);
		Instant now = clock.instant();
		User user = userRepository.findById(userId).orElseThrow();
		user.changePassword(passwordEncoder.encode(newPassword));
		user.invalidateSessionsBefore(now);
		return now;
	}

}
