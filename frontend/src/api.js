import axios from 'axios';
import { getToken, logout } from './auth';

// All requests go through the API Gateway (port 8080)
export const API_BASE = 'http://localhost:8080';

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
  checkAvailability: ({ email, enNumber, indexNumber }) =>
    api.get('/api/users/check-availability', {
      params: { email, enNumber, indexNumber },
    }),
  getById: (id) => api.get(`/api/users/${id}`),
  getAll: () => api.get('/api/users'),
  getByRole: (role) => api.get(`/api/users/by-role/${role}`),
  getPendingInstructors: (departmentId) =>
    api.get('/api/users/instructors/pending', {
      params: departmentId ? { departmentId } : {},
    }),
  approveInstructor: (id) => api.patch(`/api/users/${id}/approve`),
  rejectInstructor: (id) => api.delete(`/api/users/${id}/reject`),
  // Free-text + role search. roles is a CSV string e.g. "HOD,LECTURER".
  search: ({ q, roles, limit = 20 }) =>
    api.get('/api/users/search', { params: { q, roles, limit } }),
};

export const facultyApi = {
  list: () => api.get('/api/faculties'),
  get: (id) => api.get(`/api/faculties/${id}`),
};

export const departmentApi = {
  list: (facultyId) =>
    api.get('/api/departments', { params: facultyId ? { facultyId } : {} }),
  get: (id) => api.get(`/api/departments/${id}`),
  setHod: (id, hodUserId) =>
    api.patch(`/api/departments/${id}/hod`, { hodUserId }),
};

export const itemApi = {
  list: (params = {}) => api.get('/api/items', { params }),
  get: (id) => api.get(`/api/items/${id}`),
  create: (data) => api.post('/api/items', data),
  update: (id, data) => api.put(`/api/items/${id}`, data),
  updateStatus: (id, status) => api.patch(`/api/items/${id}/status`, { status }),
  remove: (id) => api.delete(`/api/items/${id}`),
  byStatus: (status) => api.get('/api/items', { params: { status } }),
};

// Backward-compat alias — older pages call equipmentApi while we migrate.
export const equipmentApi = itemApi;

export const labApi = {
  list: (params = {}) => api.get('/api/labs', { params }),
  get: (id) => api.get(`/api/labs/${id}`),
  create: (data) => api.post('/api/labs', data),
  update: (id, data) => api.put(`/api/labs/${id}`, data),
  assignInstructor: (id, instructorUserId) =>
    api.patch(`/api/labs/${id}/instructor`, { instructorUserId }),
  remove: (id) => api.delete(`/api/labs/${id}`),
};

// All per-line state transitions go through one umbrella endpoint with a
// discriminated body — { type: "APPROVE_DIRECTLY", pickupAt, pickupNote }, etc.
// Wrapper functions below keep the page-level call sites unchanged.
const transition = (bookingId, lineId, body) =>
  api.post(`/api/bookings/${bookingId}/items/${lineId}/transition`, body);

export const bookingApi = {
  create: (data) => api.post('/api/bookings', data),
  list: (state) => api.get('/api/bookings', { params: state ? { state } : {} }),
  get: (id) => api.get(`/api/bookings/${id}`),
  mine: () => api.get('/api/bookings/mine'),
  assignedToMe: () => api.get('/api/bookings/assigned-to-me'),
  awaitingMySupervision: () => api.get('/api/bookings/awaiting-my-supervision'),
  timeline: (id) => api.get(`/api/bookings/${id}/timeline`),
  attachments: (id) => api.get(`/api/bookings/${id}/attachments`),

  cancel: (id) => api.post(`/api/bookings/${id}/cancel`),

  // ===== Per-line instructor actions =====
  startReview: (bookingId, lineId) =>
    transition(bookingId, lineId, { type: 'START_REVIEW' }),
  approve: (bookingId, lineId, { pickupAt, pickupNote }) =>
    transition(bookingId, lineId, { type: 'APPROVE_DIRECTLY', pickupAt, pickupNote }),
  reject: (bookingId, lineId, reason) =>
    transition(bookingId, lineId, { type: 'REJECT', reason }),
  delegate: (bookingId, lineId, { supervisorUserId, note }) =>
    transition(bookingId, lineId, { type: 'DELEGATE', supervisorUserId, note }),
  finalise: (bookingId, lineId, { pickupAt, pickupNote }) =>
    transition(bookingId, lineId, { type: 'FINALISE', pickupAt, pickupNote }),
  collect: (bookingId, lineId) =>
    transition(bookingId, lineId, { type: 'MARK_COLLECTED' }),
  markReturned: (bookingId, lineId) =>
    transition(bookingId, lineId, { type: 'MARK_RETURNED' }),

  // ===== Per-line supervisor actions =====
  supervisorApprove: (bookingId, lineId, note) =>
    transition(bookingId, lineId, { type: 'SUPERVISOR_APPROVE', note }),
  supervisorDecline: (bookingId, lineId, note) =>
    transition(bookingId, lineId, { type: 'SUPERVISOR_DECLINE', note }),
};

export const notificationApi = {
  byUser: (userId) => api.get(`/api/notifications/user/${userId}`),
  unreadByUser: (userId) => api.get(`/api/notifications/user/${userId}/unread`),
  markRead: (id) => api.patch(`/api/notifications/${id}/read`),
  remove: (id) => api.delete(`/api/notifications/${id}`),
};

export default api;
