import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Parent from './parent';
import ParentDetail from './parent-detail';
import ParentUpdate from './parent-update';
import ParentDeleteDialog from './parent-delete-dialog';

const ParentRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Parent />} />
    <Route path="new" element={<ParentUpdate />} />
    <Route path=":id">
      <Route index element={<ParentDetail />} />
      <Route path="edit" element={<ParentUpdate />} />
      <Route path="delete" element={<ParentDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default ParentRoutes;
