// API Client configuration
import * as SecureStore from 'expo-secure-store';
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { Platform } from 'react-native';

// Configure your backend URL here
const API_BASE_URL = __DEV__
  ? 'http://localhost:8080/api/v1' // Development backend
  : 'https://your-production-api.com/api/v1'; // Production backend

// Token storage keys
const TOKEN_KEY = 'authToken';

// Check if we're running on web
const isWeb = Platform.OS === 'web';

// Helper to get auth token (works on web and native)
export const getAuthToken = async (): Promise<string | null> => {
  try {
    if (isWeb) {
      // Use localStorage for web
      return localStorage.getItem(TOKEN_KEY);
    } else {
      // Use SecureStore for native platforms
      return await SecureStore.getItemAsync(TOKEN_KEY);
    }
  } catch (error) {
    console.error('Error getting auth token:', error);
    return null;
  }
};

// Helper to set auth token (works on web and native)
export const setAuthToken = async (token: string): Promise<void> => {
  try {
    if (isWeb) {
      // Use localStorage for web
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      // Use SecureStore for native platforms
      await SecureStore.setItemAsync(TOKEN_KEY, token);
    }
  } catch (error) {
    console.error('Error setting auth token:', error);
  }
};

// Helper to remove auth token (works on web and native)
export const removeAuthToken = async (): Promise<void> => {
  try {
    if (isWeb) {
      // Use localStorage for web
      localStorage.removeItem(TOKEN_KEY);
    } else {
      // Use SecureStore for native platforms
      await SecureStore.deleteItemAsync(TOKEN_KEY);
    }
  } catch (error) {
    console.error('Error removing auth token:', error);
  }
};

// Create axios instance
const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
axiosInstance.interceptors.request.use(
  async (config) => {
    const token = await getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      await removeAuthToken();
      // You can navigate to login here if needed
    }
    return Promise.reject(error);
  }
);

// API client methods
export const apiClient = {
  get: async <T>(endpoint: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await axiosInstance.get<T>(endpoint, config);
    return response.data;
  },

  post: async <T>(endpoint: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
    const response = await axiosInstance.post<T>(endpoint, data, config);
    return response.data;
  },

  patch: async <T>(endpoint: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
    const response = await axiosInstance.patch<T>(endpoint, data, config);
    return response.data;
  },

  delete: async <T>(endpoint: string, config?: AxiosRequestConfig): Promise<T> => {
    const response = await axiosInstance.delete<T>(endpoint, config);
    return response.data;
  },

  // File upload with progress tracking
  uploadFile: async <T>(
    endpoint: string,
    file: FormData,
    onProgress?: (progress: number) => void
  ): Promise<T> => {
    const response = await axiosInstance.post<T>(endpoint, file, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percentCompleted);
        }
      },
    });
    return response.data;
  },
};

export default axiosInstance;

