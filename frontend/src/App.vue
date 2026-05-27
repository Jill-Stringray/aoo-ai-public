<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
  <div class="app-shell">
    <header class="hero">
      <div>
        <p class="badge">Enterprise RAG Platform</p>
        <h1>企业级 AI 知识库问答平台</h1>
        <p class="subtitle">你当前看到的是“可持续迭代”的产品雏形：先有完整业务画布，再逐步补真实能力。</p>
      </div>
      <nav class="nav">
        <RouterLink to="/">产品蓝图</RouterLink>
        <RouterLink to="/knowledge">知识资产</RouterLink>
        <RouterLink to="/chat">智能问答</RouterLink>
        <RouterLink to="/admin">运维治理</RouterLink>
      </nav>
    </header>
    <main class="panel">
      <RouterView />
    </main>
  </div>
</template>
