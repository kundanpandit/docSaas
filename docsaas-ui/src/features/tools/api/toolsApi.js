import { baseApi } from "@/services/api/baseApi";
import { apiClient } from "@/services/api/apiClient";

export const toolsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getJob: builder.query({
      query: (jobId) => `/v1/jobs/${jobId}`,
      transformResponse: (response) => response.data,
    }),

    getMyJobs: builder.query({
      query: () => "/v1/jobs",
      transformResponse: (response) => response.data,
    }),

    // Which tools can run right now (an engine may not be installed) + OCR languages
    getToolStatus: builder.query({
      query: () => "/v1/tools",
      transformResponse: (response) => response.data,
    }),

    askDocument: builder.mutation({
      query: ({ jobId, question, history }) => ({
        url: `/v1/jobs/${jobId}/chat`,
        method: "POST",
        body: { question, history },
      }),
      transformResponse: (response) => response.data,
    }),
  }),
});

export const { useGetJobQuery, useGetMyJobsQuery, useGetToolStatusQuery, useAskDocumentMutation } = toolsApi;

/** Fetches a text result (OCR text, AI summary) for display. */
export const fetchJobText = async (jobId) => {
  const response = await apiClient.get(`/v1/jobs/${jobId}/download`, { responseType: "text" });
  return response.data;
};

/**
 * Uploads files with tool options and starts a job.
 * Uses axios for upload progress. Resolves to the created job.
 */
export const startToolJob = async ({ toolId, files, options, data, onProgress }) => {
  const form = new FormData();

  files.forEach((file) => form.append("files", file));
  // structured input (the PDF editor's changes) goes as a JSON file part
  if (data) form.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }), "data.json");

  Object.entries(options).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      form.append(key, value);
    }
  });

  const response = await apiClient.post(`/v1/tools/${toolId}`, form, {
    onUploadProgress: (event) => {
      if (event.total) {
        onProgress?.(Math.round((event.loaded * 100) / event.total));
      }
    },
  });

  return response.data.data;
};

/** Downloads a job result and saves it with the server-provided filename. */
export const downloadJobResult = async (job) => {
  const response = await apiClient.get(`/v1/jobs/${job.id}/download`, {
    responseType: "blob",
  });

  const url = URL.createObjectURL(response.data);
  const link = document.createElement("a");

  link.href = url;
  link.download = job.output?.fileName || "result";
  document.body.appendChild(link);
  link.click();
  link.remove();

  setTimeout(() => URL.revokeObjectURL(url), 1000);
};
