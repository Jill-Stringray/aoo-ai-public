<script setup>
import { ref } from 'vue';
import { postChatMessage } from './api/chat';

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

const sendMessage = async () => {
  const content = draft.value.trim();
  if (!content || loading.value) {
    return;
  }

  messages.value.push({ role: 'user', content });
  draft.value = '';
  loading.value = true;

  try {
    const { reply, conversationId: nextConversationId } = await postChatMessage({
      message: content,
      conversationId: conversationId.value
    });
    if (nextConversationId) {
      conversationId.value = nextConversationId;
    }
    messages.value.push({ role: 'assistant', content: reply });
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: `请求失败：${error.message}`
    });
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <main class="chat-page">
    <header class="chat-header">
      <h1>后端对话接口测试窗口</h1>
      <p>前端本地即时渲染消息，同时后端持久化会话；必要时可通过 <code>GET /conversation/{id}/messages</code> 做历史同步。</p>
    </header>

    <section class="chat-panel">
      <div class="message-list">
        <article
          v-for="(item, index) in messages"
          :key="index"
          class="message-item"
          :class="item.role"
        >
          <div class="message-role">{{ item.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-content">{{ item.content }}</div>
        </article>
      </div>

      <div class="composer">
        <textarea
          v-model="draft"
          :disabled="loading"
          placeholder="请输入你要测试的问题..."
          @keydown.ctrl.enter="sendMessage"
        ></textarea>
        <button type="button" :disabled="loading || !draft.trim()" @click="sendMessage">
          {{ loading ? '发送中...' : '发送（Ctrl+Enter）' }}
        </button>
      </div>
    </section>
  </main>
</template>
