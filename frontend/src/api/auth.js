const JSON_HEADERS = { 'Content-Type': 'application/json' };

async function parseBody(res) {
  const text = await res.text();
  if (!text) {
    return {};
  }
  try {
    return JSON.parse(text);
  } catch {
    return {};
  }
}

export async function login(userName, password) {
  const res = await fetch('/login', {
    method: 'POST',
    headers: JSON_HEADERS,
    credentials: 'include',
    body: JSON.stringify({ userName, password })
  });
  const data = await parseBody(res);
  if (!res.ok) {
    throw new Error(data.message ?? '登录失败');
  }
  return data;
}

export async function register(userName, password) {
  const res = await fetch('/register', {
    method: 'POST',
    headers: JSON_HEADERS,
    credentials: 'include',
    body: JSON.stringify({ userName, password })
  });
  const data = await parseBody(res);
  if (!res.ok) {
    throw new Error(data.message ?? '注册失败');
  }
  return data;
}

/** 使用服务端会话恢复当前用户（携带 Cookie），未登录返回 null */
export async function fetchSessionUser() {
  const res = await fetch('/me', { credentials: 'include' });
  if (res.status === 401 || res.status === 403) {
    return null;
  }
  if (!res.ok) {
    return null;
  }
  return parseBody(res);
}

export async function logoutSession() {
  await fetch('/logout', { method: 'POST', credentials: 'include' });
}
