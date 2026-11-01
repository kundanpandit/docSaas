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
export const passwordApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Optional auth features the server has turned on (password reset needs email to be configured)
    getAuthOptions: builder.query({
      query: () => "/auth/options",
      transformResponse: (response) => response.data,
    }),
    forgotPassword: builder.mutation({
      query: (email) => ({ url: "/auth/forgot-password", method: "POST", body: { email } }),
    }),
    verifyResetToken: builder.query({
      query: (token) => ({ url: "/auth/reset-password/verify", method: "POST", body: { token } }),
      transformResponse: (response) => response.data,
    }),
    resetPassword: builder.mutation({
      query: (body) => ({ url: "/auth/reset-password", method: "POST", body }),
    }),
  }),
});

export const { useGetAuthOptionsQuery, useForgotPasswordMutation, useVerifyResetTokenQuery, useResetPasswordMutation } =
  passwordApi;
