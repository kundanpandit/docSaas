import { baseApi } from "@/services/api/baseApi";

export const usageApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    // Today's credits for the caller (guest by IP, or the logged-in user). The arg is only a cache key
    // ("guest" or the user id) so logging in/out refetches instead of showing the other identity's numbers.
    getUsage: builder.query({
      query: () => "/v1/usage",
      transformResponse: (response) => response.data,
      providesTags: ["Usage"],
    }),
  }),
});

export const { useGetUsageQuery } = usageApi;

/** Call after starting or finishing a job so every usage display refreshes. */
export const refreshUsage = (dispatch) => dispatch(baseApi.util.invalidateTags(["Usage", "Credits"]));
