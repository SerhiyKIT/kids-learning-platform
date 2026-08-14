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
