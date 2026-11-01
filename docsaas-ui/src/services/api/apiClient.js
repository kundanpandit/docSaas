import axios from "axios";
import { storage } from "@/lib/storage";

/**
 * Axios instance for requests RTK Query's fetch cannot do well:
 * upload progress and binary (blob) downloads.
 */
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = storage.getToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

/** Extracts the user-safe message from an API error (JSON or blob body). */
export const getErrorMessage = async (error, fallback = "Something went wrong. Please try again.") => {
  const data = error?.response?.data;

  if (data instanceof Blob) {
    try {
      return JSON.parse(await data.text())?.message || fallback;
    } catch {
      return fallback;
    }
  }

  if (data?.message) {
    return data.message;
  }

  if (error?.code === "ERR_NETWORK") {
    return "Cannot reach the server. Check your connection and try again.";
  }

  return fallback;
};
