import { createBrowserRouter, Navigate } from "react-router-dom";

import AuthLayout from "../../layouts/AuthLayout";
import MainLayout from "../../layouts/MainLayout";
import PublicLayout from "../../layouts/PublicLayout";

import LoginPage from "../../features/auth/pages/LoginPage";
import RegisterPage from "../../features/auth/pages/RegisterPage";
import ForgotPasswordPage from "../../features/auth/pages/ForgotPasswordPage";
import ResetPasswordPage from "../../features/auth/pages/ResetPasswordPage";
import ProtectedRoute from "../../components/common/ProtectedRoute";
import PublicRoute from "../../components/common/PublicRoute";
import ErrorPage from "../../components/common/ErrorPage";
import NotFoundPage from "../../components/common/NotFoundPage";

import CreditsPage from "../../features/pages/CreditsPage";
import JobsPage from "../../features/pages/JobsPage";
import SettingsPage from "../../features/pages/SettingsPage";
import HomePage from "../../features/home/pages/HomePage";
import ToolPage from "../../features/tools/pages/ToolPage";

const router = createBrowserRouter([
  // Public product pages: no login required (FR-001)
  {
    element: <PublicLayout />,
    errorElement: <ErrorPage />,
    children: [
      {
        path: "/",
        element: <HomePage />,
      },
      {
        // The dashboard was removed; keep old links working
        path: "/dashboard",
        element: <Navigate to="/" replace />,
      },
      {
        path: "/tools/:toolId",
        element: <ToolPage />,
      },
      {
        path: "*",
        element: <NotFoundPage />,
      },
    ],
  },

  {
    element: <AuthLayout />,
    children: [

      {
        path: "/login",
        element: (<PublicRoute>
          <LoginPage />
          </PublicRoute>),
      },
      {
        path: "/register",
        element: (<PublicRoute>
          <RegisterPage />
          </PublicRoute>),
      },
      {
        path: "/forgot-password",
        element: (
          <PublicRoute>
            <ForgotPasswordPage />
          </PublicRoute>
        ),
      },
      {
        // Not behind PublicRoute: the emailed link must work even in a browser that is logged in
        path: "/reset-password",
        element: <ResetPasswordPage />,
      },
    ],
  },

 // Phase 2 workspace (authenticated)
 {
  element: (
    <ProtectedRoute>
      <MainLayout />
    </ProtectedRoute>
  ),
  errorElement: <ErrorPage />,
  children: [
    {
      path: "/credits",
      element: <CreditsPage />,
    },
    {
      path: "/jobs",
      element: <JobsPage />,
    },
    {
      path: "/settings",
      element: <SettingsPage />,
    },
  ],
},
  // (the 404 route lives inside the public layout above)


]);

export default router;
