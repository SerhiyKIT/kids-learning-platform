package ua.kidlearn.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.kidlearn.audit.AuditAction;
import ua.kidlearn.audit.AuditService;
import ua.kidlearn.audit.AuditTargetType;
import ua.kidlearn.auth.AppUserPrincipal;
import ua.kidlearn.ratelimit.ClientIpResolver;
import ua.kidlearn.users.SessionAuthentication;

/** The signed-in adult's own data and account (docs/Ролі_та_приватність.md §1–§3). */
@RestController
public class AccountController {

	private final AccountService accountService;
	private final DataExportService dataExportService;
	private final AuditService auditService;
	private final ClientIpResolver clientIpResolver;

	public AccountController(AccountService accountService, DataExportService dataExportService,
			AuditService auditService, ClientIpResolver clientIpResolver) {
		this.accountService = accountService;
		this.dataExportService = dataExportService;
		this.auditService = auditService;
		this.clientIpResolver = clientIpResolver;
	}

	/** One child's data as a JSON download (parents of this child only; another child → 404). */
	@GetMapping("/api/children/{childId}/export")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ChildDataExport> exportChild(@AuthenticationPrincipal AppUserPrincipal principal,
			@PathVariable UUID childId, HttpServletRequest request) {
		ChildDataExport export = dataExportService.exportChild(principal.getId(), childId);
		auditService.record(principal.getId(), principal.getRole(), AuditAction.EXPORT_CHILD_DATA,
				AuditTargetType.CHILD, childId, clientIpResolver.resolve(request));
		return attachment(export, "kidlearn-child-" + childId + ".json");
	}

	/** The account's own data plus a full export of each of its children. */
	@GetMapping("/api/me/export")
	public ResponseEntity<AccountDataExport> exportAccount(@AuthenticationPrincipal AppUserPrincipal principal,
			HttpServletRequest request) {
		AccountDataExport export = dataExportService.exportAccount(principal.getId());
		auditService.record(principal.getId(), principal.getRole(), AuditAction.EXPORT_ACCOUNT_DATA,
				AuditTargetType.USER, principal.getId(), clientIpResolver.resolve(request));
		return attachment(export, "kidlearn-account.json");
	}

	@GetMapping("/api/me/deletion-preview")
	@PreAuthorize("hasRole('PARENT')")
	public DeletionPreview deletionPreview(@AuthenticationPrincipal AppUserPrincipal principal) {
		return accountService.deletionPreview(principal.getId());
	}

	/** Deletes the parent's account for real, then ends this session. 401 on a wrong password, 429 over the limit. */
	@DeleteMapping("/api/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAccount(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody PasswordRequest body, HttpServletRequest request) {
		accountService.deleteParentAccount(principal.getId(), principal.getRole(), principal.getUsername(),
				body.password(), clientIpResolver.resolve(request));
		endThisSession(request);
	}

	/** Every session of the account ends, this one included. */
	@PostMapping("/api/me/logout-everywhere")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logoutEverywhere(@AuthenticationPrincipal AppUserPrincipal principal, HttpServletRequest request) {
		accountService.logoutEverywhere(principal.getId());
		endThisSession(request);
	}

	/** Other sessions end; this one is re-marked as authenticated now and keeps working. */
	@PostMapping("/api/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
		Instant now = accountService.changePassword(principal.getId(), principal.getUsername(),
				body.currentPassword(), body.newPassword(), clientIpResolver.resolve(request));
		SessionAuthentication.markAuthenticated(request.getSession(), now);
	}

	private static void endThisSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
	}

	private static <T> ResponseEntity<T> attachment(T body, String filename) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
				.body(body);
	}

}
