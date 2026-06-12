import { createSlice } from "@reduxjs/toolkit";
//import { AUTH_PROVIDER } from "@/constants/authProvider";

const initialState = {
  user: null,
  accessToken: null,
  provider: null,
  isAuthenticated: false,
};

const authSlice = createSlice({
  name: "auth",

  initialState,

  reducers: {
    setCredentials: (state, action) => {
    state.user = action.payload.user;
    state.accessToken = action.payload.accessToken;
    state.provider = action.payload.provider;
    state.isAuthenticated = true;
},

    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.provider = null;
      state.isAuthenticated = false;
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;

export default authSlice.reducer;