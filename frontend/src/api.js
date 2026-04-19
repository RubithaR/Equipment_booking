import axios from 'axios';
import { getToken, logout } from './auth';

// All requests go through the API Gateway (port 8080)
export const API_BASE = 'https://user-service-production-c390.up.railway.app';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, kick the user back to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !err.config?.url?.includes('/login')) {
      logout();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ===== Helpers to extract clean error messages =====
export function errMsg(err) {
  return err?.response?.data?.message || err?.message || 'Request failed';
}

// ===== API calls grouped by domain =====
export const userApi = {
  register: (data) => api.post('/api/users/register', data),
  login: (email, password) => api.post('/api/users/login', { email, password }),
  getById: (id) => api.get(`/api/users/${id}`),
  getAll: () => api.get('/api/users'),
  getByRole: (role) => api.get(`/api/users/by-role/${role}`),
  getPendingInstructors: () => api.get('/api/users/instructors/pending'),
  approveInstructor: (id) => api.patch(`/api/users/${id}/approve`),
  rejectInstructor: (id) => api.delete(`/api/users/${id}/reject`),
};

export const equipmentApi = {
  list: () => api.get('/api/equipment'),
  get: (id) => api.get(`/api/equipment/${id}`),
  create: (data) => api.post('/api/equipment', data),
  update: (id, data) => api.put(`/api/equipment/${id}`, data),
  updateStatus: (id, status) => api.patch(`/api/equipment/${id}/status`, { status }),
  remove: (id) => api.delete(`/api/equipment/${id}`),
  byStatus: (status) => api.get(`/api/equipment/status/${status}`),
};

export const bookingApi = {
  create: (data) => api.post('/api/bookings', data),
  list: () => api.get('/api/bookings'),
  get: (id) => api.get(`/api/bookings/${id}`),
  byUser: (userId) => api.get(`/api/bookings/user/${userId}`),
  byStatus: (status) => api.get(`/api/bookings/status/${status}`),
  approve: (id, instructorId, note) =>
    api.patch(`/api/bookings/${id}/approve`, { instructorId, note }),
  reject: (id, instructorId, note) =>
    api.patch(`/api/bookings/${id}/reject`, { instructorId, note }),
  cancel: (id) => api.patch(`/api/bookings/${id}/cancel`),
};

export const notificationApi = {
  byUser: (userId) => api.get(`/api/notifications/user/${userId}`),
  unreadByUser: (userId) => api.get(`/api/notifications/user/${userId}/unread`),
  markRead: (id) => api.patch(`/api/notifications/${id}/read`),
  remove: (id) => api.delete(`/api/notifications/${id}`),
};

export default api;
