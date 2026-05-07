<script setup>
import { onMounted, ref } from 'vue';
import { fetchConversationMessages, postChatMessageStream } from './api/chat';

const CONV_STORAGE_KEY = 'aoo-ai-conversation-id';

const draft = ref('');
const loading = ref(false);
const streaming = ref(false);
const conversationId = ref(null);

const messages = ref([
  {
    role: 'assistant',
    content: '你好。请输入问题后发送；对话会按 conversationId 在后端落库。',
    localOnly: true
  }
]);

onMounted(async () => {
  const saved = localStorage.getItem(CONV_STORAGE_KEY);
  if (saved) {
    const id = Number(saved);
    if (!Number.isNaN(id) && id > 0) {
      conversationId.value = id;
    }
  }
  await syncHistoryFromServer();
});

const persistConversationId = (id) => {
  if (id != null && id > 0) {
    localStorage.setItem(CONV_STORAGE_KEY, String(id));
  }
};

const syncHistoryFromServer = async () => {
  if (!conversationId.value) {
    return;
  }
  try {
    const serverMessages = await fetchConversationMessages(conversationId.value);
    if (Array.isArray(serverMessages) && serverMessages.length > 0) {
      messages.value = serverMessages.map((m) => ({
        role: m.role,
        content: m.content ?? ''
      }));
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

  messages.value.push({
    role: 'user',
    content
  });

  draft.value = '';
  loading.value = true;

  try {
    const assistantDraft = {
      role: 'assistant',
      content: ''
    };
    messages.value.push(assistantDraft);
    streaming.value = true;

    await postChatMessageStream(
      {
        message: content,
        conversationId: conversationId.value
      },
      {
        onMeta(meta) {
          const id = Number(meta?.conversationId ?? meta);
          if (!Number.isNaN(id) && id > 0) {
            conversationId.value = id;
            persistConversationId(id);
          }
        },
        onDelta(delta) {
          const chunk = typeof delta === 'string' ? delta : delta?.delta ?? delta?.content ?? delta?.raw ?? '';
          assistantDraft.content += chunk;
        },
        onDone(done) {
          const id = Number(done?.conversationId ?? done);
          if (!Number.isNaN(id) && id > 0) {
            conversationId.value = id;
            persistConversationId(id);
          }
          if (!assistantDraft.content) {
            assistantDraft.content = '（模型返回为空）';
          }
        }
      }
    );
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: `请求失败：${error.message}`
    });
  } finally {
    loading.value = false;
    streaming.value = false;
  }
};
</script>

<template>
  <main class="chat-page">
    <header class="chat-header">
      <h1>后端对话测试</h1>
      <p>
        使用 <strong>流式</strong> <code>POST /chat/stream</code>：一次请求完成落库用户消息、Spring AI 调用与助手落库；历史通过
        <code>GET /conversation/{id}/messages</code> 拉取。
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
            {{ item.content }}
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
          {{ loading ? (streaming ? '流式输出中...' : '发送中...') : '发送（Ctrl+Enter）' }}
        </button>
      </div>
    </section>
  </main>
</template>
