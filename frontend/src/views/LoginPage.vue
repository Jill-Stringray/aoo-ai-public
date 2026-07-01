<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getHomePathByRole, login, register, saveSession } from '../api/auth';

const route = useRoute();
const router = useRouter();
const mode = ref(route.query.mode === 'register' ? 'register' : 'login');
const loginForm = ref({ username: '', password: '' });
const registerForm = ref({ username: '', password: '', confirmPassword: '', displayName: '', email: '', mobile: '' });
const result = ref(null);
const loading = ref(false);

const redirectPath = computed(() => {
  const redirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect;
  return redirect && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '';
});

const canLogin = computed(() => Boolean(loginForm.value.username && loginForm.value.password));
const canRegister = computed(() => Boolean(
  registerForm.value.username
  && registerForm.value.password.length >= 6
  && registerForm.value.password === registerForm.value.confirmPassword
));

const switchMode = (nextMode) => {
  mode.value = nextMode;
  result.value = null;
};

const submitLogin = async () => {
  if (!canLogin.value || loading.value) return;

  loading.value = true;
  try {
    result.value = await login(loginForm.value);
    if (result.value?.success && result.value?.data?.username) {
      saveSession(result.value.data);
      await router.push(redirectPath.value || getHomePathByRole(result.value.data.role));
    }
  } finally {
    loading.value = false;
  }
};

const submitRegister = async () => {
  if (!canRegister.value || loading.value) return;

  loading.value = true;
  try {
    const { confirmPassword, ...payload } = registerForm.value;
    result.value = await register(payload);
    if (result.value?.success) {
      loginForm.value = {
        username: registerForm.value.username,
        password: registerForm.value.password
      };
      mode.value = 'login';
    }
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <section class="page-section login-hero">
    <div class="section-header">
      <div>
        <p class="eyebrow">Entry First</p>
        <h2>账号登录 / 注册</h2>
        <p class="muted">所有业务界面均需先登录；新用户可先注册企业账号，再按账号角色进入用户端或管理员端。</p>
      </div>
    </div>

    <div class="grid">
      <article class="card login-card">
        <div class="auth-tabs" role="tablist" aria-label="账号入口">
          <button type="button" :class="['auth-tab', { active: mode === 'login' }]" @click="switchMode('login')">登录</button>
          <button type="button" :class="['auth-tab', { active: mode === 'register' }]" @click="switchMode('register')">注册</button>
        </div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
          <h3>登录到 AOO AI</h3>
          <p class="muted">请输入企业账号信息。</p>
          <input v-model="loginForm.username" autocomplete="username" placeholder="用户名" />
          <input v-model="loginForm.password" autocomplete="current-password" type="password" placeholder="密码" />
          <button :disabled="loading || !canLogin" type="submit">
            {{ loading ? '登录中...' : '登录并进入工作区' }}
          </button>
        </form>

        <form v-else class="auth-form" @submit.prevent="submitRegister">
          <h3>创建新账号</h3>
          <p class="muted">用户名和密码必填，展示名、邮箱与手机号可选。</p>
          <input v-model="registerForm.username" autocomplete="username" placeholder="用户名（必填）" />
          <input v-model="registerForm.displayName" autocomplete="name" placeholder="展示姓名（可选）" />
          <input v-model="registerForm.email" autocomplete="email" type="email" placeholder="邮箱（可选）" />
          <input v-model="registerForm.mobile" autocomplete="tel" placeholder="手机号（可选）" />
          <input v-model="registerForm.password" autocomplete="new-password" type="password" placeholder="密码（至少 6 位）" />
          <input v-model="registerForm.confirmPassword" autocomplete="new-password" type="password" placeholder="确认密码" />
          <p v-if="registerForm.confirmPassword && registerForm.password !== registerForm.confirmPassword" class="error hint">两次输入的密码不一致</p>
          <button :disabled="loading || !canRegister" type="submit">
            {{ loading ? '注册中...' : '注册账号' }}
          </button>
        </form>

        <p v-if="result?.success" class="success">
          {{ result.message }}<span v-if="result.data?.displayName">：{{ result.data.displayName }}</span>
        </p>
        <p v-else-if="result" class="error">
          操作失败：{{ result.message }}
        </p>
      </article>

      <article class="card identity-card">
        <h3>身份职责分区</h3>
        <ul class="list">
          <li>
            <span>用户端</span>
            <span class="status-pill">工作台 / 问答 / 个人中心</span>
          </li>
          <li>
            <span>管理员端</span>
            <span class="status-pill">治理 / 知识库管理</span>
          </li>
          <li>
            <span>访问控制</span>
            <span class="status-pill">未登录不可查看</span>
          </li>
        </ul>
        <p class="muted">注册成功后会自动回填登录表单；正式 RBAC 接入后可扩展为角色列表和细粒度权限。</p>
      </article>
    </div>
  </section>
</template>
