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

async function askQuestionStream({ message, conversationId, onDelta }) {
  const res = await fetch('/api/v1/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    },
    body: JSON.stringify({
      message,
      conversationId,
      stream: true
    })
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

export async function askQuestion({ message, conversationId, stream = true, onDelta } = {}) {
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
    return askQuestionStream({ message: trimmedMessage, conversationId, onDelta });
  }

  return askQuestionStream({ message: trimmedMessage, conversationId });
}
