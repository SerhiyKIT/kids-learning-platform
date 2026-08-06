package ua.kidlearn.aipipeline;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Part of the internal content pipeline (see the aipipeline package docs). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenerationController {

	private final LessonGenerationService lessonGenerationService;

	public AdminGenerationController(LessonGenerationService lessonGenerationService) {
		this.lessonGenerationService = lessonGenerationService;
	}

	@PostMapping("/lessons/{id}/generate")
	public GenerationOutcome generate(@PathVariable UUID id, @Valid @RequestBody GenerationRequest request) {
		return lessonGenerationService.generate(id, request);
	}

}
