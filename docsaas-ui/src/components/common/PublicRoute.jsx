import { useSelector } from "react-redux";
import { Navigate, useLocation } from "react-router-dom";

import { selectIsAuthenticated } from "@/features/auth/selectors/authSelectors";

/** Login/register pages: a logged-in visitor goes back to where they came from, or to the tools. */
const PublicRoute = ({ children }) => {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const location = useLocation();

  if (isAuthenticated) {
    return <Navigate to={location.state?.from || "/"} replace />;
  }

  return children;
};

export default PublicRoute;
