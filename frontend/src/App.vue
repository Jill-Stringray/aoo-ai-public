<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { computed } from 'vue';

const route = useRoute();
const pageTitle = computed(() => route.meta.title || 'AOO AI');
const pageDescription = computed(() => route.meta.description || '企业知识智能助手');

const navItems = [
  { to: '/', label: '工作台', icon: '⌘' },
  { to: '/knowledge', label: '知识库', icon: '◇' },
  { to: '/chat', label: '问答', icon: '✦' },
  { to: '/admin', label: '治理', icon: '◌' },
  { to: '/profile', label: '我的', icon: '●' }
];
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/" class="brand" aria-label="回到工作台">
        <span class="brand-mark">A</span>
        <span>
          <strong>AOO AI</strong>
          <small>Knowledge Copilot</small>
        </span>
      </RouterLink>

      <nav class="nav" aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-card">
        <p class="eyebrow">当前空间</p>
        <strong>企业知识中台</strong>
        <span>接口能力按模块逐步接入</span>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div>
          <p class="badge">Enterprise RAG Platform</p>
          <h1>{{ pageTitle }}</h1>
          <p class="subtitle">{{ pageDescription }}</p>
        </div>
        <RouterLink to="/login" class="account-entry">登录 / 注册</RouterLink>
      </header>

      <main class="panel">
        <RouterView />
      </main>
    </div>
  </div>
</template>
