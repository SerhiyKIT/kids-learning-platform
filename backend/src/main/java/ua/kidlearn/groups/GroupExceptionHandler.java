package ua.kidlearn.groups;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = { GroupController.class, GroupMembershipController.class })
class GroupExceptionHandler {

	@ExceptionHandler(ChildNotActiveException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleChildNotActive() {
		return Map.of("code", "CHILD_NOT_ACTIVE");
	}

	@ExceptionHandler(GroupInactiveException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleGroupInactive() {
		return Map.of("code", "GROUP_INACTIVE");
	}

}
