export interface IProgress {
  id?: number;
  studentId?: number;
  score?: number | null;
  status?: 'STARTED' | 'COMPLETED' | null;
  completedAt?: string | null;
  lesson?: { id?: number; title?: string } | null;
}

export const defaultValue: Readonly<IProgress> = {};
