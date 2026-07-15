package ua.kidlearn.children;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.consents.Consent;
import ua.kidlearn.consents.ConsentRepository;
import ua.kidlearn.users.User;
import ua.kidlearn.users.UserRepository;

@Service
public class ChildService {

	// No avatar library exists yet; the DB column is NOT NULL, so an omitted
	// avatarId falls back to this placeholder until one is picked.
	private static final String DEFAULT_AVATAR_ID = "default";
	private static final Set<String> VALID_RELATIONS = Set.of("mother", "father", "guardian");
	private static final int MAX_CHILD_AGE_YEARS = 8;

	private final ChildRepository childRepository;
	private final ParentChildRepository parentChildRepository;
	private final ConsentRepository consentRepository;
	private final UserRepository userRepository;

	public ChildService(ChildRepository childRepository, ParentChildRepository parentChildRepository,
			ConsentRepository consentRepository, UserRepository userRepository) {
		this.childRepository = childRepository;
		this.parentChildRepository = parentChildRepository;
		this.consentRepository = consentRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public Child create(UUID parentId, CreateChildRequest request) {
		User parent = userRepository.findById(parentId)
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + parentId));
		if (!parent.isEmailVerified()) {
			throw new EmailNotVerifiedException();
		}
		if (!VALID_RELATIONS.contains(request.relation())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid relation");
		}
		int currentYear = Year.now().getValue();
		if (request.birthYear() < currentYear - MAX_CHILD_AGE_YEARS || request.birthYear() > currentYear) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "birthYear out of range");
		}

		String avatarId = (request.avatarId() == null || request.avatarId().isBlank())
				? DEFAULT_AVATAR_ID
				: request.avatarId();
		Child child = childRepository.save(
				new Child(request.displayName(), (short) request.birthYear(), avatarId, parentId));

		// The creating parent is, by definition, this child's only linked adult so
		// far — always the primary consent contact at this point.
		parentChildRepository.save(new ParentChild(parentId, child.getId(), request.relation(), true));

		return child;
	}

	@Transactional
	public void grantAccountConsent(UUID parentId, UUID childId) {
		requireOwnership(parentId, childId);
		Child child = childRepository.findById(childId).orElseThrow(this::notFound);
		if (!child.isActive()) {
			child.activate();
			consentRepository.save(new Consent(childId, parentId, Consent.TYPE_ACCOUNT, Instant.now()));
		}
	}

	@Transactional(readOnly = true)
	public List<Child> listForParent(UUID parentId) {
		List<UUID> childIds = parentChildRepository.findById_ParentId(parentId).stream()
				.map(link -> link.getId().getChildId())
				.toList();
		return childRepository.findAllById(childIds);
	}

	@Transactional(readOnly = true)
	public Child getForParent(UUID parentId, UUID childId) {
		requireOwnership(parentId, childId);
		return childRepository.findById(childId).orElseThrow(this::notFound);
	}

	@Transactional
	public void delete(UUID parentId, UUID childId) {
		requireOwnership(parentId, childId);
		// Right to be forgotten: DB FKs cascade to parent_child, consents,
		// attempts, scene_answers, assistant_dialogs, group_members, style_prefs.
		childRepository.deleteById(childId);
	}

	private void requireOwnership(UUID parentId, UUID childId) {
		if (!parentChildRepository.existsById_ParentIdAndId_ChildId(parentId, childId)) {
			throw notFound();
		}
	}

	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

}
