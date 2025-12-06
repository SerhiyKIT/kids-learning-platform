export interface IParent {
  id?: number;
  firstName?: string;
  email?: string;
  isPremium?: boolean | null;
}

export const defaultValue: Readonly<IParent> = {
  isPremium: false,
};
