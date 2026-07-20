<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import CitationMarkdown from '../components/CitationMarkdown.vue';
import {
  askQuestion,
  chatEndpointAvailable,
  deleteConversation,
  fetchConversationList,
  fetchConversationMessages,
  renameConversation
} from '../api/qa';
import { getDocumentFileUrl, listKnowledgeBases } from '../api/knowledge';

const question = ref('');
const selectedKnowledgeBase = ref(null);
const knowledgeBases = ref([]);
const knowledgeState = ref('idle');
const knowledgeMessage = ref('');
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
const selectedSource = ref(null);
const isSourceDrawerOpen = ref(false);
const activeMenuConversationId = ref(null);
const historyMenuPosition = ref({ top: 0, left: 0, placement: 'down' });
const renameDialogOpen = ref(false);
const renameConversationId = ref(null);
const renameConversationTitle = ref('');
const renameConversationSaving = ref(false);
const deleteDialogOpen = ref(false);
const deleteConversationId = ref(null);
const deleteConversationTitle = ref('');
const deleteConversationSaving = ref(false);

const welcomeMessage = {
  role: 'assistant',
  content: '你好，我是 AOO AI。我会根据知识库内容回答你的问题；若知识库中没有相关信息，我会明确告知。'
};

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value));
const selectedKnowledgeBaseName = computed(() => knowledgeBases.value.find((item) => item.id === selectedKnowledgeBase.value)?.name || '全部知识库');
const isSending = computed(() => sendState.value === 'loading');
const isLoadingList = computed(() => listState.value === 'loading');
const isLoadingMessages = computed(() => messageState.value === 'loading');
const canSend = computed(() => Boolean(question.value.trim()) && !isSending.value && chatEndpointAvailable);
const oldestMessageId = computed(() => messages.value.find((item) => item.id)?.id);

function normalizeRole(role) {
  const value = String(role || 'assistant').toLowerCase();
  return value === 'user' ? 'user' : 'assistant';
}

function normalizeSources(sources) {
  if (!Array.isArray(sources)) return [];
  return sources
    .map((source) => ({
      ...source,
      index: Number(source?.index),
      knowledgeBaseName: source?.knowledgeBaseName || source?.knowledge_base_name || '',
      fileName: source?.fileName || source?.file_name || '',
      pageNumber: normalizePageNumber(source)
    }))
    .filter((source) => Number.isInteger(source.index));
}

function normalizePageNumber(source) {
  const value = source?.pageNumber ?? source?.pageNo ?? source?.page_number;
  if (value === null || value === undefined || value === '') return null;

  const numberValue = Number(value);
  return Number.isInteger(numberValue) ? numberValue : null;
}

function getVisibleCitationCount(message) {
  if (message?.role !== 'assistant' || !Array.isArray(message.sources) || !message.sources.length) {
    return 0;
  }

  const availableIndexes = new Set(message.sources.map((source) => Number(source.index)));
  const visibleIndexes = new Set();
  const content = String(message.content || '');
  const citePattern = /\[--cite:(\d+)]/g;
  let match;

  while ((match = citePattern.exec(content)) !== null) {
    const index = Number(match[1]);
    if (availableIndexes.has(index)) {
      visibleIndexes.add(index);
    }
  }

  return visibleIndexes.size;
}

function hasCitationMarkers(content) {
  return /\[--cite:\d+]/.test(String(content || ''));
}

function hasPendingCitationSources() {
  return messages.value.some((message) => (
    message?.role === 'assistant'
    && hasCitationMarkers(message.content)
    && getVisibleCitationCount(message) === 0
  ));
}

function sleep(ms) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
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
    createTime: item.createTime,
    sources: normalizeSources(item.sources)
  };
}

function openSourceDrawer(source) {
  selectedSource.value = source;
  isSourceDrawerOpen.value = true;
}

function closeSourceDrawer() {
  isSourceDrawerOpen.value = false;
}

function hasSourceDocument(source) {
  return Boolean(source?.documentId);
}

function formatScore(value) {
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) return '-';
  return numberValue.toFixed(4);
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

async function loadKnowledgeBases() {
  knowledgeState.value = 'loading';
  knowledgeMessage.value = '';

  try {
    const response = await listKnowledgeBases();
    if (!response.success) throw new Error(response.message || '知识库列表获取失败');
    knowledgeBases.value = response.data || [];
    if (!selectedKnowledgeBase.value && knowledgeBases.value.length) {
      selectedKnowledgeBase.value = knowledgeBases.value[0].id;
    }
    knowledgeState.value = 'success';
  } catch (error) {
    knowledgeState.value = 'error';
    knowledgeMessage.value = error.message || '知识库列表获取失败';
  }
}

function closeHistoryMenu() {
  activeMenuConversationId.value = null;
}

function updateHistoryMenuPosition(button) {
  const rect = button.getBoundingClientRect();
  const menuHeight = 92;
  const gap = 8;
  const spaceBelow = window.innerHeight - rect.bottom;
  const placement = spaceBelow >= menuHeight + gap ? 'down' : 'up';

  historyMenuPosition.value = {
    top: placement === 'down' ? rect.bottom + gap : rect.top - menuHeight - gap,
    left: Math.min(rect.right - 148, window.innerWidth - 164),
    placement
  };
}

function toggleHistoryMenu(conversationId, event) {
  if (activeMenuConversationId.value === conversationId) {
    closeHistoryMenu();
    return;
  }

  updateHistoryMenuPosition(event.currentTarget);
  activeMenuConversationId.value = conversationId;
}

function openRenameDialog(conversation) {
  if (!conversation) return;
  renameConversationId.value = conversation.id;
  renameConversationTitle.value = conversation.title || '新对话';
  renameDialogOpen.value = true;
  closeHistoryMenu();
}

function openDeleteDialog(conversation) {
  if (!conversation) return;
  deleteConversationId.value = conversation.id;
  deleteConversationTitle.value = conversation.title || '新对话';
  deleteDialogOpen.value = true;
  closeHistoryMenu();
}

function resetRenameDialog() {
  renameDialogOpen.value = false;
  renameConversationId.value = null;
  renameConversationTitle.value = '';
  renameConversationSaving.value = false;
}

function resetDeleteDialog() {
  deleteDialogOpen.value = false;
  deleteConversationId.value = null;
  deleteConversationTitle.value = '';
  deleteConversationSaving.value = false;
}

async function submitRenameConversation() {
  if (!renameConversationId.value || renameConversationSaving.value) return;

  const title = String(renameConversationTitle.value || '').trim();
  if (!title) {
    messageNotice.value = '会话标题不能为空';
    return;
  }

  renameConversationSaving.value = true;
  messageNotice.value = '';

  try {
    const response = await renameConversation({
      conversationId: renameConversationId.value,
      title
    });
    if (!response.success) throw new Error(response.message || '修改会话标题失败');

    await loadConversations({ preserveMessages: true });
    resetRenameDialog();
  } catch (error) {
    messageNotice.value = error.message || '修改会话标题失败';
  } finally {
    renameConversationSaving.value = false;
  }
}

async function submitDeleteConversation() {
  if (!deleteConversationId.value || deleteConversationSaving.value) return;

  const targetId = deleteConversationId.value;
  const wasActive = activeConversationId.value === targetId;
  deleteConversationSaving.value = true;
  messageNotice.value = '';

  try {
    const response = await deleteConversation(targetId);
    if (!response.success) throw new Error(response.message || '删除会话失败');

    conversations.value = conversations.value.filter((item) => item.id !== targetId);
    if (wasActive) {
      activeConversationId.value = null;
      startNewConversation();
    } else {
      await loadConversations({ preserveMessages: true });
    }
    resetDeleteDialog();
  } catch (error) {
    messageNotice.value = error.message || '删除会话失败';
  } finally {
    deleteConversationSaving.value = false;
  }
}

async function loadConversations({ preferLatest = false, preserveMessages = false } = {}) {
  listState.value = 'loading';
  listMessage.value = '';

  try {
    const response = await fetchConversationList();
    if (!response.success) throw new Error(response.message || '获取会话列表失败');

    conversations.value = (response.data || []).map(normalizeConversation);
    if (!conversations.value.length) {
      activeConversationId.value = null;
      if (!preserveMessages) {
        messages.value = [welcomeMessage];
      }
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

async function loadConversationMessages(
  conversationId,
  { beforeId, appendBefore = false, preserveMessages = false, expectedAssistantReply = '' } = {}
) {
  if (!conversationId) {
    if (!preserveMessages) {
      messages.value = [welcomeMessage];
    }
    hasMoreMessages.value = false;
    return false;
  }

  messageState.value = 'loading';
  messageNotice.value = '';

  try {
    const response = await fetchConversationMessages({ conversationId, beforeId, limit: 20 });
    if (!response.success) throw new Error(response.message || '获取会话消息失败');

    const nextMessages = (response.data || []).map(normalizeMessage);
    hasMoreMessages.value = nextMessages.length >= 20;

    if (expectedAssistantReply) {
      const hasPersistedReply = nextMessages.some((message) => (
        message.role === 'assistant'
        && message.content === expectedAssistantReply
      ));
      if (!hasPersistedReply) {
        return false;
      }
    }

    if (appendBefore) {
      messages.value = [...nextMessages, ...messages.value];
      return true;
    }

    if (nextMessages.length) {
      messages.value = nextMessages;
      await scrollToBottom();
      return true;
    }

    if (!preserveMessages) {
      messages.value = [welcomeMessage];
    }
    if (!appendBefore) await scrollToBottom();
    return false;
  } catch (error) {
    messageNotice.value = error.message || '获取会话消息失败';
    if (!appendBefore && !preserveMessages) messages.value = [welcomeMessage];
    return false;
  } finally {
    messageState.value = 'idle';
  }
}

async function refreshConversationMessagesAfterSend(conversationId, { preferLatest = false, expectedAssistantReply = '' } = {}) {
  let targetConversationId = conversationId;
  const delays = [1500, 3000, 8000, 15000];
  for (const delay of delays) {
    if (delay > 0) {
      await sleep(delay);
    }

    if (!targetConversationId) {
      await loadConversations({ preferLatest, preserveMessages: true });
      targetConversationId = activeConversationId.value;
      if (!targetConversationId) {
        continue;
      }
    }

    const refreshed = await loadConversationMessages(targetConversationId, {
      preserveMessages: true,
      expectedAssistantReply
    });
    if (refreshed && !hasPendingCitationSources()) {
      break;
    }
  }
}

const chooseConversation = async (conversation) => {
  if (isSending.value || conversation.id === activeConversationId.value) return;
  closeHistoryMenu();
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
  closeHistoryMenu();
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
  if (!conversationIdBeforeSend && messages.value.length === 1 && !messages.value[0]?.id) {
    messages.value = [];
  }
  messages.value.push({ role: 'user', content, sources: [] });
  messages.value.push({ role: 'assistant', content: '', sources: [] });
  const assistantIndex = messages.value.length - 1;
  question.value = '';
  sendState.value = 'loading';
  sendMessage.value = '正在请求 AI 对话接口...';
  await scrollToBottom();

  try {
    const response = await askQuestion({
      message: content,
      conversationId: conversationIdBeforeSend,
      knowledgeBaseId: selectedKnowledgeBase.value,
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

    await loadConversations({ preferLatest: !conversationIdBeforeSend, preserveMessages: true });
    await refreshConversationMessagesAfterSend(activeConversationId.value, {
      preferLatest: !conversationIdBeforeSend,
      expectedAssistantReply: messages.value[assistantIndex].content
    });
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

let removeDocumentClickListener = () => {};
let historyMenuPointerDownInside = false;

onMounted(async () => {
  const isHistoryMenuTarget = (target) => (
    target instanceof Element
    && (target.closest('.history-menu') || target.closest('.history-dropdown'))
  );

  const onDocumentPointerDown = (event) => {
    const target = event.target;
    historyMenuPointerDownInside = isHistoryMenuTarget(target);
  };

  const onDocumentPointerUp = (event) => {
    const target = event.target;
    const pointerUpInside = isHistoryMenuTarget(target);
    if (!historyMenuPointerDownInside && !pointerUpInside) {
      closeHistoryMenu();
    }
    historyMenuPointerDownInside = false;
  };

  const onDocumentPointerCancel = () => {
    historyMenuPointerDownInside = false;
  };

  document.addEventListener('pointerdown', onDocumentPointerDown);
  document.addEventListener('pointerup', onDocumentPointerUp);
  document.addEventListener('pointercancel', onDocumentPointerCancel);
  removeDocumentClickListener = () => {
    document.removeEventListener('pointerdown', onDocumentPointerDown);
    document.removeEventListener('pointerup', onDocumentPointerUp);
    document.removeEventListener('pointercancel', onDocumentPointerCancel);
  };

  await Promise.all([
    loadKnowledgeBases(),
    loadConversations()
  ]);
  if (activeConversationId.value) {
    await loadConversationMessages(activeConversationId.value);
  } else {
    messages.value = [welcomeMessage];
  }
});

onBeforeUnmount(() => {
  removeDocumentClickListener();
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

        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          :class="['history-item', { active: conversation.id === activeConversationId }]"
        >
          <button type="button" class="history-item-main" @click="chooseConversation(conversation)">
            <span class="history-title">{{ conversation.title }}</span>
            <span class="history-preview">{{ conversation.preview }}</span>
            <span class="history-time">{{ conversation.time }}</span>
          </button>
          <button
            type="button"
            class="history-more-button history-menu"
            aria-label="更多操作"
            @click.stop="toggleHistoryMenu(conversation.id, $event)"
          >
            ⋯
          </button>
        </div>
      </aside>

      <article class="chat-panel card">
        <div class="chat-toolbar">
          <div>
            <h3>{{ activeConversation?.title || '新的问答会话' }}</h3>
            <p class="muted">当前知识库：{{ selectedKnowledgeBaseName }}</p>
            <p v-if="knowledgeMessage" class="error compact-notice">{{ knowledgeMessage }}</p>
          </div>
          <label class="knowledge-select-field">
            <span>问答知识库</span>
            <select v-model="selectedKnowledgeBase" :disabled="knowledgeState === 'loading' || !knowledgeBases.length">
              <option :value="null">全部知识库</option>
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
            <span v-if="getVisibleCitationCount(message)" class="retrieval-summary">
              本次回答召回了{{ getVisibleCitationCount(message) }}条数据库知识
            </span>
            <CitationMarkdown
              :content="message.content"
              :sources="message.sources"
              @open-source="openSourceDrawer"
            />
          </div>
        </div>

        <div class="composer-card">
          <div v-if="uploadedFiles.length" class="composer-file-bubbles" aria-label="已上传文件">
            <div v-for="file in uploadedFiles" :key="file.id" class="file-chip floating">
              <span>FILE</span>
              <strong>{{ file.name }}</strong>
              <small>{{ formatSize(file.size) }}</small>
              <button type="button" aria-label="移除文件" @click="removeFile(file.id)">x</button>
            </div>
          </div>
          <textarea v-model="question" rows="5" placeholder="在这里输入你的问题，Ctrl+Enter 发送。" @keydown.ctrl.enter.prevent="sendQuestion" />
          <div class="composer-actions">
            <input ref="fileInput" class="sr-only" type="file" multiple @change="handleFileChange" />
            <button type="button" class="button secondary" @click="openFilePicker">上传文件</button>
            <p :class="sendState === 'error' ? 'error' : sendState === 'success' ? 'success' : 'muted'">{{ sendMessage }}</p>
            <button type="button" :disabled="!canSend" @click="sendQuestion">{{ isSending ? '发送中...' : '发送问题' }}</button>
          </div>
        </div>
      </article>
    </div>

    <div v-if="isSourceDrawerOpen" class="source-drawer-mask" @click.self="closeSourceDrawer">
      <aside class="source-drawer" aria-label="引用来源详情">
        <div class="source-drawer-head">
          <div>
            <span class="status-pill">引用 [{{ selectedSource?.index }}]</span>
            <h3>来源详情</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭来源详情" @click="closeSourceDrawer">x</button>
        </div>

        <dl class="source-meta">
          <div>
            <dt>知识库</dt>
            <dd>{{ selectedSource?.knowledgeBaseName || '-' }}</dd>
          </div>
          <div>
            <dt>文件名</dt>
            <dd>{{ selectedSource?.fileName || '-' }}</dd>
          </div>
          <div>
            <dt>页码</dt>
            <dd>{{ selectedSource?.pageNumber || '-' }}</dd>
          </div>
          <div>
            <dt>相似度</dt>
            <dd>{{ formatScore(selectedSource?.score) }}</dd>
          </div>
        </dl>

        <div class="source-quote">
          <h4>引用片段</h4>
          <p>{{ selectedSource?.quoteText || '暂无片段内容' }}</p>
        </div>
        <a
          v-if="hasSourceDocument(selectedSource)"
          class="button secondary source-document-link"
          :href="getDocumentFileUrl(selectedSource.documentId)"
        >
          查看文档
        </a>
      </aside>
    </div>

    <Teleport to="body">
      <div
        v-if="activeMenuConversationId"
        :class="['history-dropdown', `is-${historyMenuPosition.placement}`]"
        :style="{ top: `${historyMenuPosition.top}px`, left: `${historyMenuPosition.left}px` }"
        @pointerdown.stop
      >
        <button
          type="button"
          class="history-dropdown-item"
          @click="openRenameDialog(conversations.find((item) => item.id === activeMenuConversationId))"
        >
          修改标题
        </button>
        <button
          type="button"
          class="history-dropdown-item danger"
          @click="openDeleteDialog(conversations.find((item) => item.id === activeMenuConversationId))"
        >
          删除对话
        </button>
      </div>
    </Teleport>

    <div v-if="renameDialogOpen" class="dialog-mask" @click.self="resetRenameDialog">
      <div class="dialog-card" role="dialog" aria-modal="true" aria-labelledby="rename-dialog-title">
        <div class="dialog-head">
          <div>
            <p class="status-pill">修改标题</p>
            <h3 id="rename-dialog-title">编辑会话名称</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭修改标题弹窗" @click="resetRenameDialog">x</button>
        </div>
        <label class="dialog-field">
          <span class="muted">会话标题</span>
          <input
            v-model="renameConversationTitle"
            type="text"
            maxlength="255"
            placeholder="请输入新的会话标题"
            @keydown.enter.prevent="submitRenameConversation"
          />
        </label>
        <div class="dialog-actions">
          <button type="button" class="button secondary" @click="resetRenameDialog">取消</button>
          <button type="button" :disabled="renameConversationSaving" @click="submitRenameConversation">
            {{ renameConversationSaving ? '保存中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteDialogOpen" class="dialog-mask" @click.self="resetDeleteDialog">
      <div class="dialog-card" role="dialog" aria-modal="true" aria-labelledby="delete-dialog-title">
        <div class="dialog-head">
          <div>
            <p class="status-pill danger-pill">删除对话</p>
            <h3 id="delete-dialog-title">确认删除「{{ deleteConversationTitle }}」？</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭删除确认弹窗" @click="resetDeleteDialog">x</button>
        </div>
        <p class="muted">删除后会话与消息将从历史记录中移除，且无法恢复。</p>
        <div class="dialog-actions">
          <button type="button" class="button secondary" @click="resetDeleteDialog">取消</button>
          <button type="button" class="danger-button" :disabled="deleteConversationSaving" @click="submitDeleteConversation">
            {{ deleteConversationSaving ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
