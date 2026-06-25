import Axios, { type AxiosError, type AxiosRequestConfig } from "axios";
import { stringify } from "qs";

import { ensureValidToken, handleSessionExpired, keycloak } from "@/lib/auth";

const API_TIMEOUT = Number(process.env.NEXT_PUBLIC_API_TIMEOUT ?? 30000);

export const AXIOS_INSTANCE = Axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || undefined,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
  timeout: API_TIMEOUT,
  // Repeat-style arrays match Spring's binding (?sort=a&sort=b); skip nulls.
  paramsSerializer: (params) => stringify(params, { arrayFormat: "repeat", skipNulls: true }),
});

AXIOS_INSTANCE.interceptors.request.use(async (config) => {
  if (keycloak.authenticated && !(await ensureValidToken())) {
    throw new Axios.Cancel("Session expired");
  }
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }
  // Let the browser set the multipart boundary on FormData uploads.
  if (config.data instanceof FormData) {
    config.headers.delete("Content-Type");
  }
  return config;
});

AXIOS_INSTANCE.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (globalThis.window === undefined) {
      throw error;
    }
    // Only force logout when there is a session to clear — a 401 on an anonymous request must not
    // loop into login.
    if (error.response?.status === 401 && keycloak.authenticated) {
      handleSessionExpired();
    }
    throw error;
  },
);

/** Orval mutator — every generated hook calls through this so auth/serialization stay in one place. */
export const customInstance = async <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const response = await AXIOS_INSTANCE({ ...config, ...options });
  return response.data;
};

export default customInstance;
