<script setup>
import { ref } from 'vue';
import { login } from '../api/auth';

const form = ref({ username: '', password: '' });
const result = ref(null);

const submit = async () => {
  result.value = await login(form.value);
  if (result.value?.username) {
    localStorage.setItem('demo_user', result.value.username);
  }
};
</script>

<template>
  <section>
    <h2>登录中心</h2>
    <p class="muted">当前为演示登录，后续将替换为 JWT + 刷新令牌 + 风险校验。</p>
    <div class="card" style="max-width:480px">
      <input v-model="form.username" placeholder="用户名，如 zhangsan" />
      <input v-model="form.password" type="password" placeholder="密码" />
      <button @click="submit">登录</button>
      <p v-if="result">登录成功：{{ result.displayName }}（{{ result.role }}）</p>
    </div>
  </section>
</template>
