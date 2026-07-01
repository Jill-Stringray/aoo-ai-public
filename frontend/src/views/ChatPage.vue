<script setup>
import { computed, ref } from 'vue';

const question = ref('');
const selectedKnowledgeBase = ref('company-handbook');
const uploadedFiles = ref([]);
const activeConversationId = ref(1);
const fileInput = ref(null);

const knowledgeBases = [
  { id: 'company-handbook', name: '企业制度知识库' },
  { id: 'product-docs', name: '产品与交付文档' },
  { id: 'customer-cases', name: '客户案例库' }
];

const conversations = ref([
  {
    id: 1,
    title: '报销流程怎么走？',
    time: '09:42',
    preview: '需要准备发票、审批单并提交至财务系统。'
  },
  {
    id: 2,
    title: '项目验收材料清单',
    time: '昨天',
    preview: '验收报告、测试记录、客户签收单等材料。'
  },
  {
    id: 3,
    title: '新员工入职安排',
    time: '周一',
    preview: '账号开通、设备领取、导师分配与培训计划。'
  }
]);

const messages = ref([
  { role: 'assistant', content: '你好，我可以结合知识库与临时上传文件回答问题。请选择知识库，或上传多个文件后开始提问。' }
]);

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value));
const selectedKnowledgeBaseName = computed(() => knowledgeBases.find((item) => item.id === selectedKnowledgeBase.value)?.name || '未选择知识库');
const canSend = computed(() => Boolean(question.value.trim()));

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
  messages.value = [{ role: 'assistant', content: '已创建新会话，请选择知识库或上传文件后开始提问。' }];
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

const sendQuestion = () => {
  if (!canSend.value) return;
  const content = question.value.trim();
  messages.value.push({ role: 'user', content });
  messages.value.push({
    role: 'assistant',
    content: `已收到问题，将基于「${selectedKnowledgeBaseName.value}」${uploadedFiles.value.length ? `和 ${uploadedFiles.value.length} 个上传文件` : ''}生成可追溯回答。`
  });

  const active = activeConversation.value;
  if (active) {
    active.title = content.slice(0, 18);
    active.preview = content;
    active.time = '刚刚';
  }
  question.value = '';
};
</script>

<template>
  <section class="page-section chat-page">
    <div class="section-header">
      <div>
        <h2>智能问答</h2>
        <p class="muted">左侧查看历史问答记录，右侧在大对话框内选择知识库、上传多个文件并持续追问。</p>
      </div>
      <span class="status-pill">用户端</span>
    </div>

    <div class="chat-workspace">
      <aside class="chat-history card" aria-label="历史问答记录">
        <div class="history-head">
          <div>
            <h3>历史问答</h3>
            <p class="muted">最近会话记录</p>
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
            <select v-model="selectedKnowledgeBase">
              <option v-for="base in knowledgeBases" :key="base.id" :value="base.id">{{ base.name }}</option>
            </select>
          </label>
        </div>

        <div class="conversation-box" aria-live="polite">
          <div v-for="(message, index) in messages" :key="index" :class="['message-bubble', message.role]">
            <span class="message-role">{{ message.role === 'user' ? '我' : 'AOO AI' }}</span>
            <p>{{ message.content }}</p>
          </div>
        </div>

        <div class="composer-card">
          <textarea v-model="question" rows="5" placeholder="请输入业务问题，例如：某项流程应该如何办理？" @keydown.ctrl.enter.prevent="sendQuestion" />
          <div class="composer-actions">
            <input ref="fileInput" class="sr-only" type="file" multiple @change="handleFileChange" />
            <button type="button" class="button secondary" @click="openFilePicker">上传文件</button>
            <p class="muted">支持一次选择多个文件；文件列表会显示在下方。</p>
            <button type="button" :disabled="!canSend" @click="sendQuestion">发送问题</button>
          </div>
        </div>

        <div class="uploaded-files">
          <div class="section-header compact-header">
            <div>
              <h3>已上传文件</h3>
              <p class="muted">{{ uploadedFiles.length ? `共 ${uploadedFiles.length} 个临时附件` : '尚未上传附件' }}</p>
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
          <div v-else class="empty-upload muted">上传后，附件名称、大小会展示在这里，便于确认本轮问答参考范围。</div>
        </div>
      </article>
    </div>
  </section>
</template>
