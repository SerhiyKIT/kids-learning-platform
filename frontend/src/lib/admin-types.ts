// Admin moderation DTOs — mirror ua.kidlearn.lessons.PendingReviewEntry and
// LessonVersionDetailResponse. Kept apart from api-types.ts so the moderation screen can be
// dropped in without touching the file the other screens share.

import type { LessonScenario } from "./scenario-types";

export type LessonVersionStatus =
  | "draft"
  | "auto_validated"
  | "rejected_auto"
  | "approved"
  | "published"
  | "archived";

/** GET /api/admin/lesson-versions?status=auto_validated */
export interface PendingReviewEntry {
  versionId: string;
  lessonId: string;
  title: string;
  versionNo: number;
  status: LessonVersionStatus;
  generatedBy: string;
}

/** GET /api/admin/lesson-versions/{id} */
export interface LessonVersionDetail {
  id: string;
  lessonId: string;
  versionNo: number;
  scenario: LessonScenario;
  generatedBy: string;
  aiModel: string | null;
  status: LessonVersionStatus;
  approvedBy: string | null;
  createdAt: string;
}
