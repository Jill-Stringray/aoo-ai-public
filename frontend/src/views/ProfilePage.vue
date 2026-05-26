<script setup>
import { onMounted, ref } from 'vue';
import { fetchMyProfile } from '../api/auth';

const props = defineProps({
  user: {
    type: Object,
    default: null
  }
});

const loading = ref(false);
const error = ref('');
const profile = ref(null);

const loadProfile = async () => {
  loading.value = true;
  error.value = '';
  try {
    profile.value = await fetchMyProfile();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '获取个人中心信息失败';
  } finally {
    loading.value = false;
  }
};

onMounted(loadProfile);
</script>

<template>
  <section class="profile-page">
    <h2>我的</h2>
    <p class="muted">你好，{{ profile?.userName || props.user?.userName || '用户' }}。这里是你的个人中心。</p>

    <p v-if="loading" class="muted">正在加载个人中心数据…</p>
    <p v-else-if="error" class="auth-error">{{ error }}</p>

    <div v-else class="profile-grid">
      <article class="profile-card">
        <h3>余额</h3>
        <p class="profile-balance">¥ {{ ((profile?.balance ?? props.user?.balance ?? 0) / 100).toFixed(2) }}</p>
        <p class="muted">数据来源：后端接口 /me</p>
      </article>

      <article class="profile-card profile-card-wide">
        <h3>个人消息</h3>
        <p class="muted">暂无新消息</p>
      </article>
    </div>
  </section>
</template>
