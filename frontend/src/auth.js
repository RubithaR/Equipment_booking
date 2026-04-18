// Tiny auth helper backed by localStorage.

const TOKEN_KEY = 'sl_token';
const USER_KEY = 'sl_user';

export function setSession(authResponse) {
  localStorage.setItem(TOKEN_KEY, authResponse.token);
  localStorage.setItem(USER_KEY, JSON.stringify({
    id: authResponse.userId,
    email: authResponse.email,
    fullName: authResponse.fullName,
    role: authResponse.role,
  }));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getCurrentUser() {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
}

export function isAuthenticated() {
  return !!getToken();
}

export function hasRole(role) {
  const u = getCurrentUser();
  return u && u.role === role;
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function homePathFor(role) {
  if (role === 'ADMIN') return '/admin';
  if (role === 'INSTRUCTOR') return '/instructor';
  return '/student';
}
