import { buildAuthHeaders } from './auth';

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

function parseSseBlock(block) {
  let eventName = 'message';
  let data = '';

  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      data += line.slice(5).trim();
    }
  }

  return { eventName, data };
}

export async function fetchConversationList() {
  const res = await fetch('/api/v1/chat/conversation/list', {
    headers: buildAuthHeaders()
  });
  return parseApiResponse(res, '获取会话列表失败');
}

export async function fetchConversationMessages({ conversationId, beforeId, limit = 20 } = {}) {
  if (!conversationId) {
    return {
      success: false,
      code: 'INVALID_CONVERSATION',
      message: '请选择会话后再加载消息',
      data: null
    };
  }

  const params = new URLSearchParams({ limit: String(limit) });
  if (beforeId) params.set('beforeId', String(beforeId));

  const res = await fetch(`/api/v1/chat/${conversationId}/messages?${params.toString()}`, {
    headers: buildAuthHeaders()
  });
  return parseApiResponse(res, '获取会话消息失败');
}

export async function renameConversation({ conversationId, title } = {}) {
  const res = await fetch(`/api/v1/chat/conversation/${conversationId}/title`, {
    method: 'PUT',
    headers: buildAuthHeaders({
      'Content-Type': 'application/json'
    }),
    body: JSON.stringify({ title })
  });
  return parseApiResponse(res, '修改会话标题失败');
}

export async function deleteConversation(conversationId) {
  const res = await fetch(`/api/v1/chat/conversation/${conversationId}`, {
    method: 'DELETE',
    headers: buildAuthHeaders()
  });
  return parseApiResponse(res, '删除会话失败');
}

async function askQuestionStream({ message, conversationId, knowledgeBaseId, onDelta }) {
  const body = {
    message,
    stream: true
  };
  if (conversationId) body.conversationId = conversationId;
  if (knowledgeBaseId) body.knowledgeBaseId = knowledgeBaseId;

  const res = await fetch('/api/v1/chat/stream', {
    method: 'POST',
    headers: buildAuthHeaders({
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    }),
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    return parseApiResponse(res, 'AI 对话请求失败');
  }

  if (!res.body) {
    return {
      success: false,
      code: 'STREAM_UNAVAILABLE',
      message: '浏览器不支持流式响应',
      data: null
    };
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let reply = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split('\n\n');
    buffer = blocks.pop() || '';

    for (const block of blocks) {
      if (!block.trim()) continue;

      const { eventName, data } = parseSseBlock(block);
      if (!data) continue;

      let payload;
      try {
        payload = JSON.parse(data);
      } catch {
        continue;
      }

      if (eventName === 'delta') {
        const token = payload?.data ?? '';
        if (token) {
          reply += token;
          onDelta?.(token, reply);
        }
      } else if (eventName === 'error') {
        const errorMessage = payload?.data || 'AI 对话失败';
        return {
          success: false,
          code: 'CHAT_STREAM_ERROR',
          message: errorMessage,
          data: null
        };
      }
    }
  }

  return {
    success: true,
    code: 'SUCCESS',
    message: 'AI 已回复',
    data: { reply }
  };
}

export const chatEndpointAvailable = true;

export async function askQuestion({ message, conversationId, knowledgeBaseId, stream = true, onDelta } = {}) {
  const trimmedMessage = String(message || '').trim();
  if (!trimmedMessage) {
    return {
      success: false,
      code: 'INVALID_MESSAGE',
      message: '请输入问题后再发送',
      data: null
    };
  }

  if (stream) {
    return askQuestionStream({ message: trimmedMessage, conversationId, knowledgeBaseId, onDelta });
  }

  return askQuestionStream({ message: trimmedMessage, conversationId, knowledgeBaseId });
}
