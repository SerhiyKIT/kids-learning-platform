import React from 'react';
import { Route } from 'react-router';

import Loadable from 'react-loadable';

import StudentDashboard from 'app/modules/dashboard/student-dashboard';
import ParentDashboard from 'app/modules/dashboard/parent-dashboard';
import TeacherDashboard from 'app/modules/dashboard/teacher-dashboard';
import LessonPlayer from 'app/modules/learning/lesson-player';
import LoginRedirect from 'app/modules/login/login-redirect';
import Logout from 'app/modules/login/logout';
import Home from 'app/modules/home/home';
import DataDeletionPage from 'app/modules/account/data-deletion/data-deletion';
import EntitiesRoutes from 'app/entities/routes';
import PrivateRoute from 'app/shared/auth/private-route';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';
import PageNotFound from 'app/shared/error/page-not-found';
import { AUTHORITIES } from 'app/config/constants';

const loading = <div>loading ...</div>;

const Admin = Loadable({
  loader: () => import(/* webpackChunkName: "administration" */ 'app/modules/administration'),
  loading: () => loading,
});

const AppRoutes = () => {
  return (
    <div className="view-routes">
      <ErrorBoundaryRoutes>
        <Route index element={<Home />} />
        <Route path="logout" element={<Logout />} />
        <Route path="sign-in" element={<LoginRedirect />} />

        <Route
          path="admin/*"
          element={
            <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
              <Admin />
            </PrivateRoute>
          }
        />

        {/* Portals — protected, require login */}
        <Route
          path="student-dashboard"
          element={
            <PrivateRoute hasAnyAuthorities={[AUTHORITIES.USER]}>
              <StudentDashboard />
            </PrivateRoute>
          }
        />
        <Route
          path="parent-dashboard"
          element={
            <PrivateRoute hasAnyAuthorities={[AUTHORITIES.USER]}>
              <ParentDashboard />
            </PrivateRoute>
          }
        />
        <Route
          path="teacher-dashboard"
          element={
            <PrivateRoute hasAnyAuthorities={[AUTHORITIES.USER, AUTHORITIES.ADMIN]}>
              <TeacherDashboard />
            </PrivateRoute>
          }
        />

        {/* Lesson route */}
        <Route
          path="lesson/:id"
          element={
            <PrivateRoute hasAnyAuthorities={[AUTHORITIES.USER]}>
              <LessonPlayer />
            </PrivateRoute>
          }
        />

        {/* COPPA — data deletion (public, user confirms via email) */}
        <Route path="account/data-deletion" element={<DataDeletionPage />} />

        {/* Entity CRUD (admin/teacher use) */}
        <Route
          path="*"
          element={
            <PrivateRoute hasAnyAuthorities={[AUTHORITIES.USER]}>
              <EntitiesRoutes />
            </PrivateRoute>
          }
        />
        <Route path="*" element={<PageNotFound />} />
      </ErrorBoundaryRoutes>
    </div>
  );
};

export default AppRoutes;
