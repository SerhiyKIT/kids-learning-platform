import { IParent } from 'app/shared/model/parent.model';

export interface IStudent {
  id?: number;
  nickname?: string;
  age?: number | null;
  avatarStyle?: string | null;
  parent?: IParent | null;
}

export const defaultValue: Readonly<IStudent> = {};
