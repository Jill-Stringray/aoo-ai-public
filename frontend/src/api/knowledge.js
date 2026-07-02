async function parseApiResponse(res, fallbackMessage) {
  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : { message: await res.text() };

  if (!res.ok || data?.success === false) {
    return {
      success: false,
      code: data?.code || 'REQUEST_FAILED',
      message: data?.message || fallbackMessage
    };
  }

  return data?.success === undefined
    ? { success: true, code: 'SUCCESS', message: data?.message || '操作成功', data }
    : data;
}

async function parseUploadResponse(res) {
  return parseApiResponse(res, '上传失败，请检查文件后重试');
}

export async function uploadKnowledgeDocument(file) {
  const formData = new FormData();
  formData.append('file', file);

  const res = await fetch('/api/v1/knowledge/upload', {
    method: 'POST',
    body: formData
  });

  return parseUploadResponse(res);
}

export async function getKnowledgeStats() {
  const res = await fetch('/api/v1/knowledge/stats');
  return parseApiResponse(res, '知识库统计获取失败，请稍后重试');
}

const pendingKnowledgeApi = async (fallback) => fallback;

export async function listKnowledgeBases() {
  return pendingKnowledgeApi([]);
}

export async function createKnowledgeBase() {
  return pendingKnowledgeApi(null);
}

export async function listDocuments() {
  return pendingKnowledgeApi([]);
}

export async function createDocument() {
  return pendingKnowledgeApi(null);
}
