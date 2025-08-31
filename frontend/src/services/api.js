// src/services/api.js
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:9090/api';

// Main axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});

// ---------------- Interceptors ----------------
// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwtToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Token refresh handling
let isRefreshing = false;
let refreshSubscribers = [];

const subscribeTokenRefresh = (cb) => refreshSubscribers.push(cb);

const onRefreshed = (token) => {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
};

// 🔹 Explicit refresh function (also used by the interceptor)
export const refreshToken = async () => {
  try {
    const storedRefreshToken = localStorage.getItem('refreshToken');
    if (!storedRefreshToken) throw new Error('No refresh token available');

    const refreshResponse = await axios.post(`${API_BASE_URL}/auth/refresh`, {
      refreshToken: storedRefreshToken,
    });

    const { token: newToken, refreshToken: newRefreshToken } = refreshResponse.data;

    localStorage.setItem('jwtToken', newToken);
    localStorage.setItem('refreshToken', newRefreshToken);
    api.defaults.headers.Authorization = `Bearer ${newToken}`;

    return newToken;
  } catch (error) {
    logout();
    throw error;
  }
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const originalRequest = config;

    if (response && response.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(api(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const newToken = await refreshToken(); // 🔹 uses the helper
        isRefreshing = false;
        onRefreshed(newToken);
        return api(originalRequest);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// ---------------- Auth ----------------
export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  if (response.data.token) {
    localStorage.setItem('jwtToken', response.data.token);
    localStorage.setItem('refreshToken', response.data.refreshToken);
  }
  return response.data;
};

export const register = async (userData) => {
  const response = await api.post('/auth/register', userData);
  return response.data;
};

export const logout = () => {
  localStorage.removeItem('jwtToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userProfile');
 
};

// ---------------- Profile ----------------
export const getProfile = async () => {
  const response = await api.get('/profile');
  return response.data;
};

export const saveProfile = async (profileData) => {
  const response = await api.post('/profile', profileData);
  return response.data;
};

// ---------------- Route ----------------
export const getRecommendedRoutes = async (routeRequest) => {
  const response = await api.post('/route/recommend', routeRequest);
  return response.data;
};

export const getRouteDetails = async (routeId) => {
  const response = await api.get(`/route/${routeId}`);
  return response.data;
};

// ---------------- Test Endpoints ----------------
export const testAuth = async () => {
  const response = await api.get('/test/auth');
  return response.data;
};

export const testPublic = async () => {
  const response = await api.get('/test/public');
  return response.data;
};

// ---------------- Utils ----------------
export const isAuthenticated = () => !!localStorage.getItem('jwtToken');
export const getCurrentToken = () => localStorage.getItem('jwtToken');

export default api;
