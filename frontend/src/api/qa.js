const CHAT_ENDPOINT_MISSING_MESSAGE = '后端当前未暴露 AI 对话 Controller；未找到聊天请求地址，无法发起问答。';

export const chatEndpointAvailable = false;
export const chatEndpointMissingMessage = CHAT_ENDPOINT_MISSING_MESSAGE;

export async function askQuestion() {
  return {
    success: false,
    code: 'CHAT_ENDPOINT_MISSING',
    message: CHAT_ENDPOINT_MISSING_MESSAGE,
    data: null
  };
}
