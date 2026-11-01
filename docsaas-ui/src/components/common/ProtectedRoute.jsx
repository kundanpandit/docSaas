import { useSelector } from "react-redux";
import { Navigate, useLocation } from "react-router-dom";

import { selectIsAuthenticated } from "@/features/auth/selectors/authSelectors";

/** Account pages: send guests to login, then bring them back here. */
const ProtectedRoute = ({ children }) => {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
};

export default ProtectedRoute;
