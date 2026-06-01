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

export async function login(payload) {
  const res = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return parseJsonResponse(res);
}

export async function fetchProfile(username) {
  const res = await fetch(`/api/v1/auth/profile?username=${encodeURIComponent(username)}`);
  return parseJsonResponse(res);
}
