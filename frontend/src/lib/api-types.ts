// Mirrors backend response/request DTOs under ua.kidlearn.{auth,children,groups,attempts}. Keep
// field names/shapes in sync with those records — this file has no runtime behavior of its own.

export type Role = "PARENT" | "TEACHER" | "ADMIN";

/** GET /api/auth/me */
export interface Me {
  id: string;
  email: string;
  role: Role;
  emailVerified: boolean;
  displayName: string;
}

/** POST /api/dev/register-role body — dev-only, the only way to create a TEACHER/ADMIN login
 * (public /api/auth/register always creates a PARENT). Absent under NODE_ENV === "production". */
export interface DevRegisterRoleRequest {
  email: string;
  password: string;
  displayName: string;
  role: Role;
}

/** POST /api/dev/register-role response */
export interface DevRegisterRoleResponse {
  id: string;
  email: string;
  role: Role;
}

export type ChildStatus = "pending_consent" | "active";

/** GET /api/children, GET /api/children/{id}, POST /api/children */
export interface Child {
  id: string;
  displayName: string;
  birthYear: number;
  status: ChildStatus;
}

export type ParentRelation = "mother" | "father" | "guardian";

/** POST /api/children body */
export interface CreateChildRequest {
  displayName: string;
  birthYear: number;
  relation: ParentRelation;
  avatarId?: string;
}

/** GET /api/children/{childId}/groups, GET /api/groups, POST /api/groups,
 * POST /api/groups/{id}/regenerate-code */
export interface Group {
  id: string;
  name: string;
  joinCode: string;
  isActive: boolean;
}

/** POST /api/groups body */
export interface CreateGroupRequest {
  name: string;
}

/** GET /api/groups/{id}/members — deliberately excludes anything about the child's parent. */
export interface GroupMemberInfo {
  childId: string;
  displayName: string;
  avatarId: string;
}

/** GET /api/catalog/lessons — published lessons only. */
export interface CatalogEntry {
  lessonId: string;
  title: string;
  moduleCode: string;
  currentVersionId: string;
}

/** POST /api/assignments body — exactly one of groupId/childId. */
export interface CreateAssignmentRequest {
  lessonVersionId: string;
  groupId?: string;
  childId?: string;
  availableFrom?: string;
  dueAt?: string;
}

/** POST /api/assignments, GET /api/assignments?groupId={id} */
export interface Assignment {
  id: string;
  lessonVersionId: string;
  groupId: string | null;
  childId: string | null;
  assignedBy: string;
  availableFrom: string | null;
  dueAt: string | null;
}

/** One attempt inside a TeacherResultChild — same result/score shape as HistoryEntry, minus
 * per-scene answers (teachers see outcomes, not the attempt-by-attempt detail parents see). */
export interface TeacherResultAttempt {
  title: string;
  completedAt: string | null;
  result: AttemptResult | null;
  score: number | null;
}

/** GET /api/groups/{groupId}/results — deliberately excludes anything about the child's parent. */
export interface TeacherResultChild {
  childId: string;
  displayName: string;
  attempts: TeacherResultAttempt[];
}

/** One scene answer inside a HistoryEntry */
export interface AnswerEntry {
  sceneKey: string;
  tryNo: number;
  chosenOption: string;
  isCorrect: boolean;
  hintsUsed: number;
}

export type AttemptResult = "completed" | "abandoned";

/** GET /api/children/{childId}/history */
export interface HistoryEntry {
  attemptId: string;
  title: string;
  startedAt: string;
  completedAt: string | null;
  result: AttemptResult | null;
  score: number | null;
  answers: AnswerEntry[];
}

/** GET /api/children/{id}/play-state, PUT …/play-settings, POST …/play-state/wake, and the body of
 * POST /api/attempts/{id}/complete. Child mode only looks at `resting` — the child never sees a clock. */
export interface PlayState {
  resting: boolean;
  restUntil: string | null;
  /** null = no limit */
  sessionLimitMinutes: number | null;
}

/** PUT /api/children/{id}/play-settings body. */
export interface PlaySettingsRequest {
  sessionLimitMinutes: 10 | 15 | 20 | 30 | null;
}

/** GET /api/children/{id}/stickers — no score or grade, by design. */
export interface StickersResponse {
  stickers: Sticker[];
  topicsCompleted: string[];
  /** Assigned lessons without a sticker yet: grey silhouettes in the album. */
  locked: { lessonId: string; topic: string }[];
}

export interface Sticker {
  lessonId: string;
  lessonVersionId: string;
  title: string;
  topic: string;
  earnedAt: string;
  bridge: { key: string; text: string; audioUrl: string | null };
}

/** GET /api/kid/shell-lines — the guide's fixed child-mode lines. */
export interface ShellLine {
  key: string;
  text: string;
  audioUrl?: string;
}

/** GET /api/children/{id}/consents — every consent given for the child, revocations included. */
export interface ConsentEntry {
  type: "account" | "microphone" | "assistant_level2" | "style_personalization";
  grantedAt: string;
  revokedAt: string | null;
}

/** GET /api/me/deletion-preview — what deleting the account does to each child. */
export interface DeletionPreview {
  children: { childId: string; displayName: string; deletedCompletely: boolean }[];
}
