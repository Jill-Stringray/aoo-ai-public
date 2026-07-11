<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import {
  askQuestion,
  chatEndpointAvailable,
  fetchConversationList,
  fetchConversationMessages
} from '../api/qa';

const question = ref('');
const selectedKnowledgeBase = ref('default');
const uploadedFiles = ref([]);
const activeConversationId = ref(null);
const fileInput = ref(null);
const sendState = ref('idle');
const sendMessage = ref('输入问题后发送，AI 将基于知识库流式回复。');
const conversationBox = ref(null);
const conversations = ref([]);
const messages = ref([]);
const listState = ref('idle');
const messageState = ref('idle');
const listMessage = ref('');
const messageNotice = ref('');
const hasMoreMessages = ref(false);

const knowledgeBases = [
  { id: 'default', name: '默认知识库（后端固定 knowledgeBaseId=1）' }
];

const welcomeMessage = {
  role: 'assistant',
  content: '你好，我是 AOO AI。我会根据知识库内容回答你的问题；若知识库中没有相关信息，我会明确告知。'
};

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value));
const selectedKnowledgeBaseName = computed(() => knowledgeBases.find((item) => item.id === selectedKnowledgeBase.value)?.name || '未选择知识库');
const isSending = computed(() => sendState.value === 'loading');
const isLoadingList = computed(() => listState.value === 'loading');
const isLoadingMessages = computed(() => messageState.value === 'loading');
const canSend = computed(() => Boolean(question.value.trim()) && !isSending.value && chatEndpointAvailable);
const oldestMessageId = computed(() => messages.value.find((item) => item.id)?.id);

function renderMarkdown(content) {
  return DOMPurify.sanitize(marked.parse(content || ''));
}

function normalizeRole(role) {
  const value = String(role || 'assistant').toLowerCase();
  return value === 'user' ? 'user' : 'assistant';
}

function normalizeConversation(item) {
  return {
    id: item.conversationId ?? item.id,
    title: item.title || '新对话',
    time: formatTime(item.updateTime || item.updatedAt || item.createTime),
    preview: '点击查看历史消息',
    raw: item
  };
}

function normalizeMessage(item) {
  return {
    id: item.id,
    role: normalizeRole(item.role),
    content: item.content || '',
    createTime: item.createTime
  };
}

function formatTime(value) {
  if (!value) return '刚刚';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');

  const diffMs = Date.now() - date.getTime();
  if (diffMs >= 0 && diffMs < 60_000) return '刚刚';
  if (diffMs >= 0 && diffMs < 3_600_000) return `${Math.floor(diffMs / 60_000)} 分钟前`;
  if (diffMs >= 0 && diffMs < 86_400_000) return `${Math.floor(diffMs / 3_600_000)} 小时前`;

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

async function scrollToBottom() {
  await nextTick();
  if (conversationBox.value) {
    conversationBox.value.scrollTop = conversationBox.value.scrollHeight;
  }
}

async function loadConversations({ preferLatest = false } = {}) {
  listState.value = 'loading';
  listMessage.value = '';

  try {
    const response = await fetchConversationList();
    if (!response.success) throw new Error(response.message || '获取会话列表失败');

    conversations.value = (response.data || []).map(normalizeConversation);
    if (!conversations.value.length) {
      activeConversationId.value = null;
      messages.value = [welcomeMessage];
      hasMoreMessages.value = false;
      return;
    }

    const currentStillExists = conversations.value.some((item) => item.id === activeConversationId.value);
    if (preferLatest || !currentStillExists) {
      activeConversationId.value = conversations.value[0].id;
    }
  } catch (error) {
    listMessage.value = error.message || '获取会话列表失败';
  } finally {
    listState.value = 'idle';
  }
}

async function loadConversationMessages(conversationId, { beforeId, appendBefore = false } = {}) {
  if (!conversationId) {
    messages.value = [welcomeMessage];
    hasMoreMessages.value = false;
    return;
  }

  messageState.value = 'loading';
  messageNotice.value = '';

  try {
    const response = await fetchConversationMessages({ conversationId, beforeId, limit: 20 });
    if (!response.success) throw new Error(response.message || '获取会话消息失败');

    const nextMessages = (response.data || []).map(normalizeMessage);
    hasMoreMessages.value = nextMessages.length >= 20;
    messages.value = appendBefore ? [...nextMessages, ...messages.value] : (nextMessages.length ? nextMessages : [welcomeMessage]);
    if (!appendBefore) await scrollToBottom();
  } catch (error) {
    messageNotice.value = error.message || '获取会话消息失败';
    if (!appendBefore) messages.value = [welcomeMessage];
  } finally {
    messageState.value = 'idle';
  }
}

const chooseConversation = async (conversation) => {
  if (isSending.value || conversation.id === activeConversationId.value) return;
  activeConversationId.value = conversation.id;
  await loadConversationMessages(conversation.id);
};

const loadMoreMessages = async () => {
  if (!activeConversationId.value || isLoadingMessages.value || !oldestMessageId.value) return;
  await loadConversationMessages(activeConversationId.value, {
    beforeId: oldestMessageId.value,
    appendBefore: true
  });
};

const startNewConversation = () => {
  activeConversationId.value = null;
  messages.value = [{
    role: 'assistant',
    content: '已开始新的问答会话，请输入你的问题。'
  }];
  sendState.value = 'idle';
  sendMessage.value = '输入问题后发送，AI 将创建新的后端会话。';
  messageNotice.value = '';
  hasMoreMessages.value = false;
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

const sendQuestion = async () => {
  if (!question.value.trim() || isSending.value) return;

  const content = question.value.trim();
  const conversationIdBeforeSend = activeConversationId.value;
  messages.value.push({ role: 'user', content });
  messages.value.push({ role: 'assistant', content: '' });
  const assistantIndex = messages.value.length - 1;
  question.value = '';
  sendState.value = 'loading';
  sendMessage.value = '正在请求 AI 对话接口...';
  await scrollToBottom();

  try {
    const response = await askQuestion({
      message: content,
      conversationId: conversationIdBeforeSend,
      stream: true,
      onDelta: (_token, fullReply) => {
        messages.value[assistantIndex].content = fullReply;
        scrollToBottom();
      }
    });
    if (!response.success) throw new Error(response.message || 'AI 对话请求失败');
    messages.value[assistantIndex].content = response.data?.reply || messages.value[assistantIndex].content;
    sendState.value = 'success';
    sendMessage.value = 'AI 已回复。';

    const active = activeConversation.value;
    if (active) {
      active.preview = content;
      active.time = '刚刚';
    }

    await loadConversations({ preferLatest: !conversationIdBeforeSend });
  } catch (error) {
    sendState.value = 'error';
    sendMessage.value = error.message || 'AI 对话请求失败，请稍后重试';
    if (!messages.value[assistantIndex].content) {
      messages.value[assistantIndex].content = sendMessage.value;
    }
  } finally {
    await scrollToBottom();
  }
};

onMounted(async () => {
  await loadConversations();
  if (activeConversationId.value) {
    await loadConversationMessages(activeConversationId.value);
  } else {
    messages.value = [welcomeMessage];
  }
});
</script>

<template>
  <section class="page-section chat-page">
    <div class="section-header">
      <div>
        <h2>智能问答</h2>
        <p class="muted">已接入后端 `/api/v1/chat/stream`、会话列表和会话消息接口。</p>
      </div>
      <span class="status-pill">用户端</span>
    </div>

    <div class="chat-workspace">
      <aside class="chat-history card" aria-label="历史问答记录">
        <div class="history-head">
          <div>
            <h3>历史问答</h3>
            <p class="muted">{{ isLoadingList ? '正在加载...' : '来自后端会话列表' }}</p>
          </div>
          <button type="button" class="button secondary compact-button" @click="startNewConversation">新建</button>
        </div>

        <p v-if="listMessage" class="error">{{ listMessage }}</p>
        <div v-else-if="!conversations.length && !isLoadingList" class="empty-upload muted">暂无历史会话，发送第一条消息后会自动创建。</div>

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
          <button
            v-if="activeConversationId && hasMoreMessages"
            type="button"
            class="button secondary compact-button"
            :disabled="isLoadingMessages"
            @click="loadMoreMessages"
          >
            {{ isLoadingMessages ? '加载中...' : '加载更早消息' }}
          </button>
          <p v-if="messageNotice" class="error">{{ messageNotice }}</p>
          <p v-if="isLoadingMessages && !messages.length" class="muted">正在加载消息...</p>

          <div v-for="(message, index) in messages" :key="message.id || index" :class="['message-bubble', message.role]">
            <span class="message-role">{{ message.role === 'user' ? '我' : 'AOO AI' }}</span>
            <div class="markdown-body" v-html="renderMarkdown(message.content)"></div>
          </div>
        </div>

        <div class="composer-card">
          <textarea v-model="question" rows="5" placeholder="在这里输入你的问题，Ctrl+Enter 发送。" @keydown.ctrl.enter.prevent="sendQuestion" />
          <div class="composer-actions">
            <input ref="fileInput" class="sr-only" type="file" multiple @change="handleFileChange" />
            <button type="button" class="button secondary" @click="openFilePicker">上传文件</button>
            <p :class="sendState === 'error' ? 'error' : sendState === 'success' ? 'success' : 'muted'">{{ sendMessage }}</p>
            <button type="button" :disabled="!canSend" @click="sendQuestion">{{ isSending ? '发送中...' : '发送问题' }}</button>
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
              <span>FILE</span>
              <strong>{{ file.name }}</strong>
              <small>{{ formatSize(file.size) }}</small>
              <button type="button" aria-label="移除文件" @click="removeFile(file.id)">x</button>
            </div>
          </div>
          <div v-else class="empty-upload muted">上传后，附件名称和大小会显示在这里；待后端提供接口后再提交到服务端。</div>
        </div>
      </article>
    </div>
  </section>
</template>
