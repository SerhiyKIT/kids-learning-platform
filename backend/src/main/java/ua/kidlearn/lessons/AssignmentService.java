package ua.kidlearn.lessons;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ua.kidlearn.groups.Group;
import ua.kidlearn.groups.GroupMemberRepository;
import ua.kidlearn.groups.GroupRepository;

@Service
public class AssignmentService {

	private final LessonAssignmentRepository lessonAssignmentRepository;
	private final LessonVersionRepository lessonVersionRepository;
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;

	public AssignmentService(LessonAssignmentRepository lessonAssignmentRepository,
			LessonVersionRepository lessonVersionRepository, GroupRepository groupRepository,
			GroupMemberRepository groupMemberRepository) {
		this.lessonAssignmentRepository = lessonAssignmentRepository;
		this.lessonVersionRepository = lessonVersionRepository;
		this.groupRepository = groupRepository;
		this.groupMemberRepository = groupMemberRepository;
	}

	@Transactional
	public LessonAssignment assign(UUID teacherId, CreateAssignmentRequest request) {
		boolean hasGroup = request.groupId() != null;
		boolean hasChild = request.childId() != null;
		if (hasGroup == hasChild) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly one of groupId or childId is required");
		}

		LessonVersion version = lessonVersionRepository.findById(request.lessonVersionId())
				.filter(LessonVersion::isPublished)
				.orElseThrow(NotPublishedException::new);

		if (hasGroup) {
			requireOwnedGroup(teacherId, request.groupId());
		} else {
			requireChildInTeachersGroup(teacherId, request.childId());
		}

		return lessonAssignmentRepository.save(new LessonAssignment(version.getId(), request.groupId(),
				request.childId(), teacherId, request.availableFrom(), request.dueAt()));
	}

	@Transactional(readOnly = true)
	public List<LessonAssignment> listForTeacherAndGroup(UUID teacherId, UUID groupId) {
		requireOwnedGroup(teacherId, groupId);
		return lessonAssignmentRepository.findByGroupId(groupId);
	}

	@Transactional
	public void delete(UUID teacherId, UUID assignmentId) {
		LessonAssignment assignment = lessonAssignmentRepository.findById(assignmentId).orElseThrow(this::notFound);
		if (!assignment.getAssignedBy().equals(teacherId)) {
			throw notFound();
		}
		lessonAssignmentRepository.delete(assignment);
	}

	private void requireOwnedGroup(UUID teacherId, UUID groupId) {
		Group group = groupRepository.findById(groupId).orElseThrow(this::notFound);
		if (!group.getTeacherId().equals(teacherId)) {
			throw notFound();
		}
	}

	private void requireChildInTeachersGroup(UUID teacherId, UUID childId) {
		Set<UUID> teacherGroupIds = groupRepository.findByTeacherId(teacherId).stream()
				.map(Group::getId)
				.collect(Collectors.toSet());
		boolean childInTeachersGroup = groupMemberRepository.findById_ChildId(childId).stream()
				.anyMatch(member -> teacherGroupIds.contains(member.getId().getGroupId()));
		if (!childInTeachersGroup) {
			throw notFound();
		}
	}

	private ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND);
	}

}
