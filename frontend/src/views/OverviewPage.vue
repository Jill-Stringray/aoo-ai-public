<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>今日工作台</h2>
        <p class="muted">这里聚合已接入的业务入口；未接入的数据保持为空，不再展示模拟内容。</p>
      </div>
      <RouterLink class="button secondary" to="/knowledge">管理知识库</RouterLink>
    </div>

    <div class="grid">
      <article class="card">
        <p class="eyebrow">Knowledge</p>
        <h3>知识资产</h3>
        <p class="metric">—</p>
        <p class="muted">等待知识库接口接入后展示文档、索引和更新时间。</p>
      </article>
      <article class="card">
        <p class="eyebrow">Assistant</p>
        <h3>问答服务</h3>
        <p class="metric">—</p>
        <p class="muted">等待问答接口接入后展示最近会话与可追溯来源。</p>
      </article>
      <article class="card">
        <p class="eyebrow">Governance</p>
        <h3>系统治理</h3>
        <p class="metric">—</p>
        <p class="muted">等待监控接口接入后展示任务、审计和成本指标。</p>
      </article>
    </div>

    <div class="empty-state">
      <div>
        <div class="empty-icon">⌁</div>
        <h3>暂无业务动态</h3>
        <p class="muted">完成知识库、问答或治理接口接入后，工作台会显示真实的待办、告警和最近活动。</p>
      </div>
    </div>
  </section>
</template>
