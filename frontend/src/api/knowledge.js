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
  listKnowledgeBases: false,
  createKnowledgeBase: false,
  updateKnowledgeBase: false,
  deleteKnowledgeBase: false,
  getKnowledgeBaseDetail: false,
  listDocuments: false,
  deleteDocument: false,
  chat: false
};

export const missingKnowledgeEndpoints = [
  '知识库分页/搜索/详情/新增/编辑/删除接口',
  '文档列表/分页/删除/状态刷新接口',
  '支持按 knowledgeBaseId 上传或检索的接口'
];

function unsupportedResponse(message) {
  return Promise.resolve({
    success: false,
    code: 'ENDPOINT_NOT_EXPOSED',
    message,
    data: null
  });
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

export async function listKnowledgeBases() {
  return unsupportedResponse('后端当前未暴露知识库列表接口；仅可读取 /api/v1/knowledge/stats 汇总数据。');
}

export async function createKnowledgeBase() {
  return unsupportedResponse('后端当前未暴露新建知识库接口。');
}

export async function updateKnowledgeBase() {
  return unsupportedResponse('后端当前未暴露修改知识库接口。');
}

export async function deleteKnowledgeBase() {
  return unsupportedResponse('后端当前未暴露删除知识库接口。');
}

export async function getKnowledgeBaseDetail() {
  return unsupportedResponse('后端当前未暴露知识库详情接口。');
}

export async function listDocuments() {
  return unsupportedResponse('后端当前未暴露文档列表接口；上传成功后只能展示本页面本次上传记录。');
}

export async function deleteDocument() {
  return unsupportedResponse('后端当前未暴露删除文档接口。');
}
