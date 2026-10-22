package ua.kidlearn.lessons;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.kidlearn.groups.GroupInactiveException;

@RestControllerAdvice(basePackageClasses = { CatalogController.class, AssignmentController.class })
class LessonExceptionHandler {

	@ExceptionHandler(NotPublishedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleNotPublished() {
		return Map.of("code", "NOT_PUBLISHED");
	}

	@ExceptionHandler(AssignmentExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleAssignmentExists() {
		return Map.of("code", "ASSIGNMENT_EXISTS");
	}

	@ExceptionHandler(GroupInactiveException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleGroupInactive() {
		return Map.of("code", "GROUP_INACTIVE");
	}

}
