package ua.kidlearn.audit;

/** Recorded audit_log.action values: child-data access plus the content pipeline (generate → moderate → voice). */
public final class AuditAction {

	public static final String VIEW_CHILD_HISTORY = "VIEW_CHILD_HISTORY";
	public static final String VIEW_GROUP_RESULTS = "VIEW_GROUP_RESULTS";
	public static final String DELETE_CHILD = "DELETE_CHILD";
	public static final String DELETE_ACCOUNT = "DELETE_ACCOUNT";
	public static final String EXPORT_CHILD_DATA = "EXPORT_CHILD_DATA";
	public static final String EXPORT_ACCOUNT_DATA = "EXPORT_ACCOUNT_DATA";
	public static final String APPROVE_VERSION = "APPROVE_VERSION";
	public static final String REJECT_VERSION = "REJECT_VERSION";
	public static final String PUBLISH_VERSION = "PUBLISH_VERSION";
	public static final String GENERATE_VERSION = "GENERATE_VERSION";
	public static final String VOICE_VERSION = "VOICE_VERSION";
	public static final String VOICE_SHELL_LINES = "VOICE_SHELL_LINES";
	public static final String GENERATE_THEME = "GENERATE_THEME";
	public static final String APPROVE_THEME = "APPROVE_THEME";
	public static final String REJECT_THEME = "REJECT_THEME";
	public static final String INVITE_TEACHER = "INVITE_TEACHER";
	public static final String RESEND_TEACHER_INVITE = "RESEND_TEACHER_INVITE";
	public static final String DEACTIVATE_TEACHER = "DEACTIVATE_TEACHER";

	private AuditAction() {
	}

}
