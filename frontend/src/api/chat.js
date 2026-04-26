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
  const response = await fetch(CHAT_API_PATH, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
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
