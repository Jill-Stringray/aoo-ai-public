async function parseApiResponse(res, fallbackMessage) {
  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : { message: await res.text() };

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

export async function fetchAdminUsers() {
  const res = await fetch('/api/v1/admin/users');
  return parseApiResponse(res, '系统用户列表获取失败');
}

export async function updateUserStatus({ userId, status }) {
  const res = await fetch(`/api/v1/admin/users/${userId}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ status })
  });
  return parseApiResponse(res, '用户状态更新失败');
}

export async function fetchSystemConfig() {
  const res = await fetch('/api/v1/admin/config');
  return parseApiResponse(res, '系统配置获取失败');
}

export async function saveSystemConfig(config) {
  const res = await fetch('/api/v1/admin/config', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(config)
  });
  return parseApiResponse(res, '系统配置保存失败');
}

export async function debugRagRetrieval(payload) {
  const res = await fetch('/api/v1/admin/rag/debug', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });
  return parseApiResponse(res, 'RAG检索调试失败');
}
