import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { serializeAxiosError } from 'app/shared/reducers/reducer.utils';

const apiUrl = '/services/aicontentservice/api/generate-hint';

interface AiState {
  hint: string | null;
  loading: boolean;
  error: string | null;
}

const initialState: AiState = {
  hint: null,
  loading: false,
  error: null,
};

export const generateHint = createAsyncThunk(
  'ai/generate_hint',
  async (payload: { question: string; subject?: string }) => {
    return axios.post<{ hint: string }>(apiUrl, payload);
  },
  { serializeError: serializeAxiosError },
);

export const AiSlice = createSlice({
  name: 'ai',
  initialState,
  reducers: {
    clearHint(state) {
      state.hint = null;
      state.error = null;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(generateHint.pending, state => {
        state.loading = true;
        state.hint = null;
        state.error = null;
      })
      .addCase(generateHint.fulfilled, (state, action) => {
        state.loading = false;
        state.hint = action.payload.data.hint;
      })
      .addCase(generateHint.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message ?? 'Помилка отримання підказки';
      });
  },
});

export const { clearHint } = AiSlice.actions;
export default AiSlice.reducer;
