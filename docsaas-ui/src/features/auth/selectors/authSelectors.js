export const selectAuth = (state) =>
  state.auth;

export const selectUser = (state) =>
  state.auth.user;

export const selectIsAuthenticated = (state) =>
  state.auth.isAuthenticated;

export const selectAccessToken = (state) =>
  state.auth.accessToken;

export const selectProvider = (state) =>
  state.auth.provider;