package ua.kidlearn.audit;

/** Recorded audit_log.action values: child-data access plus the content pipeline (generate → moderate → voice). */
public final class AuditAction {

	public static final String VIEW_CHILD_HISTORY = "VIEW_CHILD_HISTORY";
	public static final String VIEW_GROUP_RESULTS = "VIEW_GROUP_RESULTS";
	public static final String DELETE_CHILD = "DELETE_CHILD";
	public static final String APPROVE_VERSION = "APPROVE_VERSION";
	public static final String REJECT_VERSION = "REJECT_VERSION";
	public static final String PUBLISH_VERSION = "PUBLISH_VERSION";
	public static final String GENERATE_VERSION = "GENERATE_VERSION";
	public static final String VOICE_VERSION = "VOICE_VERSION";

	private AuditAction() {
	}

}
