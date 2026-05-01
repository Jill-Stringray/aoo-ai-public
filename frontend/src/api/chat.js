const CHAT_API_PATH = '/chat';

/**
 * 调用聊天接口。
 *
 * @param {{message: string, conversationId?: number|null}} payload
 * @returns {Promise<{reply: string, conversationId?: number}>}
 */
export async function postChatMessage(payload) {
  const requestBody = buildChatRequest(payload);

  const response = await fetch(CHAT_API_PATH, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(requestBody)
  });

  const data = await response.json();

  if (!response.ok) {
    const message = typeof data?.additionMsg === 'string'
      ? data.additionMsg
      : typeof data?.reply === 'string'
        ? data.reply
        : '请求失败';
    throw new Error(`HTTP ${response.status}: ${message}`);
  }

  if (typeof data?.reply !== 'string') {
    throw new Error('接口返回格式错误：缺少 reply 字段');
  }

  return data;
}

export async function fetchConversationMessages(conversationId) {
  const response = await fetch(`/conversation/${conversationId}/messages`);
  const data = await response.json();

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: 拉取历史消息失败`);
  }

  if (!Array.isArray(data)) {
    throw new Error('接口返回格式错误：消息列表不是数组');
  }

  return data;
}

function buildChatRequest(payload) {
  if (!payload || typeof payload !== 'object') {
    throw new Error('请求参数不能为空');
  }

  if (typeof payload.message === 'string' && payload.message.trim()) {
    return {
      ...payload,
      message: payload.message.trim()
    };
  }

  throw new Error('请求参数错误：请提供 message');
}
