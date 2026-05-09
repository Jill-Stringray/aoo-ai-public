<script setup>
import { onMounted, reactive, ref } from 'vue';
import {
  createConversation,
  deleteConversation as deleteConversationRequest,
  fetchConversationMessages,
  fetchConversations,
  patchConversation,
  postChatMessageStream
} from '../api/chat';
import { renderMarkdown } from '../utils/markdown';

defineProps({
  userName: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['logout']);

const WELCOME = [
  {
    role: 'assistant',
    content:
      '你好。左侧可新建、重命名、删除会话；列表与标题均由服务端维护。',
    localOnly: true
  }
];

const draft = ref('');
const loading = ref(false);
const streaming = ref(false);
const conversationId = ref(null);
const conversations = ref([]);
const convListLoading = ref(false);

const messages = ref([...WELCOME]);

function formatConvTime(iso) {
  if (!iso || typeof iso !== 'string') {
    return '';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return '';
  }
  return d.toLocaleString();
}

const syncHistoryFromServer = async () => {
  if (!conversationId.value) {
    messages.value = [...WELCOME];
    return;
  }
  try {
    const serverMessages = await fetchConversationMessages(conversationId.value);
    if (Array.isArray(serverMessages) && serverMessages.length > 0) {
      messages.value = serverMessages.map((m) => ({
        role: m.role,
        content: m.content ?? ''
      }));
    } else {
      messages.value = [...WELCOME];
    }
  } catch {
    messages.value = [...WELCOME];
  }
};

const loadConversationList = async () => {
  convListLoading.value = true;
  try {
    conversations.value = await fetchConversations();
  } catch {
    conversations.value = [];
  } finally {
    convListLoading.value = false;
  }
};

const selectConversation = async (id) => {
  const n = Number(id);
  if (Number.isNaN(n) || n <= 0) {
    return;
  }
  conversationId.value = n;
  await syncHistoryFromServer();
};

/** 新建空白会话（服务端落库） */
const onCreateConversation = async () => {
  try {
    const created = await createConversation({});
    await loadConversationList();
    const id = created?.id;
    if (id != null) {
      await selectConversation(id);
    }
  } catch (e) {
    window.alert(e?.message ?? '新建会话失败');
  }
};

/** 未选择会话时的本地欢迎态（不进库，直到用户发首条或点新建） */
const clearToWelcome = () => {
  conversationId.value = null;
  messages.value = [...WELCOME];
};

const renameConversation = async (c) => {
  const current = c.title?.trim() || '';
  const next = window.prompt('修改会话标题', current);
  if (next === null) {
    return;
  }
  try {
    await patchConversation(c.id, { title: next });
    await loadConversationList();
  } catch (e) {
    window.alert(e?.message ?? '重命名失败');
  }
};

const removeConversation = async (c) => {
  const label = c.title?.trim() || `会话 #${c.id}`;
  if (!window.confirm(`确定删除「${label}」？`)) {
    return;
  }
  const id = c.id;
  try {
    await deleteConversationRequest(id);
    const wasCurrent = conversationId.value === id;
    await loadConversationList();
    if (wasCurrent) {
      if (conversations.value.length > 0) {
        await selectConversation(conversations.value[0].id);
      } else {
        clearToWelcome();
      }
    }
  } catch (e) {
    window.alert(e?.message ?? '删除失败');
  }
};

onMounted(async () => {
  await loadConversationList();
  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0].id);
  }
});

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
    const assistantDraft = reactive({
      role: 'assistant',
      content: ''
    });
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
          }
        },
        onDelta(delta) {
          const chunk =
            typeof delta === 'string'
              ? delta
              : typeof delta === 'number' || typeof delta === 'boolean'
                ? String(delta)
                : delta?.delta ?? delta?.content ?? delta?.raw ?? '';
          assistantDraft.content += chunk;
        },
        async onDone(done) {
          const id = Number(done?.conversationId ?? done);
          if (!Number.isNaN(id) && id > 0) {
            conversationId.value = id;
          }
          if (!assistantDraft.content) {
            assistantDraft.content = '（模型返回为空）';
          }
          await loadConversationList();
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
  <main class="chat-page chat-page-with-sidebar">
    <aside class="chat-sidebar">
      <h2>我的会话</h2>
      <div class="chat-sidebar-toolbar">
        <button type="button" class="btn-secondary btn-new-chat" @click="onCreateConversation">
          新建会话
        </button>
        <button type="button" class="btn-ghost" @click="clearToWelcome">欢迎页</button>
      </div>
      <div v-if="convListLoading" class="chat-conv-meta">加载列表…</div>
      <div v-else class="chat-sidebar-list">
        <div
          v-for="c in conversations"
          :key="c.id"
          class="chat-conv-row"
          :class="{ active: conversationId === c.id }"
        >
          <button type="button" class="chat-conv-main" @click="selectConversation(c.id)">
            <div class="chat-conv-title">
              {{ c.title?.trim() || `会话 #${c.id}` }}
            </div>
            <div class="chat-conv-meta">{{ formatConvTime(c.updatedAt) }}</div>
          </button>
          <div class="chat-conv-actions">
            <button
              type="button"
              class="chat-conv-action"
              title="重命名"
              @click.stop="renameConversation(c)"
            >
              改名
            </button>
            <button
              type="button"
              class="chat-conv-action danger"
              title="删除"
              @click.stop="removeConversation(c)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </aside>

    <div class="chat-main-col">
      <header class="chat-header chat-header-row">
        <div class="chat-header-main">
          <h1>后端对话测试</h1>
          <p>
            左侧 <strong>新建 / 改名 / 删除</strong> 对应
            <code>POST/PATCH/DELETE /conversations</code>；列表为
            <code>GET /conversations</code>；单条为 <code>GET /conversations/:id</code>；消息仍为
            <code>GET /conversation/:id/messages</code>。
          </p>
        </div>
        <div v-if="userName" class="chat-user-bar">
          <span class="chat-user-name">{{ userName }}</span>
          <button type="button" class="btn-secondary" @click="emit('logout')">退出登录</button>
        </div>
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
            <div
              v-if="item.role === 'assistant'"
              class="message-content markdown-body"
              v-html="renderMarkdown(item.content)"
            ></div>
            <div v-else class="message-content">{{ item.content }}</div>
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
            {{ loading ? (streaming ? '流式输出中...' : '发送中...') : '发送（Ctrl+Enter）' }}
          </button>
        </div>
      </section>
    </div>
  </main>
</template>
