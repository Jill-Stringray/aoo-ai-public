<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchProfile, login } from '../api/auth';

const form = ref({ username: '', password: '' });
const loginResult = ref(null);
const profile = ref(null);

const currentUsername = ref(localStorage.getItem('demo_user') || '');
const isLoggedIn = computed(() => !!currentUsername.value);

const loadProfile = async () => {
  if (!isLoggedIn.value) {
    profile.value = null;
    return;
  }
  profile.value = await fetchProfile(currentUsername.value);
};

const submit = async () => {
  loginResult.value = await login(form.value);
  if (loginResult.value?.success && loginResult.value?.data?.username) {
    currentUsername.value = loginResult.value.data.username;
    localStorage.setItem('demo_user', loginResult.value.data.username);
    form.value = { username: '', password: '' };
    await loadProfile();
  }
};

const logout = () => {
  localStorage.removeItem('demo_user');
  currentUsername.value = '';
  profile.value = null;
  loginResult.value = null;
};

onMounted(loadProfile);
</script>

<template>
  <section>
    <h2>个人中心</h2>

    <template v-if="!isLoggedIn">
      <p class="muted">未登录，请先登录后查看个人信息。</p>
      <div class="card" style="max-width:480px">
        <input v-model="form.username" placeholder="用户名，如 zhangsan" />
        <input v-model="form.password" type="password" placeholder="密码" />
        <button @click="submit">登录</button>
        <p v-if="loginResult?.success" class="success">登录成功：{{ loginResult.data.displayName }}（{{ loginResult.data.role }}）</p>
        <p v-else-if="loginResult" class="error">登录失败：{{ loginResult.message }}</p>
      </div>
    </template>

    <template v-else>
      <p class="muted">已登录为 {{ currentUsername }}，这里展示你的个人画像与使用情况。</p>
      <button @click="logout" style="margin-bottom:12px">退出登录</button>

      <div class="grid" v-if="profile">
        <article class="card">
          <h3>基础信息</h3>
          <p>姓名：{{ profile.displayName }}</p>
          <p>用户名：{{ profile.username }}</p>
          <p>邮箱：{{ profile.email }}</p>
          <p>部门：{{ profile.department }}</p>
          <p>角色：{{ profile.role }}</p>
        </article>
        <article class="card">
          <h3>使用概览</h3>
          <p>知识库数量：{{ profile.knowledgeBaseCount }}</p>
          <p>累计会话数：{{ profile.conversationCount }}</p>
        </article>
        <article class="card">
          <h3>最近活动</h3>
          <ul>
            <li v-for="(item, idx) in profile.recentActivities" :key="idx">{{ item }}</li>
          </ul>
        </article>
      </div>
    </template>
  </section>
</template>
