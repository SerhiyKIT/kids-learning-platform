package ua.kidlearn.aipipeline;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.ratelimit.ClientIpResolver;

/** Part of the internal content pipeline (see the aipipeline package docs). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoicingController {

	private final VoicingService voicingService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AdminVoicingController(VoicingService voicingService, AuditService auditService,
			ClientIpResolver clientIpResolver) {
		this.voicingService = voicingService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	/**
	 * Audited whenever the TTS provider was actually called — on success, and on a provider
	 * failure too (lines synthesized before it may already have been paid for). A version that
	 * isn't approved/published never reaches the provider and isn't audited.
	 */
	@PostMapping("/lesson-versions/{id}/voice")
	public VoicingResult voice(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest httpRequest) {
		VoicingResult result;
		try {
			result = voicingService.voice(id);
		} catch (TtsSynthesisException e) {
			recordVoicing(id, principal, httpRequest);
			throw e;
		}
		recordVoicing(id, principal, httpRequest);
		return result;
	}

	private void recordVoicing(UUID versionId, AppUserPrincipal principal, HttpServletRequest httpRequest) {
		auditService.record(principal.getId(), principal.getRole(), AuditAction.VOICE_VERSION,
				AuditTargetType.LESSON_VERSION, versionId, clientIpResolver.resolve(httpRequest));
	}

}
