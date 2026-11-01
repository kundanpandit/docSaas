import { createSlice } from "@reduxjs/toolkit";
import { AUTH_PROVIDER } from "@/constants/authProvider";
import { storage } from "@/lib/storage";

/** Reads the JWT expiry without verifying it; the backend remains the authority. */
const isTokenExpired = (token) => {
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return !payload.exp || payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};

/**
 * Restore the session synchronously so protected routes don't redirect
 * to /login on a hard refresh before the session is restored.
 */
const loadInitialState = () => {
  const token = storage.getToken();
  const user = storage.getUser();

  if (token && user && !isTokenExpired(token)) {
    return {
      user,
      accessToken: token,
      provider: AUTH_PROVIDER.LOCAL,
      isAuthenticated: true,
    };
  }

  storage.removeToken();
  storage.removeUser();

  return {
    user: null,
    accessToken: null,
    provider: null,
    isAuthenticated: false,
  };
};

const authSlice = createSlice({
  name: "auth",

  initialState: loadInitialState,

  reducers: {
    setCredentials: (state, action) => {
    state.user = action.payload.user;
    state.accessToken = action.payload.accessToken;
    state.provider = action.payload.provider;
    state.isAuthenticated = true;
},

    // Profile edits: keep the session, refresh the displayed user
    updateUser: (state, action) => {
      state.user = { ...state.user, ...action.payload };
    },

    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.provider = null;
      state.isAuthenticated = false;
    },
  },
});

export const { setCredentials, updateUser, logout } = authSlice.actions;

export default authSlice.reducer;
