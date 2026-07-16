<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchAdminUsers, fetchSystemConfig, saveSystemConfig, updateUserStatus } from '../api/admin';

const users = ref([]);
const usersState = ref('idle');
const usersMessage = ref('');
const configState = ref('idle');
const configMessage = ref('');
const savingUserId = ref(null);
const config = ref({
  embeddingModel: '',
  llmModel: '',
  chunkSize: 800,
  chunkOverlap: 80,
  topK: 5,
  similarityThreshold: 0.41,
  rerankEnabled: false
});

const totalUsers = computed(() => users.value.length);
const activeUsers = computed(() => users.value.filter((item) => item.status === 'ACTIVE').length);
const disabledUsers = computed(() => users.value.filter((item) => item.status === 'DISABLED').length);
const totalQuestions = computed(() => users.value.reduce((sum, item) => sum + Number(item.usage?.questionCount || 0), 0));
const totalTokens = computed(() => users.value.reduce((sum, item) => sum + Number(item.usage?.totalTokens || 0), 0));

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN');
}

async function loadUsers() {
  usersState.value = 'loading';
  usersMessage.value = '';
  try {
    const response = await fetchAdminUsers();
    if (!response.success) throw new Error(response.message || '系统用户列表获取失败');
    users.value = response.data || [];
    usersState.value = 'success';
  } catch (error) {
    usersState.value = 'error';
    usersMessage.value = error.message || '系统用户列表获取失败';
  }
}

async function loadConfig() {
  configState.value = 'loading';
  configMessage.value = '';
  try {
    const response = await fetchSystemConfig();
    if (!response.success) throw new Error(response.message || '系统配置获取失败');
    config.value = { ...config.value, ...(response.data || {}) };
    configState.value = 'success';
  } catch (error) {
    configState.value = 'error';
    configMessage.value = error.message || '系统配置获取失败';
  }
}

async function toggleUserStatus(user) {
  if (!user || savingUserId.value) return;
  const nextStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  savingUserId.value = user.id;
  usersMessage.value = '';
  try {
    const response = await updateUserStatus({ userId: user.id, status: nextStatus });
    if (!response.success) throw new Error(response.message || '用户状态更新失败');
    user.status = nextStatus;
  } catch (error) {
    usersMessage.value = error.message || '用户状态更新失败';
  } finally {
    savingUserId.value = null;
  }
}

async function submitConfig() {
  configState.value = 'saving';
  configMessage.value = '';
  try {
    const payload = {
      ...config.value,
      chunkSize: Number(config.value.chunkSize),
      chunkOverlap: Number(config.value.chunkOverlap),
      topK: Number(config.value.topK),
      similarityThreshold: Number(config.value.similarityThreshold),
      rerankEnabled: Boolean(config.value.rerankEnabled)
    };
    const response = await saveSystemConfig(payload);
    if (!response.success) throw new Error(response.message || '系统配置保存失败');
    config.value = response.data || payload;
    configState.value = 'success';
    configMessage.value = '系统配置已保存，后续召回和切分任务将使用新参数。';
  } catch (error) {
    configState.value = 'error';
    configMessage.value = error.message || '系统配置保存失败';
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadConfig()]);
});
</script>

<template>
  <section class="page-section admin-console-page">
    <div class="section-header">
      <div>
        <h2>管理员控制台</h2>
        <p class="muted">集中管理系统用户、使用情况和 RAG 运行参数。</p>
      </div>
      <span class="status-pill">管理员端</span>
    </div>

    <div class="admin-metric-grid">
      <article class="admin-metric-card">
        <span>系统用户</span>
        <strong>{{ totalUsers }}</strong>
        <p>ACTIVE {{ activeUsers }} / DISABLED {{ disabledUsers }}</p>
      </article>
      <article class="admin-metric-card">
        <span>提问次数</span>
        <strong>{{ formatNumber(totalQuestions) }}</strong>
        <p>基于 conversation_message 聚合</p>
      </article>
      <article class="admin-metric-card">
        <span>Token 消耗</span>
        <strong>{{ formatNumber(totalTokens) }}</strong>
        <p>Prompt + Completion 总量</p>
      </article>
      <article class="admin-metric-card">
        <span>召回 TopK</span>
        <strong>{{ config.topK }}</strong>
        <p>阈值 {{ config.similarityThreshold }}</p>
      </article>
    </div>

    <div class="admin-console-layout">
      <article class="card admin-users-card">
        <div class="section-header">
          <div>
            <h3>用户管理</h3>
            <p class="muted">查看系统用户基本信息、状态和使用情况。</p>
          </div>
          <button type="button" class="button secondary compact-button" :disabled="usersState === 'loading'" @click="loadUsers">
            {{ usersState === 'loading' ? '刷新中...' : '刷新用户' }}
          </button>
        </div>
        <p v-if="usersMessage" class="error">{{ usersMessage }}</p>

        <div class="admin-user-table">
          <div class="admin-user-row admin-user-head">
            <span>用户</span>
            <span>联系方式</span>
            <span>状态</span>
            <span>使用情况</span>
            <span>操作</span>
          </div>
          <div v-for="user in users" :key="user.id" class="admin-user-row">
            <span>
              <strong>{{ user.displayName || user.username }}</strong>
              <small>{{ user.username }} · {{ user.roleCode }}</small>
            </span>
            <span>
              <strong>{{ user.email || '-' }}</strong>
              <small>{{ user.mobile || '-' }}</small>
            </span>
            <span :class="['status-pill', user.status === 'ACTIVE' ? 'success-pill' : 'danger-pill']">{{ user.status }}</span>
            <span>
              <strong>{{ formatNumber(user.usage?.questionCount) }} 次提问</strong>
              <small>{{ formatNumber(user.usage?.totalTokens) }} tokens · 最近 {{ formatTime(user.usage?.lastAskAt) }}</small>
            </span>
            <button type="button" class="button secondary compact-button" :disabled="savingUserId === user.id" @click="toggleUserStatus(user)">
              {{ user.status === 'ACTIVE' ? '禁用' : '启用' }}
            </button>
          </div>
          <div v-if="!users.length && usersState !== 'loading'" class="empty-state compact">
            <div>
              <div class="empty-icon">□</div>
              <h3>暂无用户</h3>
              <p class="muted">系统用户创建后会显示在这里。</p>
            </div>
          </div>
        </div>
      </article>

      <article class="card admin-config-card">
        <div class="section-header">
          <div>
            <h3>系统配置管理</h3>
            <p class="muted">调整模型、切分和召回参数。</p>
          </div>
        </div>
        <p v-if="configMessage" :class="configState === 'error' ? 'error' : 'success'">{{ configMessage }}</p>

        <form class="admin-config-form" @submit.prevent="submitConfig">
          <label>
            <span>Embedding 模型</span>
            <input v-model="config.embeddingModel" type="text">
          </label>
          <label>
            <span>LLM 模型</span>
            <input v-model="config.llmModel" type="text">
          </label>
          <label>
            <span>Chunk 大小</span>
            <input v-model.number="config.chunkSize" type="number" min="1">
          </label>
          <label>
            <span>Chunk overlap</span>
            <input v-model.number="config.chunkOverlap" type="number" min="0">
          </label>
          <label>
            <span>TopK 召回数量</span>
            <input v-model.number="config.topK" type="number" min="1">
          </label>
          <label>
            <span>相似度阈值</span>
            <input v-model.number="config.similarityThreshold" type="number" min="0" max="1" step="0.01">
          </label>
          <label class="config-toggle">
            <span>Rerank 开关</span>
            <input v-model="config.rerankEnabled" type="checkbox">
          </label>
          <button type="submit" :disabled="configState === 'saving'">
            {{ configState === 'saving' ? '保存中...' : '保存配置' }}
          </button>
        </form>
      </article>
    </div>
  </section>
</template>
