package ua.kidlearn.admin;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.lessons.CreateLessonRequest;
import ua.kidlearn.lessons.CreateLessonVersionRequest;
import ua.kidlearn.lessons.Lesson;
import ua.kidlearn.lessons.LessonResponse;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionResponse;

/**
 * STOPGAP content entry for admins, standing in for the future AI generation
 * pipeline + moderation UI. Publishing here is an unreviewed status flip —
 * fine for seeding the MVP catalog, not a substitute for real moderation.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLessonController {

	private final LessonService lessonService;

	public AdminLessonController(LessonService lessonService) {
		this.lessonService = lessonService;
	}

	@PostMapping("/lessons")
	@ResponseStatus(HttpStatus.CREATED)
	public LessonResponse createLesson(@Valid @RequestBody CreateLessonRequest request) {
		Lesson lesson = lessonService.createLesson(request.moduleCode(), request.title());
		return new LessonResponse(lesson.getId(), request.moduleCode(), lesson.getTitle());
	}

	@PostMapping("/lessons/{id}/versions")
	@ResponseStatus(HttpStatus.CREATED)
	public LessonVersionResponse createVersion(@PathVariable UUID id,
			@Valid @RequestBody CreateLessonVersionRequest request) {
		LessonVersion version = lessonService.createVersion(id, request.scenario(), request.generatedBy());
		return LessonVersionResponse.from(version);
	}

	@PostMapping("/lesson-versions/{id}/publish")
	public LessonVersionResponse publish(@PathVariable UUID id) {
		return LessonVersionResponse.from(lessonService.publish(id));
	}

}
