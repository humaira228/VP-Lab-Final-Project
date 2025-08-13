import api from './services/api';

export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  localStorage.setItem('jwtToken', response.data.token);
  return response.data;
};

export const register = async (email, password) => {
  await api.post('/auth/register', { email, password });
};

export const getProfile = async () => {
  const response = await api.get('/api/profile');
  return response.data;
};

export const saveProfile = async (profile) => {
  const response = await api.post('/api/profile', profile);
  return response.data;
};