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

export const knowledgeApiCapabilities = {
  stats: true,
  upload: true,
  listKnowledgeBases: true,
  createKnowledgeBase: true,
  updateKnowledgeBase: true,
  deleteKnowledgeBase: true,
  getKnowledgeBaseDetail: true,
  listDocuments: true,
  deleteDocument: true,
  chat: false
};

export const missingKnowledgeEndpoints = [
  '文档分页/状态刷新接口'
];

function unsupportedResponse(message) {
  return Promise.resolve({
    success: false,
    code: 'ENDPOINT_NOT_EXPOSED',
    message,
    data: null
  });
}

export async function uploadKnowledgeDocument(file, knowledgeBaseId) {
  const formData = new FormData();
  formData.append('file', file);

  const url = knowledgeBaseId
    ? `/api/v1/knowledge/bases/${knowledgeBaseId}/upload`
    : '/api/v1/knowledge/upload';
  const res = await fetch(url, {
    method: 'POST',
    body: formData
  });

  return parseUploadResponse(res);
}

export async function getKnowledgeStats() {
  const res = await fetch('/api/v1/knowledge/stats');
  return parseApiResponse(res, '知识库统计获取失败，请稍后重试');
}

export async function listKnowledgeBases() {
  const res = await fetch('/api/v1/knowledge/bases');
  return parseApiResponse(res, '知识库列表获取失败，请稍后重试');
}

export async function createKnowledgeBase({ name, description, visibility = 'COMPANY' } = {}) {
  const res = await fetch('/api/v1/knowledge/bases', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ name, description, visibility })
  });
  return parseApiResponse(res, '新建知识库失败');
}

export async function updateKnowledgeBase({ knowledgeBaseId, name }) {
  const res = await fetch(`/api/v1/knowledge/bases/${knowledgeBaseId}/name`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ name })
  });
  return parseApiResponse(res, '修改知识库名称失败');
}

export async function deleteKnowledgeBase(knowledgeBaseId) {
  const res = await fetch(`/api/v1/knowledge/bases/${knowledgeBaseId}`, {
    method: 'DELETE'
  });
  return parseApiResponse(res, '删除知识库失败');
}

export async function getKnowledgeBaseDetail() {
  return listKnowledgeBases();
}

export async function listDocuments(knowledgeBaseId) {
  const res = await fetch(`/api/v1/knowledge/bases/${knowledgeBaseId}/documents`);
  return parseApiResponse(res, '知识库文档列表获取失败，请稍后重试');
}

export async function getDocumentContent(documentId) {
  const res = await fetch(`/api/v1/knowledge/documents/${documentId}/content`);
  return parseApiResponse(res, '文档内容获取失败，请稍后重试');
}

export function getDocumentFileUrl(documentId) {
  return `/api/v1/knowledge/documents/${documentId}/file`;
}

export async function deleteDocument(documentId) {
  const res = await fetch(`/api/v1/knowledge/documents/${documentId}`, {
    method: 'DELETE'
  });
  return parseApiResponse(res, '删除文档失败');
}
