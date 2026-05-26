<script setup>
import { reactive, ref } from 'vue';

const form = reactive({
  name: '',
  style: '',
  intro: '',
  instruction: ''
});

const savedRoles = ref([]);

const saveRole = () => {
  if (!form.name.trim() || !form.instruction.trim()) {
    window.alert('请至少填写角色名称与核心指令');
    return;
  }
  savedRoles.value.unshift({
    id: Date.now(),
    ...form,
    createdAt: new Date().toLocaleString()
  });
  form.name = '';
  form.style = '';
  form.intro = '';
  form.instruction = '';
};
</script>

<template>
  <section class="workshop-page">
    <div class="workshop-grid">
      <form class="workshop-form" @submit.prevent="saveRole">
        <h2>角色工坊</h2>
        <label>
          角色名称
          <input v-model="form.name" type="text" placeholder="例如：Java 架构师导师" />
        </label>
        <label>
          对话风格
          <input v-model="form.style" type="text" placeholder="例如：严谨、结构化、可执行" />
        </label>
        <label>
          角色简介
          <textarea v-model="form.intro" rows="3" placeholder="描述这个角色最适合帮助用户的场景" />
        </label>
        <label>
          核心指令
          <textarea v-model="form.instruction" rows="5" placeholder="例如：所有建议都附带工程实践与风险提示" />
        </label>
        <button type="submit" class="btn-primary">保存角色</button>
      </form>

      <aside class="workshop-list">
        <h3>已创建角色</h3>
        <p v-if="savedRoles.length === 0" class="muted">还没有保存的角色，先创建一个吧。</p>
        <article v-for="role in savedRoles" :key="role.id" class="saved-role">
          <div class="saved-role-title">{{ role.name }}</div>
          <div class="saved-role-meta">{{ role.style || '未设置风格' }} · {{ role.createdAt }}</div>
          <p>{{ role.intro || '（无简介）' }}</p>
          <pre>{{ role.instruction }}</pre>
        </article>
      </aside>
    </div>
  </section>
</template>
