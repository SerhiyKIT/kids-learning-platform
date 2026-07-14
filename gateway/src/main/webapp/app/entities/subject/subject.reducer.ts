import axios from 'axios';
import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { EntityState, createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { ISubject, defaultValue } from 'app/shared/model/subject.model';

const initialState: EntityState<ISubject> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const apiUrl = '/services/learningservice/api/subjects';

export const getEntities = createAsyncThunk(
  'subject/fetch_entity_list',
  async () => {
    return axios.get<ISubject[]>(`${apiUrl}?size=50&cacheBuster=${new Date().getTime()}`);
  },
  { serializeError: serializeAxiosError },
);

export const SubjectSlice = createEntitySlice({
  name: 'subject',
  initialState,
  extraReducers(builder) {
    builder
      .addMatcher(isFulfilled(getEntities), (state, action) => {
        const { data } = action.payload;
        return { ...state, loading: false, entities: data };
      })
      .addMatcher(isPending(getEntities), state => {
        state.errorMessage = null;
        state.loading = true;
      });
  },
});

export const { reset } = SubjectSlice.actions;
export default SubjectSlice.reducer;
