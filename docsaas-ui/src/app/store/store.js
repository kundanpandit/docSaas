import { configureStore } from "@reduxjs/toolkit";
import authReducer from "@/features/auth/slice/authSlice";
import themeReducer from "@/features/theme/slice/themeSlice";

import { baseApi } from "@/services/api/baseApi";

export const store = configureStore({
  reducer: {
    auth: authReducer,
    theme: themeReducer,
    [baseApi.reducerPath]: baseApi.reducer,
  },

  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(baseApi.middleware),
});