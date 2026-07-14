import { ReducersMapObject } from '@reduxjs/toolkit';
import { loadingBarReducer as loadingBar } from 'react-redux-loading-bar';

import administration from 'app/modules/administration/administration.reducer';
import locale from './locale';
import authentication from './authentication';
import applicationProfile from './application-profile';

import userManagement from './user-management';
import lesson from 'app/entities/lesson/lesson.reducer';
import progress from 'app/entities/progress/progress.reducer';
import subject from 'app/entities/subject/subject.reducer';
import ai from 'app/entities/ai/ai.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const rootReducer: ReducersMapObject = {
  authentication,
  locale,
  applicationProfile,
  administration,
  userManagement,
  lesson,
  progress,
  subject,
  ai,
  loadingBar,
  /* jhipster-needle-add-reducer-combine - JHipster will add reducer here */
};

export default rootReducer;
