package ua.kidlearn.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.lessons.CreateLessonRequest;
import ua.kidlearn.lessons.CreateLessonVersionRequest;
import ua.kidlearn.lessons.Lesson;
import ua.kidlearn.lessons.LessonResponse;
import ua.kidlearn.lessons.LessonService;
import ua.kidlearn.lessons.LessonVersion;
import ua.kidlearn.lessons.LessonVersionDetailResponse;
import ua.kidlearn.lessons.LessonVersionResponse;
import ua.kidlearn.lessons.PendingReviewEntry;
import ua.kidlearn.lessons.RejectVersionRequest;
import ua.kidlearn.ratelimit.ClientIpResolver;

/**
 * Admin content entry (manual or AI-generated, both scenario-validated) plus the moderation
 * lifecycle: auto_validated -&gt; approve -&gt; approved -&gt; publish -&gt; published, or
 * auto_validated -&gt; reject -&gt; archived. Only 'approved' versions may publish.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLessonController {

	private final LessonService lessonService;
	private final ObjectMapper objectMapper;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AdminLessonController(LessonService lessonService, ObjectMapper objectMapper, AuditService auditService,
			ClientIpResolver clientIpResolver) {
		this.lessonService = lessonService;
		this.objectMapper = objectMapper;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
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

	@GetMapping("/lesson-versions")
	public List<PendingReviewEntry> listByStatus(@RequestParam String status) {
		return lessonService.listByStatus(status);
	}

	@GetMapping("/lesson-versions/{id}")
	public LessonVersionDetailResponse getVersion(@PathVariable UUID id) {
		return LessonVersionDetailResponse.from(lessonService.getVersion(id), objectMapper);
	}

	@PostMapping("/lesson-versions/{id}/approve")
	public LessonVersionResponse approve(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest request) {
		LessonVersionResponse response = LessonVersionResponse.from(lessonService.approve(id, principal.getId()));
		auditService.record(principal.getId(), principal.getRole(), AuditAction.APPROVE_VERSION,
				AuditTargetType.LESSON_VERSION, id, clientIpResolver.resolve(request));
		return response;
	}

	@PostMapping("/lesson-versions/{id}/reject")
	public LessonVersionResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectVersionRequest body,
			@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest request) {
		LessonVersionResponse response = LessonVersionResponse.from(lessonService.reject(id, body.reason()));
		auditService.record(principal.getId(), principal.getRole(), AuditAction.REJECT_VERSION,
				AuditTargetType.LESSON_VERSION, id, clientIpResolver.resolve(request));
		return response;
	}

	@PostMapping("/lesson-versions/{id}/publish")
	public LessonVersionResponse publish(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest request) {
		LessonVersionResponse response = LessonVersionResponse.from(lessonService.publish(id));
		auditService.record(principal.getId(), principal.getRole(), AuditAction.PUBLISH_VERSION,
				AuditTargetType.LESSON_VERSION, id, clientIpResolver.resolve(request));
		return response;
	}

}
