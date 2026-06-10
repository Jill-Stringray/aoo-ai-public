<script setup>
import { computed } from 'vue';
import { getCurrentIdentity } from '../api/auth';

const identity = computed(() => getCurrentIdentity());
const localProfile = computed(() => identity.value.profile);
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>个人中心</h2>
        <p class="muted">用户端账号信息页；未登录访问会先跳转到登录页。</p>
      </div>
    </div>

    <div class="grid" v-if="localProfile">
      <article class="card">
        <p class="eyebrow">Profile</p>
        <h3>{{ localProfile.displayName || identity.username }}</h3>
        <p class="muted">{{ localProfile.username || identity.username }}</p>
        <p>邮箱：{{ localProfile.email || '—' }}</p>
        <p>账号状态：{{ localProfile.status || '—' }}</p>
        <p>身份：{{ identity.isAdmin ? '管理员' : '用户' }}</p>
      </article>
      <article class="card">
        <h3>用户端使用概览</h3>
        <p class="metric">—</p>
        <p class="muted">知识问答、最近会话和个人活动接口接入后展示真实数据。</p>
      </article>
    </div>

    <div v-else class="empty-state">
      <div>
        <div class="empty-icon">●</div>
        <h3>已登录</h3>
        <p class="muted">当前账号：{{ identity.username }}。个人详情接口接入后将在这里展示完整资料。</p>
      </div>
    </div>
  </section>
</template>
