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

/** GET /api/admin/lessons — mirrors ua.kidlearn.lessons.AdminLessonEntry. */
export interface AdminLessonEntry {
  id: string;
  moduleCode: string;
  title: string;
  versionCount: number;
  latestVersion: { id: string; versionNo: number; status: LessonVersionStatus } | null;
  publishedVersionId: string | null;
}

/** POST /api/admin/lessons response — mirrors ua.kidlearn.lessons.LessonResponse. */
export interface LessonCreated {
  id: string;
  moduleCode: string;
  title: string;
}

/** POST /api/admin/lessons/{id}/generate body — mirrors ua.kidlearn.aipipeline.GenerationRequest. */
export interface GenerationRequest {
  topic: string;
  patternIds: string[];
  ageBand: string;
  title?: string;
  learningGoal?: string;
}

/** One validation problem — mirrors ua.kidlearn.scenario.ScenarioProblem. */
export interface ScenarioProblem {
  path: string;
  message: string;
}

/** POST /api/admin/lessons/{id}/generate response — mirrors GenerationOutcome. */
export interface GenerationOutcome {
  versionId: string;
  status: LessonVersionStatus;
  attempts: number;
  problems: ScenarioProblem[];
}

/** POST /api/admin/lesson-versions/{id}/voice response — mirrors VoicingResult. */
export interface VoicingResult {
  linesTotal: number;
  synthesized: number;
  cached: number;
}

/** GET /api/admin/audit — mirrors ua.kidlearn.audit.AuditEntryResponse (no PII, only ids). */
export interface AuditEntry {
  id: string;
  actorId: string | null;
  actorRole: string;
  action: string;
  targetType: string;
  targetId: string | null;
  clientIp: string | null;
  createdAt: string;
}

// ---- Display vocabularies -------------------------------------------------------------------
// The value lists mirror docs/lesson-scenario.schema.json (topic, age_band enums), which the
// backend enforces on every generation request; labels and pattern names come from
// docs/Вимоги_модуль_Безпека.md §5–6.

export const TOPIC_LABELS: Record<string, string> = {
  road_safety: "ПДР",
  emergency_numbers: "Номери екстрених служб",
};

export const AGE_BAND_LABELS: Record<string, string> = {
  age_3_4: "3–4 роки",
  age_5_6: "5–6 років",
};

export interface PatternOption {
  id: string;
  topic: keyof typeof TOPIC_LABELS;
  name: string;
}

export const PATTERNS: PatternOption[] = [
  { id: "1.1", topic: "road_safety", name: "Стоп біля дороги" },
  { id: "1.2", topic: "road_safety", name: "Світлофор" },
  { id: "1.3", topic: "road_safety", name: "Зебра" },
  { id: "1.4", topic: "road_safety", name: "Подивись і переконайся" },
  { id: "1.5", topic: "road_safety", name: "За руку з дорослим" },
  { id: "1.6", topic: "road_safety", name: "М'яч на дорозі" },
  { id: "1.7", topic: "road_safety", name: "Сортування: безпечно/небезпечно біля дороги" },
  { id: "2.1", topic: "emergency_numbers", name: "Впізнай службу" },
  { id: "2.2", topic: "emergency_numbers", name: "Який номер у якій ситуації" },
  { id: "2.3", topic: "emergency_numbers", name: "Єдиний номер 112" },
  { id: "2.4", topic: "emergency_numbers", name: "Що сказати" },
  { id: "2.5", topic: "emergency_numbers", name: "Коли дзвонити, а коли ні" },
  { id: "2.6", topic: "emergency_numbers", name: "Спершу — дорослий" },
];

export const MAX_PATTERNS_PER_LESSON = 2;

export const VERSION_STATUS_LABELS: Record<LessonVersionStatus, { label: string; tone: "ok" | "waiting" | "bad" | "neutral" }> = {
  draft: { label: "Чернетка", tone: "neutral" },
  auto_validated: { label: "На перевірці", tone: "waiting" },
  rejected_auto: { label: "Відхилено валідатором", tone: "bad" },
  approved: { label: "Схвалено", tone: "ok" },
  published: { label: "Опубліковано", tone: "ok" },
  archived: { label: "Відхилено модератором", tone: "bad" },
};

export const AUDIT_ACTION_LABELS: Record<string, string> = {
  VIEW_CHILD_HISTORY: "Перегляд історії дитини",
  VIEW_GROUP_RESULTS: "Перегляд результатів групи",
  DELETE_CHILD: "Видалення дитини",
  GENERATE_VERSION: "Генерація версії",
  APPROVE_VERSION: "Схвалення версії",
  REJECT_VERSION: "Відхилення версії",
  PUBLISH_VERSION: "Публікація версії",
  VOICE_VERSION: "Озвучення версії",
  VOICE_SHELL_LINES: "Озвучення реплік дитячого режиму",
  GENERATE_THEME: "Генерація теми",
  APPROVE_THEME: "Схвалення теми",
  REJECT_THEME: "Відхилення теми",
  INVITE_TEACHER: "Запрошення вчителя",
  RESEND_TEACHER_INVITE: "Повторне запрошення вчителя",
  DEACTIVATE_TEACHER: "Деактивація вчителя",
};

export const AUDIT_TARGET_LABELS: Record<string, string> = {
  child: "Дитина",
  group: "Група",
  lesson: "Урок",
  lesson_version: "Версія уроку",
  user: "Користувач",
  kid_shell_lines: "Репліки дитячого режиму",
  theme: "Тема оформлення",
};

export const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Адміністратор",
  TEACHER: "Вчитель",
  PARENT: "Батьки",
};

/** Moderation tab that lists a version in `status` (see the tabs in app/moderation/page.tsx). */
export function moderationTab(status: LessonVersionStatus): "pending" | "approved" | "published" | "rejected" {
  switch (status) {
    case "approved":
      return "approved";
    case "published":
      return "published";
    case "rejected_auto":
    case "archived":
      return "rejected";
    default:
      return "pending";
  }
}

/** Deep link that opens one version in /moderation, on the tab that lists it. */
export function moderationHref(versionId: string, status: LessonVersionStatus): string {
  return `/moderation?status=${moderationTab(status)}&version=${encodeURIComponent(versionId)}`;
}

// ---- Teachers ---------------------------------------------------------------------------------

export type TeacherStatus = "invited" | "active" | "deactivated";

/** GET /api/admin/teachers — mirrors ua.kidlearn.admin.TeacherEntry (no child/parent data). */
export interface TeacherEntry {
  id: string;
  email: string;
  displayName: string;
  status: TeacherStatus;
  groupCount: number;
  createdAt: string;
}

/** POST /api/admin/teachers body — mirrors InviteTeacherRequest. The admin never sets a password. */
export interface InviteTeacherRequest {
  email: string;
  displayName: string;
}

/** POST /api/admin/teachers response — mirrors InvitedTeacherResponse. */
export interface InvitedTeacher {
  id: string;
  email: string;
  displayName: string;
  status: TeacherStatus;
}

export const TEACHER_STATUS_LABELS: Record<TeacherStatus, { label: string; tone: "ok" | "waiting" | "neutral" }> = {
  invited: { label: "Запрошено", tone: "waiting" },
  active: { label: "Активний", tone: "ok" },
  deactivated: { label: "Деактивовано", tone: "neutral" },
};
