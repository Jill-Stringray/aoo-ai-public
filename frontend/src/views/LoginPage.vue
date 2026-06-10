<script setup>
import { ref } from 'vue';
import { login } from '../api/auth';

const form = ref({ username: '', password: '' });
const result = ref(null);
const loading = ref(false);

const submit = async () => {
  if (!form.value.username || !form.value.password || loading.value) return;

  loading.value = true;
  try {
    result.value = await login(form.value);
    if (result.value?.success && result.value?.data?.username) {
      localStorage.setItem('aoo_user', result.value.data.username);
      localStorage.setItem('aoo_user_profile', JSON.stringify(result.value.data));
    }
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>账号登录</h2>
        <p class="muted">登录接口已接入，可使用后端账号进入系统。</p>
      </div>
    </div>

    <div class="grid">
      <article class="card">
        <h3>登录到 AOO AI</h3>
        <p class="muted">请输入企业账号信息。</p>
        <input v-model="form.username" autocomplete="username" placeholder="用户名" />
        <input v-model="form.password" autocomplete="current-password" type="password" placeholder="密码" />
        <button :disabled="loading || !form.username || !form.password" @click="submit">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <p v-if="result?.success" class="success">
          登录成功：{{ result.data.displayName }}（{{ result.data.role }}）
        </p>
        <p v-else-if="result" class="error">
          登录失败：{{ result.message }}
        </p>
      </article>

      <article class="card">
        <h3>账号能力</h3>
        <ul class="list">
          <li><span>登录认证</span><span class="status-pill">已接入</span></li>
          <li><span>注册</span><span class="status-pill">接口可用</span></li>
          <li><span>权限与令牌</span><span class="status-pill">待完善</span></li>
        </ul>
      </article>
    </div>
  </section>
</template>
