package ua.kidlearn.lessons;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = { CatalogController.class, AssignmentController.class })
class LessonExceptionHandler {

	@ExceptionHandler(NotPublishedException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	Map<String, String> handleNotPublished() {
		return Map.of("code", "NOT_PUBLISHED");
	}

}
