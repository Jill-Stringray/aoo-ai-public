<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
  <div class="layout">
    <header class="topbar">
      <h1>企业级 AI 知识库平台（脚手架）</h1>
      <nav>
        <RouterLink to="/">概览</RouterLink>
        <RouterLink to="/knowledge">知识库</RouterLink>
        <RouterLink to="/chat">问答</RouterLink>
        <RouterLink to="/admin">管理台</RouterLink>
      </nav>
    </header>
    <main><RouterView /></main>
  </div>
</template>
