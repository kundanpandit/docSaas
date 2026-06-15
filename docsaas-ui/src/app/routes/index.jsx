import { createBrowserRouter } from "react-router-dom";

import AuthLayout from "../../layouts/AuthLayout";
import MainLayout from "../../layouts/MainLayout";

import LoginPage from "../../features/auth/pages/LoginPage";
import RegisterPage from "../../features/auth/pages/RegisterPage";
import DashboardPage from "../../features/dashboard/pages/DashboardPage";
import HomePage from "../../features/home/pages/HomePage";
import ProtectedRoute from "../../components/common/ProtectedRoute";
import PublicRoute from "../../components/common/PublicRoute";
import ErrorPage from "../../components/common/ErrorPage";
import NotFoundPage from "../../components/common/NotFoundPage";

import DocumentsPage from "../../features/pages/DocumentPage";
import UploadPage from "../../features/pages/UploadPage";
import JobsPage from "../../features/pages/JobsPage";
import SettingsPage from "../../features/pages/SettingsPage";
import HomePage1 from "../../features/home/pages/HomePage1";

const router = createBrowserRouter([
     {
  path: "/",
  element: <HomePage1 />,
  errorElement: <ErrorPage />,
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
    ],
  },

 {
  element: (
    <ProtectedRoute>
      <MainLayout />
    </ProtectedRoute>
  ),
  children: [
    {
      path: "/dashboard",
      element: <DashboardPage />,
    },
    {
      path: "/documents",
      element: <DocumentsPage />,
    },
    {
      path: "/upload",
      element: <UploadPage />,
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
  {
  path: "*",
  element: <NotFoundPage />,
}

  
]);

export default router;