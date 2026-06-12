import { baseApi } from "@/services/api/baseApi";

export const authApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    login: builder.mutation({
      query: (credentials) => ({
        url: "/auth/login",
        method: "POST",
        body: credentials,
      }),
    }),

    register: builder.mutation({
      query: (userData) => ({
        url: "/auth/register",
        method: "POST",
        body: userData,
      }),
    }),

    googleRegister: builder.mutation({
  query: (token) => ({
    url: "/auth/google",
    method: "POST",
    body: {
      token,
    },
  }),
}),

googleLogin: builder.mutation({
  query: (token) => ({
    url: "/auth/google-login",
    method: "POST",
    body: {
      token,
    },
  }),
}),
  }),
});

export const {
  useLoginMutation,
  useRegisterMutation,
  useGoogleRegisterMutation,
  useGoogleLoginMutation,
} = authApi;