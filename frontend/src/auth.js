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
    facultyId: authResponse.facultyId ?? null,
    departmentId: authResponse.departmentId ?? null,
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

export function isAdmin(user = getCurrentUser()) {
  return user?.role === 'MAIN_ADMIN' || user?.role === 'DEPT_ADMIN';
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function homePathFor(role) {
  if (role === 'MAIN_ADMIN' || role === 'DEPT_ADMIN') return '/admin';
  if (role === 'HOD') return '/hod';
  if (role === 'INSTRUCTOR' || role === 'LECTURER') return '/instructor';
  if (role === 'STAFF') return '/staff';   // registered staff awaiting an admin-assigned role
  return '/student';
}
