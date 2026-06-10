<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getHomePathByRole, login, saveSession } from '../api/auth';

const route = useRoute();
const router = useRouter();
const form = ref({ username: '', password: '' });
const result = ref(null);
const loading = ref(false);

const redirectPath = computed(() => {
  const redirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect;
  return redirect && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '';
});

const submit = async () => {
  if (!form.value.username || !form.value.password || loading.value) return;

  loading.value = true;
  try {
    result.value = await login(form.value);
    if (result.value?.success && result.value?.data?.username) {
      saveSession(result.value.data);
      await router.push(redirectPath.value || getHomePathByRole(result.value.data.role));
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
        <h2>账号登录</h2>
        <p class="muted">所有业务界面均需先登录；登录成功后会按账号角色进入用户端或管理员端，避免不同职责内容混在一起。</p>
      </div>
    </div>

    <div class="grid">
      <article class="card login-card">
        <h3>登录到 AOO AI</h3>
        <p class="muted">请输入企业账号信息。</p>
        <input v-model="form.username" autocomplete="username" placeholder="用户名" @keyup.enter="submit" />
        <input v-model="form.password" autocomplete="current-password" type="password" placeholder="密码" @keyup.enter="submit" />
        <button :disabled="loading || !form.username || !form.password" @click="submit">
          {{ loading ? '登录中...' : '登录并进入工作区' }}
        </button>

        <p v-if="result?.success" class="success">
          登录成功：{{ result.data.displayName }}（{{ result.data.role }}）
        </p>
        <p v-else-if="result" class="error">
          登录失败：{{ result.message }}
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
        <p class="muted">当前前端仅使用登录返回的角色字段进行界面分流；正式 RBAC 接入后可扩展为角色列表和细粒度权限。</p>
      </article>
    </div>
  </section>
</template>
