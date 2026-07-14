import axios from 'axios';
import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { EntityState, IQueryParams, createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { ILesson, defaultValue } from 'app/shared/model/lesson.model';

const initialState: EntityState<ILesson> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const apiUrl = '/services/learningservice/api/lessons';

export const getEntities = createAsyncThunk(
  'lesson/fetch_entity_list',
  async ({ page, size, sort }: IQueryParams) => {
    const requestUrl = `${apiUrl}?${sort ? `page=${page}&size=${size}&sort=${sort}&` : ''}cacheBuster=${new Date().getTime()}`;
    return axios.get<ILesson[]>(requestUrl);
  },
  { serializeError: serializeAxiosError },
);

export const getEntity = createAsyncThunk(
  'lesson/fetch_entity',
  async (id: string | number) => {
    return axios.get<ILesson>(`${apiUrl}/${id}`);
  },
  { serializeError: serializeAxiosError },
);

export const createEntity = createAsyncThunk(
  'lesson/create_entity',
  async (entity: ILesson) => {
    return axios.post<ILesson>(apiUrl, entity);
  },
  { serializeError: serializeAxiosError },
);

export const LessonSlice = createEntitySlice({
  name: 'lesson',
  initialState,
  extraReducers(builder) {
    builder
      .addCase(getEntity.fulfilled, (state, action) => {
        state.loading = false;
        state.entity = action.payload.data;
      })
      .addMatcher(isFulfilled(getEntities), (state, action) => {
        const { data, headers } = action.payload;
        return {
          ...state,
          loading: false,
          entities: data,
          totalItems: parseInt(headers['x-total-count'], 10) || data.length,
        };
      })
      .addMatcher(isFulfilled(createEntity), (state, action) => {
        state.updating = false;
        state.updateSuccess = true;
        state.entity = action.payload.data;
        state.entities = [...state.entities, action.payload.data];
      })
      .addMatcher(isPending(getEntities, getEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      })
      .addMatcher(isPending(createEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.updating = true;
      });
  },
});

export const { reset } = LessonSlice.actions;
export default LessonSlice.reducer;
