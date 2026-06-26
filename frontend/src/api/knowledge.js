async function parseUploadResponse(res) {
  const contentType = res.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await res.json() : { message: await res.text() };

  if (!res.ok || data?.success === false) {
    return {
      success: false,
      code: data?.code || 'UPLOAD_FAILED',
      message: data?.message || '上传失败，请检查文件后重试'
    };
  }

  return data?.success === undefined
    ? { success: true, code: 'SUCCESS', message: data?.message || '上传成功', data }
    : data;
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
