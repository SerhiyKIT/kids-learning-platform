import { expect, type Browser, type Page } from "@playwright/test";
import { adminPage } from "./admin";
import { api, openLogin } from "./api";
import { linkFromEmail } from "./mailpit";
import { PASSWORD, signIn, uniqueEmail } from "./users";

/**
 * A published, voiced lesson (stub LLM: demo → choice → sorting → control choice) assigned to a
 * fresh teacher's group; returns what a parent needs to join it. Through the admin and teacher
 * APIs — the specs that are about those flows drive the UI instead.
 */
export async function publishedLesson(admin: Page, title: string): Promise<string> {
  const lesson = await api<{ id: string }>(admin, "POST", "/admin/lessons", { moduleCode: "safety", title });
  expect(lesson.status).toBe(201);
  const generated = await api<{ versionId: string; status: string }>(admin, "POST", `/admin/lessons/${lesson.json.id}/generate`, {
    topic: "road_safety",
    patternIds: ["1.6"],
    ageBand: "age_5_6",
    learningGoal: "Безпечно переходити дорогу",
  });
  expect(generated.json.status).toBe("auto_validated");
  const versionId = generated.json.versionId;
  expect((await api(admin, "POST", `/admin/lesson-versions/${versionId}/approve`)).status).toBe(200);
  expect((await api(admin, "POST", `/admin/lesson-versions/${versionId}/voice`)).status).toBe(200);
  expect((await api(admin, "POST", `/admin/lesson-versions/${versionId}/publish`)).status).toBe(200);
  return versionId;
}

export interface TeacherGroup {
  teacher: Page;
  close: () => Promise<void>;
  groupId: string;
  joinCode: string;
  lessonTitle: string;
}

/**
 * A teacher (invited by the admin, password set from the invite email) with a group that has a
 * freshly published lesson assigned.
 */
export async function teacherGroupWithLesson(browser: Browser): Promise<TeacherGroup> {
  const { page: admin, context: adminContext } = await adminPage(browser);
  const lessonTitle = `Урок ${Date.now()}`;
  const versionId = await publishedLesson(admin, lessonTitle);
  const email = uniqueEmail("teacher");
  expect((await api(admin, "POST", "/admin/teachers", { email, displayName: "Вчителька" })).status).toBe(201);
  await adminContext.close();

  const context = await browser.newContext();
  const teacher = await context.newPage();
  await openLogin(teacher);
  const link = await linkFromEmail(email, "/accept-invite");
  expect((await api(teacher, "POST", "/auth/accept-invite", { token: link.searchParams.get("token"), password: PASSWORD })).status).toBe(200);
  await signIn(teacher, email);
  const group = await api<{ id: string; joinCode: string }>(teacher, "POST", "/groups", { name: "Зайчики" });
  expect(group.status).toBe(201);
  expect((await api(teacher, "POST", "/assignments", { lessonVersionId: versionId, groupId: group.json.id })).status).toBe(201);
  return { teacher, close: () => context.close(), groupId: group.json.id, joinCode: group.json.joinCode, lessonTitle };
}
