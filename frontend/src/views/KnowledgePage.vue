<script setup>
const upcomingActions = [
  '新建知识库',
  '上传企业文档',
  '查看解析与向量化状态'
];
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>知识库管理</h2>
        <p class="muted">用于维护企业知识来源。当前知识库与文档接口未接入，因此列表保持为空。</p>
      </div>
      <button disabled>新建知识库</button>
    </div>

    <div class="grid">
      <article class="card">
        <h3>知识库</h3>
        <p class="metric">—</p>
        <p class="muted">接口接入后展示可访问的知识库数量。</p>
      </article>
      <article class="card">
        <h3>文档</h3>
        <p class="metric">—</p>
        <p class="muted">接口接入后展示已上传、解析中和失败文档。</p>
      </article>
      <article class="card">
        <h3>索引状态</h3>
        <p class="metric">—</p>
        <p class="muted">接口接入后展示切分、Embedding 与入库进度。</p>
      </article>
    </div>

    <article class="card">
      <div class="section-header">
        <div>
          <h3>知识库列表</h3>
          <p class="muted">真实接口可用前不填充临时数据。</p>
        </div>
        <span class="status-pill">未接入</span>
      </div>
      <div class="empty-state">
        <div>
          <div class="empty-icon">◇</div>
          <h3>暂无知识库</h3>
          <p class="muted">后端知识库接口接入后，可在这里创建、检索和维护企业知识资产。</p>
        </div>
      </div>
    </article>

    <article class="card">
      <h3>待开放操作</h3>
      <ul class="list">
        <li v-for="item in upcomingActions" :key="item">
          <span>{{ item }}</span>
          <span class="status-pill">等待接口</span>
        </li>
      </ul>
    </article>
  </section>
</template>
