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

/** GET /api/children/{childId}/groups */
export interface Group {
  id: string;
  name: string;
  joinCode: string;
  isActive: boolean;
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
