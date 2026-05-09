<script setup>
import { ref } from 'vue';
import { login, register } from '../api/auth';

const emit = defineEmits(['logged-in']);

const mode = ref('login');
const userName = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

const resetError = () => {
  error.value = '';
};

const submitLogin = async () => {
  resetError();
  loading.value = true;
  try {
    const user = await login(userName.value.trim(), password.value);
    emit('logged-in', user);
  } catch (e) {
    error.value = e.message ?? '登录失败';
  } finally {
    loading.value = false;
  }
};

const submitRegister = async () => {
  resetError();
  loading.value = true;
  try {
    const user = await register(userName.value.trim(), password.value);
    emit('logged-in', user);
  } catch (e) {
    error.value = e.message ?? '注册失败';
  } finally {
    loading.value = false;
  }
};

const onSubmit = () => {
  if (mode.value === 'login') {
    submitLogin();
  } else {
    submitRegister();
  }
};
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <h1>AOO AI</h1>
      <p class="auth-lead">登录或注册后开始对话；登录状态由服务端会话（Cookie）保存。</p>

      <div class="auth-tabs">
        <button
          type="button"
          class="auth-tab"
          :class="{ active: mode === 'login' }"
          @click="mode = 'login'"
        >
          登录
        </button>
        <button
          type="button"
          class="auth-tab"
          :class="{ active: mode === 'register' }"
          @click="mode = 'register'"
        >
          注册
        </button>
      </div>

      <form class="auth-form" @submit.prevent="onSubmit">
        <label class="auth-field">
          <span>用户名</span>
          <input v-model="userName" type="text" autocomplete="username" required />
        </label>
        <label class="auth-field">
          <span>密码</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
          />
        </label>

        <p v-if="error" class="auth-error">{{ error }}</p>

        <button type="submit" class="auth-submit" :disabled="loading">
          {{ loading ? '请稍候…' : mode === 'login' ? '登录' : '注册' }}
        </button>
      </form>
    </section>
  </main>
</template>
