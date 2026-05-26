<script setup>
import { onMounted, ref } from 'vue';
import { fetchSessionUser, logoutSession } from './api/auth';
import AuthPage from './views/AuthPage.vue';
import ChatPage from './views/ChatPage.vue';
import HomePage from './views/HomePage.vue';
import RoleWorkshopPage from './views/RoleWorkshopPage.vue';
import ProfilePage from './views/ProfilePage.vue';

const currentUser = ref(null);
const sessionChecked = ref(false);
const currentView = ref('home');

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
    <aside class="portal-nav">
      <nav class="portal-tabs" aria-label="页面导航">
        <button :class="['portal-tab', { active: currentView === 'home' }]" @click="currentView = 'home'">主页</button>
        <button :class="['portal-tab', { active: currentView === 'chat' }]" @click="currentView = 'chat'">聊天页面</button>
        <button :class="['portal-tab', { active: currentView === 'workshop' }]" @click="currentView = 'workshop'">角色工坊</button>
        <button :class="['portal-tab', { active: currentView === 'profile' }]" @click="currentView = 'profile'">我的</button>
      </nav>
      <button type="button" class="btn-ghost portal-logout" @click="logout">退出登录</button>
    </aside>

    <HomePage v-if="currentView === 'home'" @start-chat="startChatWithRole" />
    <ChatPage v-else-if="currentView === 'chat'" :user-name="currentUser.userName ?? ''" @logout="logout" />
    <RoleWorkshopPage v-else-if="currentView === 'workshop'" />
    <ProfilePage v-else :user="currentUser" />
  </main>
</template>
