import { baseApi } from "@/services/api/baseApi";

export const accountApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    updateProfile: builder.mutation({
      query: (body) => ({ url: "/v1/users/me", method: "PATCH", body }),
      transformResponse: (response) => response.data,
    }),

    changePassword: builder.mutation({
      query: (body) => ({ url: "/v1/users/me/password", method: "POST", body }),
    }),

    deleteAccount: builder.mutation({
      query: (body) => ({ url: "/v1/users/me", method: "DELETE", body }),
    }),

    // Balance, monthly usage, per-tool costs and recent ledger entries
    getCredits: builder.query({
      query: () => "/v1/credits",
      transformResponse: (response) => response.data,
      providesTags: ["Credits"],
    }),
  }),
});

export const {
  useUpdateProfileMutation,
  useChangePasswordMutation,
  useDeleteAccountMutation,
  useGetCreditsQuery,
} = accountApi;

/** RTK Query error -> user-safe message from the API envelope. */
export const apiErrorMessage = (error, fallback = "Something went wrong. Please try again.") =>
  error?.data?.message || fallback;
