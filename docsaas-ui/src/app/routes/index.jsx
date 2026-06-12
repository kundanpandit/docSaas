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

const router = createBrowserRouter([
     {
    path: "/",
    element: <HomePage />,
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
    element: <MainLayout />,
    children: [
      {
        path: "/dashboard",
        element: (<ProtectedRoute>
          <DashboardPage />
          </ProtectedRoute>),
      },
    ],
  }, 
  {
  path: "*",
  element: <NotFoundPage />,
}

  
]);

export default router;