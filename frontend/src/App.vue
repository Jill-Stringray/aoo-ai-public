<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchSessionUser, logoutSession } from './api/auth';
import AuthPage from './views/AuthPage.vue';
import ChatPage from './views/ChatPage.vue';
import HomePage from './views/HomePage.vue';
import RoleWorkshopPage from './views/RoleWorkshopPage.vue';

const currentUser = ref(null);
const sessionChecked = ref(false);
const currentView = ref('home');

const title = computed(() => {
  if (currentView.value === 'chat') return '聊天页面';
  if (currentView.value === 'workshop') return '角色工坊';
  return '推荐角色卡';
});

onMounted(async () => {
  try {
    currentUser.value = await fetchSessionUser();
  } finally {
    sessionChecked.value = true;
  }
});

const onLoggedIn = (user) => {
  currentUser.value = user;
};

const logout = async () => {
  try {
    await logoutSession();
  } catch {
    /* 忽略网络错误，仍退出本地状态 */
  }
  currentUser.value = null;
  currentView.value = 'home';
};

const startChatWithRole = () => {
  currentView.value = 'chat';
};
</script>

<template>
  <div v-if="!sessionChecked" class="app-boot">加载中…</div>
  <AuthPage v-else-if="!currentUser" @logged-in="onLoggedIn" />
  <main v-else class="portal-layout">
    <header class="portal-header">
      <div>
        <h1>AOO AI 创作门户</h1>
        <p>你好，{{ currentUser.userName }}，欢迎来到 {{ title }}</p>
      </div>
      <button type="button" class="btn-ghost" @click="logout">退出登录</button>
    </header>

    <nav class="portal-tabs">
      <button :class="['portal-tab', { active: currentView === 'home' }]" @click="currentView = 'home'">主页</button>
      <button :class="['portal-tab', { active: currentView === 'chat' }]" @click="currentView = 'chat'">聊天页面</button>
      <button :class="['portal-tab', { active: currentView === 'workshop' }]" @click="currentView = 'workshop'">角色工坊</button>
    </nav>

    <HomePage v-if="currentView === 'home'" @start-chat="startChatWithRole" />
    <ChatPage v-else-if="currentView === 'chat'" :user-name="currentUser.userName ?? ''" @logout="logout" />
    <RoleWorkshopPage v-else />
  </main>
</template>
