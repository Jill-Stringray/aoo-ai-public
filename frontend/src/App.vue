<script setup>
import { onMounted, ref } from 'vue';
import { fetchConversationMessages, postChatMessage } from './api/chat';

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

  try {
    // 2️⃣ 发送给后端（只传有效上下文）
    const { reply, conversationId: nextConversationId } = await postChatMessage({
      message: content,
      conversationId: conversationId.value,
      messages: messages.value
          .filter(m => m && !m.localOnly && m.role && m.content)
          .map(m => ({
            role: m.role,
            content: m.content
          }))
    });

    // 3️⃣ 更新会话 ID
    if (nextConversationId) {
      conversationId.value = nextConversationId;
    }

    // 4️⃣ 渲染 AI 回复
    messages.value.push({
      role: 'assistant',
      content: reply
    });

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
      <p>
        前端仅负责展示状态，历史记录由后端根据
        <code>conversationId</code> 维护
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
          {{ loading ? '发送中...' : '发送（Ctrl+Enter）' }}
        </button>
      </div>
    </section>
  </main>
</template>