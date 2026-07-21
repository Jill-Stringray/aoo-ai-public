import { buildAuthHeaders, parseApiResponse } from './auth';

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

export async function fetchConversationList({ favoriteOnly = false } = {}) {
  const params = new URLSearchParams();
  if (favoriteOnly) params.set('favoriteOnly', 'true');
  const query = params.toString();

  const res = await fetch(`/api/v1/chat/conversation/list${query ? `?${query}` : ''}`, {
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

export async function updateConversationFavorite({ conversationId, favorite } = {}) {
  const res = await fetch(`/api/v1/chat/conversation/${conversationId}/favorite`, {
    method: 'PUT',
    headers: buildAuthHeaders({
      'Content-Type': 'application/json'
    }),
    body: JSON.stringify({ favorite: Boolean(favorite) })
  });
  return parseApiResponse(res, favorite ? '收藏会话失败' : '取消收藏失败');
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

export async function regenerateAnswer({ conversationId, messageId, knowledgeBaseId, onDelta } = {}) {
  if (!conversationId || !messageId) {
    return {
      success: false,
      code: 'INVALID_REGENERATE_TARGET',
      message: '请选择要重新生成的AI回复',
      data: null
    };
  }

  const body = {
    stream: true
  };
  if (knowledgeBaseId) body.knowledgeBaseId = knowledgeBaseId;

  const res = await fetch(`/api/v1/chat/${conversationId}/messages/${messageId}/regenerate`, {
    method: 'POST',
    headers: buildAuthHeaders({
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    }),
    body: JSON.stringify(body)
  });

  if (!res.ok) {
    return parseApiResponse(res, '重新生成失败');
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
        return {
          success: false,
          code: 'CHAT_REGENERATE_ERROR',
          message: payload?.data || '重新生成失败',
          data: null
        };
      }
    }
  }

  return {
    success: true,
    code: 'SUCCESS',
    message: 'AI 已重新生成',
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
