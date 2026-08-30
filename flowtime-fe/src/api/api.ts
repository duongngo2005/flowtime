import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const GOOGLE_LOGIN_URL = `${API_BASE_URL}/oauth2/authorization/google`;

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & {
      _retry?: boolean;
    }) | undefined;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes("/api/auth/refresh") &&
      !originalRequest.url?.includes("/api/auth/logout")
    ) {
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const res = await axios.post<{ accessToken?: string; token?: string }>(
          `${API_BASE_URL}/api/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newAccessToken = res.data.accessToken || res.data.token;
        if (newAccessToken) {
          localStorage.setItem("access_token", newAccessToken);
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          processQueue(null, newAccessToken);
          return api(originalRequest);
        } else {
          throw new Error("Không nhận được access token mới từ phiên đăng nhập.");
        }
      } catch (refreshError) {
        processQueue(refreshError, null);
        localStorage.removeItem("access_token");
        window.location.href = "/";
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export const logout = async () => {
  try {
    await api.post("/api/auth/logout");
  } catch (err) {
    console.warn("Logout request failed", err);
  } finally {
    localStorage.removeItem("access_token");
    window.location.href = "/";
  }
};

export default api;
