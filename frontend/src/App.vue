<script setup>
import { onMounted, ref } from 'vue';
import { fetchSessionUser, logoutSession } from './api/auth';
import AuthPage from './views/AuthPage.vue';
import ChatPage from './views/ChatPage.vue';

const currentUser = ref(null);
const sessionChecked = ref(false);

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
};
</script>

<template>
  <div v-if="!sessionChecked" class="app-boot">加载中…</div>
  <AuthPage v-else-if="!currentUser" @logged-in="onLoggedIn" />
  <ChatPage v-else :user-name="currentUser.userName ?? ''" @logout="logout" />
</template>
