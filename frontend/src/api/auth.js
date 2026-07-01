const USER_PROFILE_KEY = 'aoo_user_profile';
const USERNAME_KEY = 'aoo_user';

async function parseJsonResponse(res) {
  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : {};

  if (!res.ok) {
    return {
      success: false,
      code: data?.code || 'REQUEST_FAILED',
      message: data?.message || '请求失败，请稍后重试'
    };
  }

  return data;
}

export function getStoredProfile() {
  try {
    return JSON.parse(localStorage.getItem(USER_PROFILE_KEY) || 'null');
  } catch {
    return null;
  }
}

export function getStoredUsername() {
  return localStorage.getItem(USERNAME_KEY) || '';
}

export function normalizeRole(role) {
  return String(role || 'USER').trim().toUpperCase();
}

export function isAdminRole(role) {
  return ['ADMIN', 'SUPER_ADMIN', 'SYSTEM_ADMIN', 'MANAGER'].includes(normalizeRole(role));
}

export function getHomePathByRole(role) {
  return isAdminRole(role) ? '/admin' : '/';
}

export function isAuthenticated() {
  return Boolean(getStoredUsername());
}

export function saveSession(profile) {
  if (!profile?.username) return;
  localStorage.setItem(USERNAME_KEY, profile.username);
  localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile));
}

export function clearSession() {
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(USER_PROFILE_KEY);
}

export function getCurrentIdentity() {
  const profile = getStoredProfile();
  const username = getStoredUsername();
  const role = normalizeRole(profile?.role);

  return {
    authenticated: Boolean(username),
    username,
    profile,
    role,
    isAdmin: isAdminRole(role)
  };
}

export async function login(payload) {
  const res = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJsonResponse(res);
}

export async function register(payload) {
  const res = await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJsonResponse(res);
}
