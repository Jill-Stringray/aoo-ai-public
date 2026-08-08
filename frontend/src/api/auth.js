const USER_PROFILE_KEY = 'aoo_user_profile';
const USERNAME_KEY = 'aoo_user';
const TOKEN_KEY = 'aoo_token';
const AUTH_EXPIRED_EVENT = 'aoo-auth-expired';
let authExpiredNotified = false;

export async function parseApiResponse(res, fallbackMessage = '请求失败，请稍后重试') {
  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : { message: await res.text() };

  if (isAuthExpiredResponse(res, data)) {
    notifyAuthExpired();
  }

  if (!res.ok || data?.success === false) {
    return {
      success: false,
      code: data?.code || 'REQUEST_FAILED',
      message: data?.message || fallbackMessage,
      data: null
    };
  }

  return data?.success === undefined
    ? { success: true, code: 'SUCCESS', message: data?.message || '操作成功', data }
    : data;
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

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + (4 - normalizedPayload.length % 4) % 4,
      '='
    );
    return JSON.parse(window.atob(paddedPayload));
  } catch {
    return null;
  }
}

export function isStoredTokenExpired() {
  const token = getStoredToken();
  if (!token) return true;

  const payload = decodeJwtPayload(token);
  if (!payload?.exp) return false;

  return payload.exp * 1000 <= Date.now();
}

export function isAuthExpiredResponse(res, data = {}) {
  if (!getStoredToken()) return false;
  const code = String(data?.code || '').toUpperCase();
  const message = String(data?.message || '');

  return [401, 403].includes(res.status)
    || ['UNAUTHORIZED', 'FORBIDDEN', 'TOKEN_EXPIRED', 'JWT_EXPIRED', 'AUTH_EXPIRED'].includes(code)
    || (/jwt|token|认证|登录|身份/.test(message) && /过期|失效|无效|未登录|unauthorized|forbidden/i.test(message));
}

export function notifyAuthExpired() {
  if (authExpiredNotified) return;
  authExpiredNotified = true;
  clearSession();
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT, {
    detail: { message: '身份认证已过期' }
  }));
}

export function resetAuthExpiredNotice() {
  authExpiredNotified = false;
}

export { AUTH_EXPIRED_EVENT };

export function buildAuthHeaders(extraHeaders = {}) {
  const token = getStoredToken();
  return {
    ...extraHeaders,
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };
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
  if (!getStoredUsername() || !getStoredToken()) return false;
  if (isStoredTokenExpired()) {
    notifyAuthExpired();
    return false;
  }
  return true;
}

export function normalizeLoginPayload(payload) {
  if (!payload) return null;
  if (payload.user) {
    return {
      ...payload.user,
      token: payload.token
    };
  }
  return payload;
}

export function saveSession(payload) {
  const profile = normalizeLoginPayload(payload);
  if (!profile?.username) return null;

  resetAuthExpiredNotice();
  localStorage.setItem(USERNAME_KEY, profile.username);
  localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile));
  if (profile.token) {
    localStorage.setItem(TOKEN_KEY, profile.token);
  }

  return profile;
}

export function clearSession() {
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(USER_PROFILE_KEY);
  localStorage.removeItem(TOKEN_KEY);
}

export function getCurrentIdentity() {
  const profile = getStoredProfile();
  const username = getStoredUsername();
  const authenticated = Boolean(username) && Boolean(getStoredToken()) && !isStoredTokenExpired();
  const role = normalizeRole(profile?.role);

  if (username && !authenticated) {
    notifyAuthExpired();
  }

  return {
    authenticated,
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
  return parseApiResponse(res);
}

export async function register(payload) {
  const res = await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseApiResponse(res);
}
