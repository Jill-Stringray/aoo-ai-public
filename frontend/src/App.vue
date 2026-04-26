<script setup>
import { ref } from 'vue';
import { postChatMessage } from './api/chat';

const draft = ref('');
const loading = ref(false);
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是测试助手。请输入问题后点击发送。'
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
    const { reply } = await postChatMessage({ message: content });
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
      <p>前端将调用 <code>POST /chat</code>，请求体为 <code>{"message":"..."}</code>。</p>
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
