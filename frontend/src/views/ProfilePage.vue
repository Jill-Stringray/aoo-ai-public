<script setup>
import { onMounted, ref } from 'vue';
import { fetchProfile } from '../api/auth';

const username = ref(localStorage.getItem('demo_user') || 'student');
const profile = ref(null);

const load = async () => {
  profile.value = await fetchProfile(username.value);
};

onMounted(load);
</script>

<template>
  <section>
    <h2>个人中心</h2>
    <p class="muted">该页面定义了后端未来需要提供的个人画像、行为轨迹和统计字段。</p>

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
  </section>
</template>
