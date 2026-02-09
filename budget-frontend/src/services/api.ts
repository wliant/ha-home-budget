import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

/**
 * API client service for communicating with the backend.
 *
 * This service:
 * - Configures axios with the backend base URL
 * - Handles authentication via X-Hass-User header (added by Home Assistant nginx)
 * - Provides typed request/response handling
 * - Centralizes error handling
 */

// Resolve API base URL differently for server vs browser.
// - Server: prefer INTERNAL_API_URL (container-only), then NEXT_PUBLIC_API_URL
// - Browser: prefer NEXT_PUBLIC_API_URL, else same-origin (empty base)
const isServer = typeof window === 'undefined';
const API_BASE_URL = isServer
  ? process.env.INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
  : process.env.NEXT_PUBLIC_API_URL || '';

// Create axios instance with default configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000, // 10 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Include cookies and credentials
});

// Request interceptor - can be used to add auth tokens, logging, etc.
apiClient.interceptors.request.use(
  (config) => {
    // In production, X-Hass-User header will be added by nginx
    // In development, you can manually add it for testing
    if (process.env.NODE_ENV === 'development') {
      const testUser = process.env.NEXT_PUBLIC_TEST_USER;
      if (testUser && config.headers) {
        config.headers['X-Hass-User'] = testUser;
      }
    }

    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      if (config.headers) {
        if (typeof (config.headers as any).delete === 'function') {
          (config.headers as any).delete('Content-Type');
        }
        delete (config.headers as any)['Content-Type'];
        delete (config.headers as any)['content-type'];
      }
    }

    console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('[API] Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor - centralized error handling
apiClient.interceptors.response.use(
  (response) => {
    console.log(`[API] Response from ${response.config.url}:`, response.status);
    return response;
  },
  (error) => {
    if (error.response) {
      // Server responded with error status
      console.error(
        `[API] Error response from ${error.config?.url}:`,
        error.response.status,
        error.response.data
      );
    } else if (error.request) {
      // Request was made but no response received
      console.error('[API] No response received:', error.message);
    } else {
      // Error setting up the request
      console.error('[API] Request setup error:', error.message);
    }
    return Promise.reject(error);
  }
);

/**
 * Generic API methods
 */
const api = {
  /**
   * GET request
   */
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> => {
    return apiClient.get<T>(url, config);
  },

  /**
   * POST request
   */
  post: <T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> => {
    return apiClient.post<T>(url, data, config);
  },

  /**
   * PUT request
   */
  put: <T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> => {
    return apiClient.put<T>(url, data, config);
  },

  /**
   * PATCH request
   */
  patch: <T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> => {
    return apiClient.patch<T>(url, data, config);
  },

  /**
   * DELETE request
   */
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> => {
    return apiClient.delete<T>(url, config);
  },
};

/**
 * Health check service
 */
export interface HealthResponse {
  status: string;
  service: string;
  version: string;
}

export interface InfoResponse {
  app: string;
  description: string;
  version: string;
  authentication: string;
}

export const healthService = {
  /**
   * Check backend health status
   */
  checkHealth: async (): Promise<HealthResponse> => {
    const response = await api.get<HealthResponse>('/api/health');
    return response.data;
  },

  /**
   * Get backend service info
   */
  getInfo: async (): Promise<InfoResponse> => {
    const response = await api.get<InfoResponse>('/api/info');
    return response.data;
  },
};

// Export the base API client and axios instance for advanced usage
export { apiClient };
export default api;
