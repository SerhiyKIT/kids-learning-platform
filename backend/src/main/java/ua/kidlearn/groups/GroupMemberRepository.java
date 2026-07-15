package ua.kidlearn.groups;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

	boolean existsById_ChildIdAndId_GroupId(UUID childId, UUID groupId);

	List<GroupMember> findById_GroupId(UUID groupId);

	List<GroupMember> findById_ChildId(UUID childId);

	void deleteById_ChildIdAndId_GroupId(UUID childId, UUID groupId);

}
