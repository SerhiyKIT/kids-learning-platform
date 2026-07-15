package ua.kidlearn.groups;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.children.Child;
import ua.kidlearn.children.ChildRepository;
import ua.kidlearn.children.ParentChildRepository;

@Service
public class GroupMembershipService {

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ChildRepository childRepository;
	private final ParentChildRepository parentChildRepository;

	public GroupMembershipService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
			ChildRepository childRepository, ParentChildRepository parentChildRepository) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.childRepository = childRepository;
		this.parentChildRepository = parentChildRepository;
	}

	@Transactional
	public void join(UUID parentId, String joinCode, UUID childId) {
		requireChildOwnership(parentId, childId);
		Child child = childRepository.findById(childId).orElseThrow(this::notFound);
		if (!child.isActive()) {
			throw new ChildNotActiveException();
		}

		Group group = groupRepository.findByJoinCode(joinCode).orElseThrow(this::notFound);
		if (!group.isActive()) {
			throw new GroupInactiveException();
		}

		if (!groupMemberRepository.existsById_ChildIdAndId_GroupId(childId, group.getId())) {
			groupMemberRepository.save(new GroupMember(childId, group.getId(), Instant.now()));
		}
	}

	@Transactional(readOnly = true)
	public List<Group> listForChild(UUID parentId, UUID childId) {
		requireChildOwnership(parentId, childId);
		List<UUID> groupIds = groupMemberRepository.findById_ChildId(childId).stream()
				.map(member -> member.getId().getGroupId())
				.toList();
		return groupRepository.findAllById(groupIds);
	}

	@Transactional
	public void leave(UUID parentId, UUID childId, UUID groupId) {
		requireChildOwnership(parentId, childId);
		groupMemberRepository.deleteById_ChildIdAndId_GroupId(childId, groupId);
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
