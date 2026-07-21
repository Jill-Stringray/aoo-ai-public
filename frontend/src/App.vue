<script setup>
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { AUTH_EXPIRED_EVENT, clearSession, getCurrentIdentity, resetAuthExpiredNotice } from './api/auth';

const route = useRoute();
const router = useRouter();
const pageTitle = computed(() => route.meta.title || 'AOO AI');
const pageDescription = computed(() => route.meta.description || '企业知识智能助手');
const identity = computed(() => {
  route.fullPath;
  return getCurrentIdentity();
});

const userNavItems = [
  { to: '/', label: '用户工作台', icon: '⌘' },
  { to: '/chat', label: '智能问答', icon: '✦' },
  { to: '/profile', label: '个人中心', icon: '●' }
];

const adminNavItems = [
  { to: '/admin', label: '管理员控制台', icon: '◌' },
  { to: '/admin/knowledge', label: '知识库管理', icon: '◇' },
  { to: '/admin/rag-debug', label: 'RAG检索调试', icon: '⌕' }
];

const loginNavItems = [
  { to: '/login', label: '登录', icon: '→' }
];

const navItems = computed(() => {
  if (!identity.value.authenticated) return loginNavItems;
  return identity.value.isAdmin ? adminNavItems : userNavItems;
});

const roleLabel = computed(() => (identity.value.isAdmin ? '管理员端' : '用户端'));
const displayName = computed(() => identity.value.profile?.displayName || identity.value.username || '未登录');
const authExpiredDialogOpen = ref(false);
const authExpiredMessage = ref('身份认证已过期');

const logout = async () => {
  clearSession();
  resetAuthExpiredNotice();
  await router.push('/login');
};

const handleAuthExpired = async (event) => {
  authExpiredMessage.value = event.detail?.message || '身份认证已过期';
  authExpiredDialogOpen.value = true;
  if (route.path !== '/login') {
    await router.push('/login');
  }
};

const closeAuthExpiredDialog = () => {
  authExpiredDialogOpen.value = false;
};

onMounted(() => {
  window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
});

onUnmounted(() => {
  window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
});
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/login" class="brand" aria-label="进入登录页">
        <span class="brand-mark">A</span>
        <span>
          <strong>AOO AI</strong>
          <small>Knowledge Copilot</small>
        </span>
      </RouterLink>

      <nav class="nav" aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-card">
        <p class="eyebrow">当前空间</p>
        <strong>{{ identity.authenticated ? roleLabel : '请先登录' }}</strong>
        <span>{{ identity.authenticated ? `${displayName} · ${identity.role}` : '登录后按身份进入对应界面' }}</span>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div>
          <p class="badge">Enterprise RAG Platform</p>
          <h1>{{ pageTitle }}</h1>
          <p class="subtitle">{{ pageDescription }}</p>
        </div>
        <div class="account-actions">
          <span v-if="identity.authenticated" class="account-entry muted-entry">{{ displayName }}</span>
          <button v-if="identity.authenticated" class="account-entry as-button" @click="logout">退出登录</button>
          <RouterLink v-else to="/login" class="account-entry">登录 / 注册</RouterLink>
        </div>
      </header>

      <main class="panel">
        <RouterView />
      </main>
    </div>

    <div v-if="authExpiredDialogOpen" class="dialog-mask" @click.self="closeAuthExpiredDialog">
      <div class="dialog-card auth-expired-dialog" role="alertdialog" aria-modal="true" aria-labelledby="auth-expired-title">
        <div class="dialog-head">
          <div>
            <p class="eyebrow">登录状态</p>
            <h3 id="auth-expired-title">{{ authExpiredMessage }}</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭身份认证过期提示" @click="closeAuthExpiredDialog">x</button>
        </div>
        <p class="muted">请重新登录后继续使用。</p>
        <div class="dialog-actions">
          <button type="button" class="primary-action" @click="closeAuthExpiredDialog">我知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>
