package ua.kidlearn.aipipeline;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Part of the internal content pipeline (see the aipipeline package docs). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoicingController {

	private final VoicingService voicingService;

	public AdminVoicingController(VoicingService voicingService) {
		this.voicingService = voicingService;
	}

	@PostMapping("/lesson-versions/{id}/voice")
	public VoicingResult voice(@PathVariable UUID id) {
		return voicingService.voice(id);
	}

}
