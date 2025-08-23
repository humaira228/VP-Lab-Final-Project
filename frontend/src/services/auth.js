// src/services/auth.js
import api from './api';

export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  localStorage.setItem('jwtToken', response.data.token);
  return response.data;
};

export const register = async (email, password) => {
  await api.post('/auth/register', { email, password });
};

export const logout = () => {
  localStorage.removeItem('jwtToken');
};

export const getProfile = async () => {
  const response = await api.get('/profile');
  return response.data;
};

export const saveProfile = async (profile) => {
  const response = await api.post('/profile', profile);
  return response.data;
};