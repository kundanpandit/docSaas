import { useEffect } from "react";
import { useDispatch } from "react-redux";

import { storage } from "@/lib/storage";
import { setCredentials } from "@/features/auth/slice/authSlice";
import { AUTH_PROVIDER } from "@/constants/authProvider";

const AuthInitializer = ({ children }) => {
  const dispatch = useDispatch();

  useEffect(() => {
    const token = storage.getToken();
    const user = storage.getUser();

    if (token && user) {
      dispatch(
        setCredentials({
          user,
          accessToken: token,
          provider: AUTH_PROVIDER.LOCAL,
        })
      );
    }
  }, [dispatch]);

  return children;
};

export default AuthInitializer;