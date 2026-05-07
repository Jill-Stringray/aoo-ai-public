const CHAT_API_PATH = '/chat';
const CHAT_STREAM_PATH = '/chat/stream';

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

  const data = await parseJsonResponse(response, '聊天接口');

  if (!response.ok) {
    const message = typeof data?.additionMsg === 'string'
      ? data.additionMsg
      : typeof data?.reply === 'string'
        ? data.reply
        : '请求失败';
    throw new Error(`HTTP ${response.status}: ${message}`);
  }

  // 兼容 Jackson 在 NON_EMPTY 等策略下省略空 reply；异步场景下 reply 可能为空串
  const reply =
    data != null && typeof data.reply === 'string'
      ? data.reply
      : '';

  return { ...data, reply };
}

/**
 * SSE 流式对话（POST /chat/stream）。事件：meta / delta / done / error
 *
 * @param {{message: string, conversationId?: number|null}} payload
 * @param {{onMeta?: Function, onDelta?: Function, onDone?: Function, onError?: Function}} handlers
 */
export function postChatMessageStream(payload, handlers = {}) {
  return new Promise((resolve, reject) => {
    let settled = false;

    const finish = (fn, arg) => {
      if (settled) {
        return;
      }
      settled = true;
      fn(arg);
    };

    (async () => {
      try {
        const requestBody = buildChatRequest(payload);
        const response = await fetch(CHAT_STREAM_PATH, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream'
          },
          body: JSON.stringify(requestBody)
        });

        const ct = response.headers.get('content-type') || '';
        if (!response.ok) {
          const text = await response.text();
          let msg = `HTTP ${response.status}`;
          if (ct.includes('application/json')) {
            try {
              const j = JSON.parse(text);
              msg = j.message || j.error || text || msg;
            } catch {
              msg = text || msg;
            }
          } else {
            msg = text || msg;
          }
          throw new Error(msg);
        }

        if (!ct.includes('text/event-stream') || !response.body) {
          throw new Error('聊天流接口应返回 text/event-stream，请检查代理与路由');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (value) {
            buffer += decoder.decode(value, { stream: true });
          }

          const parts = buffer.split(/\r?\n\r?\n/);
          buffer = parts.pop() ?? '';

          for (const raw of parts) {
            if (!raw.trim()) {
              continue;
            }
            const evt = parseSseBlock(raw);
            if (!evt) {
              continue;
            }

            if (evt.event === 'meta') {
              handlers.onMeta?.(evt.data);
            } else if (evt.event === 'delta') {
              handlers.onDelta?.(evt.data);
            } else if (evt.event === 'done') {
              handlers.onDone?.(evt.data);
              finish(resolve, evt.data ?? {});
              return;
            } else if (evt.event === 'error') {
              const msg = evt.data?.message || '流式请求失败';
              handlers.onError?.(msg);
              finish(reject, new Error(msg));
              return;
            }
          }

          if (done) {
            break;
          }
        }

        if (buffer.trim()) {
          const evt = parseSseBlock(buffer);
          if (evt?.event === 'done') {
            handlers.onDone?.(evt.data);
            finish(resolve, evt.data ?? {});
            return;
          }
          if (evt?.event === 'error') {
            const msg = evt.data?.message || '流式请求失败';
            handlers.onError?.(msg);
            finish(reject, new Error(msg));
            return;
          }
        }

        handlers.onDone?.({});
        finish(resolve, {});
      } catch (e) {
        handlers.onError?.(e?.message || String(e));
        finish(reject, e);
      }
    })();
  });
}

function parseSseBlock(block) {
  let eventName = 'message';
  const dataLines = [];
  for (const line of block.split(/\r?\n/)) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart());
    }
  }
  const dataStr = dataLines.join('\n');
  let data = {};
  if (dataStr) {
    try {
      data = JSON.parse(dataStr);
    } catch {
      data = { raw: dataStr };
    }
  }
  return { event: eventName, data };
}

/**
 * MQ 异步落库时，HTTP 仅返回空 reply；轮询历史直到最后一条为 assistant。
 */
export async function pollUntilAssistantReply(conversationId, options = {}) {
  const intervalMs = options.intervalMs ?? 800;
  const timeoutMs = options.timeoutMs ?? 60_000;
  const start = Date.now();

  while (Date.now() - start < timeoutMs) {
    const list = await fetchConversationMessages(conversationId);
    const last = list[list.length - 1];
    if (last && last.role === 'assistant') {
      return list;
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }

  throw new Error('等待 AI 回复超时，请稍后刷新或重试');
}

export async function fetchConversationMessages(conversationId) {
  const response = await fetch(`/conversation/${conversationId}/messages`);
  const data = await parseJsonResponse(response, '历史消息接口');

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: 拉取历史消息失败`);
  }

  if (!Array.isArray(data)) {
    throw new Error('接口返回格式错误：消息列表不是数组');
  }

  return data;
}

async function parseJsonResponse(response, apiName) {
  const contentType = response.headers.get('content-type') || '';
  const rawText = await response.text();

  if (!contentType.includes('application/json')) {
    throw new Error(`${apiName}返回非 JSON 响应，请检查前端代理和后端路由配置`);
  }

  try {
    return JSON.parse(rawText);
  } catch {
    throw new Error(`${apiName}返回了无效 JSON`);
  }
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
