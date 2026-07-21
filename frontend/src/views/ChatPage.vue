<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import CitationMarkdown from '../components/CitationMarkdown.vue';
import {
  askQuestion,
  chatEndpointAvailable,
  deleteConversation,
  fetchConversationList,
  fetchConversationMessages,
  regenerateAnswer,
  renameConversation,
  updateConversationFavorite
} from '../api/qa';
import { getDocumentFileUrl, listKnowledgeBases } from '../api/knowledge';

const question = ref('');
const route = useRoute();
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
const conversationCollection = ref('all');
const regeneratingMessageId = ref(null);

const welcomeMessage = {
  role: 'assistant',
  content: '你好，我是 AOO AI。我会根据知识库内容回答你的问题；若知识库中没有相关信息，我会明确告知。',
  sources: []
};

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value));
const selectedKnowledgeBaseName = computed(() => knowledgeBases.value.find((item) => item.id === selectedKnowledgeBase.value)?.name || '全部知识库');
const isSending = computed(() => sendState.value === 'loading');
const isRegenerating = computed(() => regeneratingMessageId.value !== null);
const isLoadingList = computed(() => listState.value === 'loading');
const isLoadingMessages = computed(() => messageState.value === 'loading');
const canSend = computed(() => Boolean(question.value.trim()) && !isSending.value && !isRegenerating.value && chatEndpointAvailable);
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
  const sources = getDisplaySources(message);
  if (message?.role !== 'assistant' || !Array.isArray(sources) || !sources.length) {
    return 0;
  }

  const availableIndexes = new Set(sources.map((source) => Number(source.index)));
  const visibleIndexes = new Set();
  const content = String(getDisplayContent(message));
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
    && hasCitationMarkers(getDisplayContent(message))
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
    isFavorite: Boolean(item.isFavorite ?? item.favorite),
    raw: item
  };
}

function normalizeMessage(item) {
  const sources = normalizeSources(item.sources);
  const content = item.content || '';
  return {
    id: item.id,
    parentMessageId: item.parentMessageId ?? item.parent_message_id ?? null,
    role: normalizeRole(item.role),
    content,
    createTime: item.createTime,
    sources,
    replyVersions: normalizeRole(item.role) === 'assistant'
      ? [{ id: item.id, content, sources }]
      : [],
    activeReplyIndex: 0
  };
}

function groupAssistantReplyVersions(nextMessages) {
  const groupedMessages = [];
  const assistantByParentId = new Map();

  for (const message of nextMessages) {
    if (message.role !== 'assistant' || !message.parentMessageId) {
      groupedMessages.push(message);
      continue;
    }

    const existing = assistantByParentId.get(message.parentMessageId);
    if (!existing) {
      assistantByParentId.set(message.parentMessageId, message);
      groupedMessages.push(message);
      continue;
    }

    existing.replyVersions.push({
      id: message.id,
      content: message.content,
      sources: message.sources
    });
    existing.activeReplyIndex = existing.replyVersions.length - 1;
    existing.id = message.id;
    existing.content = message.content;
    existing.sources = message.sources;
  }

  return groupedMessages;
}

function getReplyVersions(message) {
  if (message?.role !== 'assistant') return [];
  if (Array.isArray(message.replyVersions) && message.replyVersions.length) {
    return message.replyVersions;
  }
  return [{
    id: message?.id,
    content: message?.content || '',
    sources: Array.isArray(message?.sources) ? message.sources : []
  }];
}

function getActiveReplyIndex(message) {
  const versions = getReplyVersions(message);
  if (!versions.length) return 0;
  const index = Number(message?.activeReplyIndex);
  if (!Number.isInteger(index)) return versions.length - 1;
  return Math.min(Math.max(index, 0), versions.length - 1);
}

function getDisplayContent(message) {
  if (message?.role !== 'assistant') return message?.content || '';
  return getReplyVersions(message)[getActiveReplyIndex(message)]?.content || '';
}

function getDisplaySources(message) {
  if (message?.role !== 'assistant') return Array.isArray(message?.sources) ? message.sources : [];
  return getReplyVersions(message)[getActiveReplyIndex(message)]?.sources || [];
}

function getDisplayMessageId(message) {
  if (message?.role !== 'assistant') return message?.id;
  return getReplyVersions(message)[getActiveReplyIndex(message)]?.id || message?.id;
}

function switchReplyVersion(message, direction) {
  const versions = getReplyVersions(message);
  if (versions.length < 2) return;
  const nextIndex = getActiveReplyIndex(message) + direction;
  message.activeReplyIndex = Math.min(Math.max(nextIndex, 0), versions.length - 1);
}

function mergeReplyVersions(nextMessages) {
  const currentById = new Map(messages.value.filter((message) => message?.id).map((message) => [message.id, message]));
  return nextMessages.map((message) => {
    const current = currentById.get(message.id);
    if (message.role !== 'assistant' || !current?.replyVersions?.length) return message;

    const versions = current.replyVersions.map((version) => ({ ...version }));
    const activeIndex = getActiveReplyIndex(current);
    const nextContent = message.content || '';
    const nextSources = message.sources || [];
    const existingIndex = versions.findIndex((version) => version.content === nextContent);

    if (existingIndex >= 0) {
      versions[existingIndex] = { id: message.id, content: nextContent, sources: nextSources };
      message.activeReplyIndex = existingIndex;
    } else if (versions[activeIndex]?.content === nextContent || !versions[activeIndex]?.content) {
      versions[activeIndex] = { id: message.id, content: nextContent, sources: nextSources };
      message.activeReplyIndex = activeIndex;
    } else {
      versions.push({ id: message.id, content: nextContent, sources: nextSources });
      message.activeReplyIndex = versions.length - 1;
    }

    message.replyVersions = versions;
    return message;
  });
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
    const routeKnowledgeBaseId = Number(route.query.knowledgeBaseId);
    const routeKnowledgeBaseExists = knowledgeBases.value.some((item) => item.id === routeKnowledgeBaseId);
    if (routeKnowledgeBaseExists) {
      selectedKnowledgeBase.value = routeKnowledgeBaseId;
    } else if (!selectedKnowledgeBase.value && knowledgeBases.value.length) {
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
    const response = await fetchConversationList({ favoriteOnly: conversationCollection.value === 'favorites' });
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
    const routeConversationId = Number(route.query.conversationId);
    const routeConversationExists = conversations.value.some((item) => item.id === routeConversationId);
    if (routeConversationExists) {
      activeConversationId.value = routeConversationId;
    } else if (preferLatest || !currentStillExists) {
      activeConversationId.value = conversations.value[0].id;
    }
  } catch (error) {
    listMessage.value = error.message || '获取会话列表失败';
  } finally {
    listState.value = 'idle';
  }
}

async function switchConversationCollection(collection) {
  if (conversationCollection.value === collection) return;
  closeHistoryMenu();
  conversationCollection.value = collection;
  await loadConversations();
  if (activeConversationId.value) {
    await loadConversationMessages(activeConversationId.value);
  }
}

async function toggleFavoriteConversation(conversation) {
  if (!conversation) return;

  const nextFavorite = !conversation.isFavorite;
  const previousFavorite = conversation.isFavorite;
  conversation.isFavorite = nextFavorite;
  messageNotice.value = '';
  closeHistoryMenu();

  try {
    const response = await updateConversationFavorite({
      conversationId: conversation.id,
      favorite: nextFavorite
    });
    if (!response.success) throw new Error(response.message || (nextFavorite ? '收藏会话失败' : '取消收藏失败'));

    if (conversationCollection.value === 'favorites' && !nextFavorite) {
      conversations.value = conversations.value.filter((item) => item.id !== conversation.id);
      if (activeConversationId.value === conversation.id) {
        activeConversationId.value = conversations.value[0]?.id || null;
        await loadConversationMessages(activeConversationId.value);
      }
    }
  } catch (error) {
    conversation.isFavorite = previousFavorite;
    messageNotice.value = error.message || (nextFavorite ? '收藏会话失败' : '取消收藏失败');
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

    let nextMessages = groupAssistantReplyVersions((response.data || []).map(normalizeMessage));
    if (preserveMessages) {
      nextMessages = mergeReplyVersions(nextMessages);
    }
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
  conversationCollection.value = 'all';
  activeConversationId.value = null;
  messages.value = [{
    role: 'assistant',
    content: '已开始新的问答会话，请输入你的问题。',
    sources: []
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
  if (!question.value.trim() || isSending.value || isRegenerating.value) return;

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

    if (!conversationIdBeforeSend && conversationCollection.value === 'favorites') {
      conversationCollection.value = 'all';
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

const regenerateMessage = async (message, index) => {
  const targetMessageId = getDisplayMessageId(message);
  if (!activeConversationId.value || !targetMessageId || isSending.value || isRegenerating.value) return;

  const previousContent = message.content;
  const previousSources = message.sources;
  const previousVersions = getReplyVersions(message).map((version) => ({ ...version }));
  const previousActiveReplyIndex = getActiveReplyIndex(message);
  const nextVersionIndex = previousVersions.length;
  message.replyVersions = [...previousVersions, { id: null, content: '', sources: [] }];
  message.activeReplyIndex = nextVersionIndex;
  regeneratingMessageId.value = message.id;
  message.content = '';
  message.sources = [];
  sendState.value = 'loading';
  sendMessage.value = '正在重新生成 AI 回复...';
  messageNotice.value = '';
  await scrollToBottom();

  try {
    const response = await regenerateAnswer({
      conversationId: activeConversationId.value,
      messageId: targetMessageId,
      knowledgeBaseId: selectedKnowledgeBase.value,
      onDelta: (_token, fullReply) => {
        messages.value[index].replyVersions[nextVersionIndex].content = fullReply;
        messages.value[index].content = fullReply;
        scrollToBottom();
      }
    });
    if (!response.success) throw new Error(response.message || '重新生成失败');
    const finalReply = response.data?.reply || messages.value[index].content;
    messages.value[index].replyVersions[nextVersionIndex].content = finalReply;
    messages.value[index].content = finalReply;
    sendState.value = 'success';
    sendMessage.value = 'AI 已重新生成。';

    await refreshConversationMessagesAfterSend(activeConversationId.value, {
      preserveMessages: true,
      expectedAssistantReply: messages.value[index].content
    });
  } catch (error) {
    message.content = previousContent;
    message.sources = previousSources;
    message.replyVersions = previousVersions;
    message.activeReplyIndex = previousActiveReplyIndex;
    sendState.value = 'error';
    sendMessage.value = error.message || '重新生成失败，请稍后重试';
  } finally {
    regeneratingMessageId.value = null;
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

        <div class="history-collections" aria-label="问答收藏集筛选">
          <button
            type="button"
            :class="{ active: conversationCollection === 'all' }"
            @click="switchConversationCollection('all')"
          >
            全部
          </button>
          <button
            type="button"
            :class="{ active: conversationCollection === 'favorites' }"
            @click="switchConversationCollection('favorites')"
          >
            收藏集
          </button>
        </div>

        <p v-if="listMessage" class="error">{{ listMessage }}</p>
        <div v-else-if="!conversations.length && !isLoadingList" class="empty-upload muted">
          {{ conversationCollection === 'favorites' ? '暂无收藏对话。' : '暂无历史会话，发送第一条消息后会自动创建。' }}
        </div>

        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          :class="['history-item', { active: conversation.id === activeConversationId }]"
        >
          <button type="button" class="history-item-main" @click="chooseConversation(conversation)">
            <span class="history-title">
              <span v-if="conversation.isFavorite" class="history-favorite-mark">★</span>{{ conversation.title }}
            </span>
            <span class="history-preview">{{ conversation.preview }}</span>
            <span class="history-time">{{ conversation.time }}</span>
          </button>
          <button
            type="button"
            :class="['history-favorite-button', { active: conversation.isFavorite }]"
            :aria-label="conversation.isFavorite ? '取消收藏对话' : '收藏对话'"
            :title="conversation.isFavorite ? '取消收藏' : '收藏'"
            @click.stop="toggleFavoriteConversation(conversation)"
          >
            {{ conversation.isFavorite ? '★' : '☆' }}
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

          <div v-for="(message, index) in messages" :key="message.id || index" :class="['message-stack', message.role]">
            <div :class="['message-bubble', message.role]">
              <span class="message-role">{{ message.role === 'user' ? '我' : 'AOO AI' }}</span>
              <span v-if="getVisibleCitationCount(message)" class="retrieval-summary">
                本次回答召回了{{ getVisibleCitationCount(message) }}条数据库知识
              </span>
              <CitationMarkdown
                :content="getDisplayContent(message)"
                :sources="getDisplaySources(message)"
                @open-source="openSourceDrawer"
              />
            </div>
            <div v-if="message.role === 'assistant' && message.id" class="message-actions">
              <div v-if="getReplyVersions(message).length > 1" class="reply-version-switcher" aria-label="切换回复版本">
                <button
                  type="button"
                  class="reply-version-button"
                  :disabled="getActiveReplyIndex(message) === 0 || isSending || isRegenerating"
                  aria-label="上一条回复"
                  title="上一条回复"
                  @click="switchReplyVersion(message, -1)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m15 18-6-6 6-6" />
                  </svg>
                </button>
                <span class="reply-version-count">
                  {{ getActiveReplyIndex(message) + 1 }}/{{ getReplyVersions(message).length }}
                </span>
                <button
                  type="button"
                  class="reply-version-button"
                  :disabled="getActiveReplyIndex(message) === getReplyVersions(message).length - 1 || isSending || isRegenerating"
                  aria-label="下一条回复"
                  title="下一条回复"
                  @click="switchReplyVersion(message, 1)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m9 18 6-6-6-6" />
                  </svg>
                </button>
              </div>
              <button
                type="button"
                :class="['message-action-button', { loading: regeneratingMessageId === message.id }]"
                :disabled="isSending || isRegenerating"
                :aria-label="regeneratingMessageId === message.id ? '重新生成中' : '重新生成'"
                :title="regeneratingMessageId === message.id ? '重新生成中' : '重新生成'"
                @click="regenerateMessage(message, index)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M21 12a9 9 0 0 1-15.3 6.4" />
                  <path d="M3 12A9 9 0 0 1 18.3 5.6" />
                  <path d="M18 2v4h-4" />
                  <path d="M6 22v-4h4" />
                </svg>
              </button>
            </div>
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
