<script setup>
import { computed, nextTick, ref } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { askQuestion, chatEndpointAvailable, chatEndpointMissingMessage } from '../api/qa';

const question = ref('');
const selectedKnowledgeBase = ref('default');
const uploadedFiles = ref([]);
const activeConversationId = ref(1);
const fileInput = ref(null);
const sendState = ref('idle');
const sendMessage = ref(chatEndpointMissingMessage);
const conversationBox = ref(null);

const knowledgeBases = [
  { id: 'default', name: '默认知识库（后端固定 knowledgeBaseId=1）' }
];

const conversations = ref([
  {
    id: 1,
    title: '新的问答会话',
    time: '刚刚',
    preview: '等待后端暴露聊天接口后即可提问。'
  }
]);

const messages = ref([
  {
    role: 'assistant',
    content: `你好，我已检查后端 Controller。当前未发现 AI 对话 HTTP 接口，因此暂不能发送问题。${chatEndpointMissingMessage}`
  }
]);

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value));
const selectedKnowledgeBaseName = computed(() => knowledgeBases.find((item) => item.id === selectedKnowledgeBase.value)?.name || '未选择知识库');
const isSending = computed(() => sendState.value === 'loading');
const canSend = computed(() => Boolean(question.value.trim()) && !isSending.value && chatEndpointAvailable);

function renderMarkdown(content) {
  return DOMPurify.sanitize(marked.parse(content || ''));
}

const chooseConversation = (conversation) => {
  activeConversationId.value = conversation.id;
};

const startNewConversation = () => {
  const nextId = Math.max(...conversations.value.map((item) => item.id), 0) + 1;
  conversations.value.unshift({
    id: nextId,
    title: '新的问答会话',
    time: '刚刚',
    preview: '等待输入第一个问题。'
  });
  activeConversationId.value = nextId;
  messages.value = [{ role: 'assistant', content: chatEndpointMissingMessage }];
  sendState.value = 'idle';
  sendMessage.value = chatEndpointMissingMessage;
};

const openFilePicker = () => {
  fileInput.value?.click();
};

const addFiles = (fileList) => {
  const nextFiles = Array.from(fileList || []).map((file) => ({
    id: `${file.name}-${file.size}-${file.lastModified}`,
    name: file.name,
    size: file.size,
    type: file.type || '未知类型'
  }));

  const existingIds = new Set(uploadedFiles.value.map((file) => file.id));
  uploadedFiles.value.push(...nextFiles.filter((file) => !existingIds.has(file.id)));
};

const handleFileChange = (event) => {
  addFiles(event.target.files);
  event.target.value = '';
};

const removeFile = (fileId) => {
  uploadedFiles.value = uploadedFiles.value.filter((file) => file.id !== fileId);
};

const formatSize = (size) => {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};

async function scrollToBottom() {
  await nextTick();
  if (conversationBox.value) {
    conversationBox.value.scrollTop = conversationBox.value.scrollHeight;
  }
}

const sendQuestion = async () => {
  if (!question.value.trim() || isSending.value) return;

  if (!chatEndpointAvailable) {
    sendState.value = 'error';
    sendMessage.value = chatEndpointMissingMessage;
    return;
  }

  const content = question.value.trim();
  messages.value.push({ role: 'user', content });
  question.value = '';
  sendState.value = 'loading';
  sendMessage.value = '正在请求 AI 对话接口…';
  await scrollToBottom();

  try {
    const response = await askQuestion({ message: content, stream: false });
    if (!response.success) throw new Error(response.message || 'AI 对话请求失败');
    messages.value.push({ role: 'assistant', content: response.data?.reply || response.data || '' });
    sendState.value = 'success';
    sendMessage.value = 'AI 已回复。';

    const active = activeConversation.value;
    if (active) {
      active.title = content.slice(0, 18);
      active.preview = content;
      active.time = '刚刚';
    }
  } catch (error) {
    sendState.value = 'error';
    sendMessage.value = error.message || 'AI 对话请求失败，请稍后重试';
    messages.value.push({ role: 'assistant', content: sendMessage.value });
  } finally {
    await scrollToBottom();
  }
};
</script>

<template>
  <section class="page-section chat-page">
    <div class="section-header">
      <div>
        <h2>智能问答</h2>
        <p class="muted">已检查后端 Controller：当前未暴露 AI 对话 HTTP/SSE 接口，前端不会自行假设请求地址。</p>
      </div>
      <span class="status-pill">用户端</span>
    </div>

    <p class="error">{{ chatEndpointMissingMessage }}</p>

    <div class="chat-workspace">
      <aside class="chat-history card" aria-label="历史问答记录">
        <div class="history-head">
          <div>
            <h3>历史问答</h3>
            <p class="muted">本地会话占位</p>
          </div>
          <button type="button" class="button secondary compact-button" @click="startNewConversation">新建</button>
        </div>
        <button
          v-for="conversation in conversations"
          :key="conversation.id"
          type="button"
          :class="['history-item', { active: conversation.id === activeConversationId }]"
          @click="chooseConversation(conversation)"
        >
          <span class="history-title">{{ conversation.title }}</span>
          <span class="history-preview">{{ conversation.preview }}</span>
          <span class="history-time">{{ conversation.time }}</span>
        </button>
      </aside>

      <article class="chat-panel card">
        <div class="chat-toolbar">
          <div>
            <h3>{{ activeConversation?.title || '新的问答会话' }}</h3>
            <p class="muted">当前知识库：{{ selectedKnowledgeBaseName }}</p>
          </div>
          <label>
            <span class="sr-only">选择知识库</span>
            <select v-model="selectedKnowledgeBase" disabled>
              <option v-for="base in knowledgeBases" :key="base.id" :value="base.id">{{ base.name }}</option>
            </select>
          </label>
        </div>

        <div ref="conversationBox" class="conversation-box" aria-live="polite">
          <div v-for="(message, index) in messages" :key="index" :class="['message-bubble', message.role]">
            <span class="message-role">{{ message.role === 'user' ? '我' : 'AOO AI' }}</span>
            <div class="markdown-body" v-html="renderMarkdown(message.content)"></div>
          </div>
        </div>

        <div class="composer-card">
          <textarea v-model="question" rows="5" placeholder="后端暴露聊天接口后可在这里输入问题。" @keydown.ctrl.enter.prevent="sendQuestion" />
          <div class="composer-actions">
            <input ref="fileInput" class="sr-only" type="file" multiple @change="handleFileChange" />
            <button type="button" class="button secondary" @click="openFilePicker">上传文件</button>
            <p :class="sendState === 'error' ? 'error' : sendState === 'success' ? 'success' : 'muted'">{{ sendMessage }}</p>
            <button type="button" :disabled="!canSend" @click="sendQuestion">{{ isSending ? '发送中…' : '发送问题' }}</button>
          </div>
        </div>

        <div class="uploaded-files">
          <div class="section-header compact-header">
            <div>
              <h3>已上传文件</h3>
              <p class="muted">后端当前没有临时附件问答上传接口，此处仅保留文件选择交互。</p>
            </div>
          </div>
          <div v-if="uploadedFiles.length" class="file-list">
            <div v-for="file in uploadedFiles" :key="file.id" class="file-chip">
              <span>📎</span>
              <strong>{{ file.name }}</strong>
              <small>{{ formatSize(file.size) }}</small>
              <button type="button" aria-label="移除文件" @click="removeFile(file.id)">×</button>
            </div>
          </div>
          <div v-else class="empty-upload muted">上传后，附件名称、大小会展示在这里；待后端提供接口后再提交到服务端。</div>
        </div>
      </article>
    </div>
  </section>
</template>
