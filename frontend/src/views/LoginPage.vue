<script setup>
import { ref } from 'vue';
import { login } from '../api/auth';

const form = ref({ username: '', password: '' });
const result = ref(null);
const loading = ref(false);

const submit = async () => {
  loading.value = true;
  result.value = await login(form.value);
  loading.value = false;

  if (result.value?.success && result.value?.username) {
    localStorage.setItem('demo_user', result.value.username);
  }
};
</script>

<template>
  <section>
    <h2>登录中心</h2>
    <p class="muted">当前已接入后端 /api/v1/auth/login 接口，后续可继续演进为 JWT + 刷新令牌 + 风险校验。</p>
    <div class="card" style="max-width:480px">
      <input v-model="form.username" placeholder="用户名，如 zhangsan" />
      <input v-model="form.password" type="password" placeholder="密码" />
      <button :disabled="loading" @click="submit">{{ loading ? '登录中...' : '登录' }}</button>

      <p v-if="result?.success" class="success">
        登录成功：{{ result.displayName }}（{{ result.role }}）
      </p>
      <p v-else-if="result" class="error">
        登录失败：{{ result.message }}
      </p>
    </div>
  </section>
</template>
