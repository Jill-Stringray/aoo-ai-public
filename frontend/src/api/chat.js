const CHAT_API_PATH = '/chat';

/**
 * @typedef {Object} ChatRequest
 * @property {string} message 用户输入文本
 */

/**
 * @typedef {Object} ChatResponse
 * @property {string} reply 助手回复文本
 */

/**
 * 调用聊天接口。
 *
 * @param {ChatRequest} payload
 * @returns {Promise<ChatResponse>}
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
    const message = typeof data?.reply === 'string' ? data.reply : '请求失败';
    throw new Error(`HTTP ${response.status}: ${message}`);
  }

  if (typeof data?.reply !== 'string') {
    throw new Error('接口返回格式错误：缺少 reply 字段');
  }

  return data;
}

/**
 * 将前端消息流标准化为后端 /chat 接口支持的请求体。
 *
 * @param {ChatRequest} payload
 * @returns {ChatRequest}
 */
function buildChatRequest(payload) {
  if (!payload || typeof payload !== 'object') {
    throw new Error('请求参数不能为空');
  }

  if (Array.isArray(payload.messages) && payload.messages.length > 0) {
    const messages = payload.messages
      .filter((item) => item && typeof item.content === 'string' && item.content.trim())
      .map((item) => ({
        role: typeof item.role === 'string' && item.role.trim() ? item.role.trim() : 'user',
        content: item.content.trim()
      }));

    if (messages.length > 0) {
      return {
        ...payload,
        messages
      };
    }
  }

  if (typeof payload.message === 'string' && payload.message.trim()) {
    return {
      ...payload,
      message: payload.message.trim()
    };
  }

  throw new Error('请求参数错误：请提供 message 或非空 messages');
}
