<script setup>
import { onMounted, ref } from 'vue';
import { fetchConversationMessages, postChatMessageStream } from './api/chat';

const draft = ref('');
const loading = ref(false);
const conversationId = ref(null);

const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是测试助手。请输入问题后点击发送。',
    localOnly: true
  }
]);

onMounted(async () => {
  await syncHistoryFromServer();
});

const syncHistoryFromServer = async () => {
  if (!conversationId.value) {
    return;
  }
  try {
    const serverMessages = await fetchConversationMessages(conversationId.value);
    if (Array.isArray(serverMessages) && serverMessages.length > 0) {
      messages.value = serverMessages;
    }
  } catch {
    // 后端不可用时忽略，继续使用本地内存状态
  }
};

const sendMessage = async () => {
  const content = draft.value.trim();
  if (!content || loading.value) {
    return;
  }

  // 1️⃣ 立即渲染用户消息
  messages.value.push({
    role: 'user',
    content
  });

  draft.value = '';
  loading.value = true;

  messages.value.push({
    role: 'assistant',
    content: '',
    streaming: true
  });

  try {
    await postChatMessageStream(
      {
        message: content,
        conversationId: conversationId.value,
        messages: messages.value
            .filter(m => m && !m.localOnly && !m.streaming && m.role && m.content)
            .map(m => ({
              role: m.role,
              content: m.content
            }))
      },
      {
        onMeta: (data) => {
          if (data?.conversationId != null) {
            conversationId.value = data.conversationId;
          }
        },
        onDelta: (data) => {
          const piece = data?.c;
          if (typeof piece !== 'string' || !piece) {
            return;
          }
          const last = messages.value[messages.value.length - 1];
          if (last?.role === 'assistant') {
            last.content += piece;
          }
        },
        onDone: () => {
          const last = messages.value[messages.value.length - 1];
          if (last?.streaming) {
            delete last.streaming;
          }
        },
        onError: (msg) => {
          const last = messages.value[messages.value.length - 1];
          if (last?.role === 'assistant') {
            if (!last.content) {
              last.content = msg || '流式请求失败';
            }
            delete last.streaming;
          }
        }
      }
    );
  } catch (error) {
    const last = messages.value[messages.value.length - 1];
    if (last?.role === 'assistant' && last?.streaming) {
      last.content = last.content || `请求失败：${error.message}`;
      delete last.streaming;
    } else {
      messages.value.push({
        role: 'assistant',
        content: `请求失败：${error.message}`
      });
    }
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <main class="chat-page">
    <header class="chat-header">
      <h1>后端对话接口测试窗口</h1>
      <p>
        使用 <strong>SSE</strong>（<code>POST /chat/stream</code>）流式输出；历史由后端根据
        <code>conversationId</code> 落库
      </p>
    </header>

    <section class="chat-panel">
      <div class="message-list">
        <article
            v-for="(item, index) in messages"
            :key="index"
            class="message-item"
            :class="item.role"
        >
          <div class="message-role">
            {{ item.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="message-content">
            {{ item.content }}<span v-if="item.streaming" class="stream-caret" aria-hidden="true">▍</span>
          </div>
        </article>
      </div>

      <div class="composer">
        <textarea
            v-model="draft"
            :disabled="loading"
            placeholder="请输入你要测试的问题..."
            @keydown.ctrl.enter="sendMessage"
        ></textarea>

        <button
            type="button"
            :disabled="loading || !draft.trim()"
            @click="sendMessage"
        >
          {{ loading ? '发送中...' : '发送（Ctrl+Enter）' }}
        </button>
      </div>
    </section>
  </main>
</template>