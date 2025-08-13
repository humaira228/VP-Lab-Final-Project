import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:9090/api',
  withCredentials: true,
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('jwtToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  localStorage.setItem('jwtToken', response.data.token);
  return response.data;
};

export const register = async (email, password) => {
  await api.post('/auth/register', { email, password });
};

export const getProfile = async () => {
  const response = await api.get('/profile');
  return response.data;
};

export const saveProfile = async (profile) => {
  const response = await api.post('/profile', profile);
  return response.data;
};

export default api;