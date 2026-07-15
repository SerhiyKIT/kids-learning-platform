package ua.kidlearn.groups;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.children.Child;
import ua.kidlearn.children.ChildRepository;

@Service
public class GroupService {

	private static final int MAX_JOIN_CODE_ATTEMPTS = 10;

	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final ChildRepository childRepository;
	private final JoinCodeGenerator joinCodeGenerator;

	public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
			ChildRepository childRepository, JoinCodeGenerator joinCodeGenerator) {
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
		this.childRepository = childRepository;
		this.joinCodeGenerator = joinCodeGenerator;
	}

	@Transactional
	public Group create(UUID teacherId, String name) {
		return groupRepository.save(new Group(teacherId, name, generateUniqueJoinCode()));
	}

	@Transactional(readOnly = true)
	public List<Group> listForTeacher(UUID teacherId) {
		return groupRepository.findByTeacherId(teacherId);
	}

	@Transactional
	public void archive(UUID teacherId, UUID groupId) {
		requireOwnedGroup(teacherId, groupId).archive();
	}

	@Transactional
	public Group regenerateCode(UUID teacherId, UUID groupId) {
		Group group = requireOwnedGroup(teacherId, groupId);
		group.changeJoinCode(generateUniqueJoinCode());
		return group;
	}

	@Transactional(readOnly = true)
	public List<GroupMemberInfo> listMembers(UUID teacherId, UUID groupId) {
		requireOwnedGroup(teacherId, groupId);
		List<UUID> childIds = groupMemberRepository.findById_GroupId(groupId).stream()
				.map(member -> member.getId().getChildId())
				.toList();
		return childRepository.findAllById(childIds).stream()
				.map(GroupService::toMemberInfo)
				.toList();
	}

	@Transactional
	public void removeMember(UUID teacherId, UUID groupId, UUID childId) {
		requireOwnedGroup(teacherId, groupId);
		groupMemberRepository.deleteById_ChildIdAndId_GroupId(childId, groupId);
	}

	private String generateUniqueJoinCode() {
		for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
			String code = joinCodeGenerator.generate();
			if (!groupRepository.existsByJoinCode(code)) {
				return code;
			}
		}
		throw new IllegalStateException("Could not generate a unique join code after "
				+ MAX_JOIN_CODE_ATTEMPTS + " attempts");
	}

	private Group requireOwnedGroup(UUID teacherId, UUID groupId) {
		Group group = groupRepository.findById(groupId).orElseThrow(this::notFound);
		if (!group.getTeacherId().equals(teacherId)) {
			throw notFound();
		}
		return group;
	}

	private static GroupMemberInfo toMemberInfo(Child child) {
		return new GroupMemberInfo(child.getId(), child.getDisplayName(), child.getAvatarId());
	}

	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

}
