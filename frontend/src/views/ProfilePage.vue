<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { getCurrentIdentity } from '../api/auth';
import { fetchConversationList, updateConversationFavorite } from '../api/qa';

const identity = computed(() => getCurrentIdentity());
const localProfile = computed(() => identity.value.profile || {});
const conversations = ref([]);
const conversationState = ref('idle');
const conversationMessage = ref('');

const displayName = computed(() => (
  localProfile.value.displayName
  || localProfile.value.username
  || identity.value.username
  || '当前用户'
));
const accountStatus = computed(() => localProfile.value.status || '正常');
const departmentLabel = computed(() => (
  localProfile.value.departmentName
  || localProfile.value.deptName
  || (localProfile.value.departmentId ? `部门 ${localProfile.value.departmentId}` : '未设置')
));
const recentConversations = computed(() => conversations.value.slice(0, 6));

function formatDateTime(value) {
  if (!value) return '暂无记录';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ');
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function normalizeConversation(item) {
  return {
    id: item.conversationId ?? item.id,
    title: item.title || '新对话',
    updateTime: item.updateTime || item.updatedAt || item.createTime,
    isFavorite: Boolean(item.isFavorite ?? item.favorite)
  };
}

async function loadConversations() {
  conversationState.value = 'loading';
  conversationMessage.value = '';

  try {
    const response = await fetchConversationList();
    if (!response.success) throw new Error(response.message || '最近问答获取失败');
    conversations.value = (response.data || []).map(normalizeConversation);
    conversationState.value = 'success';
  } catch (error) {
    conversationState.value = 'error';
    conversationMessage.value = error.message || '最近问答获取失败';
  }
}

async function toggleFavoriteConversation(conversation) {
  if (!conversation) return;

  const nextFavorite = !conversation.isFavorite;
  const previousFavorite = conversation.isFavorite;
  conversation.isFavorite = nextFavorite;
  conversationMessage.value = '';

  try {
    const response = await updateConversationFavorite({
      conversationId: conversation.id,
      favorite: nextFavorite
    });
    if (!response.success) throw new Error(response.message || (nextFavorite ? '收藏会话失败' : '取消收藏失败'));
  } catch (error) {
    conversation.isFavorite = previousFavorite;
    conversationMessage.value = error.message || (nextFavorite ? '收藏会话失败' : '取消收藏失败');
  }
}

onMounted(() => {
  loadConversations();
});
</script>

<template>
  <section class="page-section profile-page">
    <div class="section-header">
      <div>
        <h2>个人中心</h2>
        <p class="muted">查看账号信息和最近问答记录。</p>
      </div>
    </div>

    <div class="profile-layout">
      <article class="card profile-account-card">
        <p class="eyebrow">Account</p>
        <h3>{{ displayName }}</h3>
        <p class="muted">{{ localProfile.username || identity.username }}</p>
        <dl class="profile-detail-list">
          <div>
            <dt>邮箱</dt>
            <dd>{{ localProfile.email || '-' }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ localProfile.phone || '-' }}</dd>
          </div>
          <div>
            <dt>部门</dt>
            <dd>{{ departmentLabel }}</dd>
          </div>
          <div>
            <dt>账号状态</dt>
            <dd>{{ accountStatus }}</dd>
          </div>
        </dl>
      </article>
    </div>

    <div class="profile-content-grid">
      <article class="card">
        <div class="profile-card-head">
          <div>
            <p class="eyebrow">Conversations</p>
            <h3>最近问答记录</h3>
          </div>
          <RouterLink class="button secondary compact-button" to="/chat">进入问答</RouterLink>
        </div>
        <p v-if="conversationMessage" class="error">{{ conversationMessage }}</p>

        <div v-if="recentConversations.length" class="profile-list">
          <div
            v-for="conversation in recentConversations"
            :key="conversation.id"
            class="profile-list-row"
          >
            <RouterLink
              class="profile-list-main"
              :to="{ path: '/chat', query: { conversationId: conversation.id } }"
            >
              <strong>{{ conversation.title }}</strong>
              <small>{{ formatDateTime(conversation.updateTime) }}</small>
            </RouterLink>
            <button
              type="button"
              :class="['history-favorite-button', { active: conversation.isFavorite }]"
              :aria-label="conversation.isFavorite ? '取消收藏对话' : '收藏对话'"
              :title="conversation.isFavorite ? '取消收藏' : '收藏'"
              @click="toggleFavoriteConversation(conversation)"
            >
              {{ conversation.isFavorite ? '★' : '☆' }}
            </button>
          </div>
        </div>
        <div v-else-if="!conversationMessage" class="empty-state compact">
          <p class="muted">{{ conversationState === 'loading' ? '正在加载最近问答...' : '暂无问答记录。' }}</p>
        </div>
      </article>
    </div>
  </section>
</template>
