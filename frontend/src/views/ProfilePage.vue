<script setup>
import { computed, ref } from 'vue';
import { login } from '../api/auth';

const readStoredProfile = () => {
  try {
    return JSON.parse(localStorage.getItem('aoo_user_profile') || 'null');
  } catch {
    return null;
  }
};

const form = ref({ username: '', password: '' });
const loginResult = ref(null);
const loading = ref(false);
const currentUsername = ref(localStorage.getItem('aoo_user') || '');
const localProfile = ref(readStoredProfile());

const isLoggedIn = computed(() => Boolean(currentUsername.value));

const submit = async () => {
  if (!form.value.username || !form.value.password || loading.value) return;

  loading.value = true;
  try {
    loginResult.value = await login(form.value);
    if (loginResult.value?.success && loginResult.value?.data?.username) {
      currentUsername.value = loginResult.value.data.username;
      localProfile.value = loginResult.value.data;
      localStorage.setItem('aoo_user', loginResult.value.data.username);
      localStorage.setItem('aoo_user_profile', JSON.stringify(loginResult.value.data));
      form.value = { username: '', password: '' };
    }
  } finally {
    loading.value = false;
  }
};

const logout = () => {
  localStorage.removeItem('aoo_user');
  localStorage.removeItem('aoo_user_profile');
  currentUsername.value = '';
  localProfile.value = null;
  loginResult.value = null;
};
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>个人中心</h2>
        <p class="muted">查看当前登录账号的基础资料；个人详情接口未接入前仅展示登录返回的信息。</p>
      </div>
      <button v-if="isLoggedIn" @click="logout">退出登录</button>
    </div>

    <template v-if="!isLoggedIn">
      <article class="card" style="max-width: 520px">
        <h3>请先登录</h3>
        <p class="muted">登录后可查看当前账号状态。</p>
        <input v-model="form.username" autocomplete="username" placeholder="用户名" />
        <input v-model="form.password" autocomplete="current-password" type="password" placeholder="密码" />
        <button :disabled="loading || !form.username || !form.password" @click="submit">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p v-if="loginResult?.success" class="success">登录成功：{{ loginResult.data.displayName }}（{{ loginResult.data.role }}）</p>
        <p v-else-if="loginResult" class="error">登录失败：{{ loginResult.message }}</p>
      </article>
    </template>

    <template v-else>
      <div class="grid" v-if="localProfile">
        <article class="card">
          <p class="eyebrow">Profile</p>
          <h3>{{ localProfile.displayName || currentUsername }}</h3>
          <p class="muted">{{ localProfile.username || currentUsername }}</p>
          <p>邮箱：{{ localProfile.email || '—' }}</p>
          <p>部门：{{ localProfile.department || '—' }}</p>
          <p>角色：{{ localProfile.role || '—' }}</p>
        </article>
        <article class="card">
          <h3>使用概览</h3>
          <p class="metric">—</p>
          <p class="muted">知识库、会话和最近活动接口接入后展示真实数据。</p>
        </article>
      </div>

      <div v-else class="empty-state">
        <div>
          <div class="empty-icon">●</div>
          <h3>已登录</h3>
          <p class="muted">当前账号：{{ currentUsername }}。个人详情接口接入后将在这里展示完整资料。</p>
        </div>
      </div>
    </template>
  </section>
</template>
