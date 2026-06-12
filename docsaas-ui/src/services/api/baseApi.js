import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { storage } from "@/lib/storage";

export const baseApi = createApi({
  reducerPath: "api",

  baseQuery: fetchBaseQuery({
    baseUrl: import.meta.env.VITE_API_URL,

    prepareHeaders: (headers) => {
      // const token = localStorage.getItem(
      //   "accessToken"
      // );

      const token = storage.getToken();

      if (token) {
        headers.set(
          "Authorization",
          `Bearer ${token}`
        );
      }

      return headers;
    },
  }),

  endpoints: () => ({}),
});