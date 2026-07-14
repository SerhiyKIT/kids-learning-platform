import axios from 'axios';
import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { EntityState, IQueryParams, createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { IProgress, defaultValue } from 'app/shared/model/progress.model';

const initialState: EntityState<IProgress> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const apiUrl = '/services/learningservice/api/progresses';

export const getEntities = createAsyncThunk(
  'progress/fetch_entity_list',
  async ({ page, size, sort }: IQueryParams) => {
    const requestUrl = `${apiUrl}?${sort ? `page=${page}&size=${size}&sort=${sort}&` : ''}cacheBuster=${new Date().getTime()}`;
    return axios.get<IProgress[]>(requestUrl);
  },
  { serializeError: serializeAxiosError },
);

export const getProgressForStudent = createAsyncThunk(
  'progress/fetch_by_student',
  async (studentId: number) => {
    return axios.get<IProgress[]>(`${apiUrl}?studentId.equals=${studentId}&cacheBuster=${new Date().getTime()}`);
  },
  { serializeError: serializeAxiosError },
);

export const saveProgress = createAsyncThunk(
  'progress/save',
  async (entity: IProgress) => {
    return axios.post<IProgress>(apiUrl, entity);
  },
  { serializeError: serializeAxiosError },
);

export const ProgressSlice = createEntitySlice({
  name: 'progress',
  initialState,
  extraReducers(builder) {
    builder
      .addMatcher(isFulfilled(getEntities, getProgressForStudent), (state, action) => {
        const { data, headers } = action.payload;
        return {
          ...state,
          loading: false,
          entities: data,
          totalItems: parseInt(headers?.['x-total-count'], 10) || data.length,
        };
      })
      .addMatcher(isFulfilled(saveProgress), (state, action) => {
        state.updating = false;
        state.updateSuccess = true;
        state.entity = action.payload.data;
      })
      .addMatcher(isPending(getEntities, getProgressForStudent), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      })
      .addMatcher(isPending(saveProgress), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.updating = true;
      });
  },
});

export const { reset } = ProgressSlice.actions;
export default ProgressSlice.reducer;
