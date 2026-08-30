import axios from "axios";

interface ErrorResponse {
  message?: string;
  error?: string;
}

export const getErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.message || error.response?.data?.error || fallback;
  }

  return error instanceof Error && error.message ? error.message : fallback;
};
