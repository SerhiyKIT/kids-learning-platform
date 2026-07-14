export interface ILesson {
  id?: number;
  title?: string;
  content?: string; // JSON string: { theme, questions[] }
  difficultyLevel?: number | null;
  subject?: { id?: number; title?: string } | null;
}

export const defaultValue: Readonly<ILesson> = {};
