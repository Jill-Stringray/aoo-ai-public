<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchSessionUser, logoutSession } from './api/auth';
import AuthPage from './views/AuthPage.vue';

const currentUser = ref(null);
const sessionChecked = ref(false);

const route = useRoute();
const router = useRouter();

const currentView = computed(() => route.name ?? 'home');

onMounted(async () => {
  try {
    currentUser.value = await fetchSessionUser();
  } finally {
    sessionChecked.value = true;
  }
});

watch(
  [sessionChecked, currentUser],
  ([checked, user]) => {
    if (checked && !user && route.name !== 'auth') {
      router.replace({ name: 'auth' });
      return;
    }
    if (checked && user && route.name === 'auth') {
      router.replace({ name: 'home' });
    }
  },
  { immediate: true }
);

const onLoggedIn = (user) => {
  currentUser.value = user;
  router.replace({ name: 'home' });
};

const logout = async () => {
  try {
    await logoutSession();
  } catch {
    /* 忽略网络错误，仍退出本地状态 */
  }
  currentUser.value = null;
  router.replace({ name: 'auth' });
};

const startChatWithRole = () => {
  router.push({ name: 'chat' });
};

const goTo = (name) => {
  if (name !== route.name) {
    router.push({ name });
  }
};
</script>

<template>
  <div v-if="!sessionChecked" class="app-boot">加载中…</div>
  <AuthPage v-else-if="!currentUser" @logged-in="onLoggedIn" />
  <main v-else class="portal-layout">
    <aside class="portal-nav">
      <nav class="portal-tabs" aria-label="页面导航">
        <button :class="['portal-tab', { active: currentView === 'home' }]" @click="goTo('home')">主页</button>
        <button :class="['portal-tab', { active: currentView === 'chat' }]" @click="goTo('chat')">聊天页面</button>
        <button :class="['portal-tab', { active: currentView === 'workshop' }]" @click="goTo('workshop')">角色工坊</button>
        <button :class="['portal-tab', { active: currentView === 'profile' }]" @click="goTo('profile')">我的</button>
      </nav>
      <button type="button" class="btn-ghost portal-logout" @click="logout">退出登录</button>
    </aside>

    <router-view v-slot="{ Component }">
      <component :is="Component" :user="currentUser" :user-name="currentUser?.userName ?? ''" @start-chat="startChatWithRole" @logout="logout" />
    </router-view>
  </main>
</template>
