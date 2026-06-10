<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>用户工作台</h2>
        <p class="muted">这里聚合普通用户可使用的知识问答入口；管理员治理能力已独立放入管理员端。</p>
      </div>
      <RouterLink class="button secondary" to="/chat">开始问答</RouterLink>
    </div>

    <div class="grid">
      <article class="card">
        <p class="eyebrow">User Knowledge</p>
        <h3>可访问知识</h3>
        <p class="metric">—</p>
        <p class="muted">等待权限与知识库接口接入后展示当前用户可访问的知识范围。</p>
      </article>
      <article class="card">
        <p class="eyebrow">Assistant</p>
        <h3>智能问答</h3>
        <p class="metric">—</p>
        <p class="muted">等待问答接口接入后展示用户最近会话与可追溯来源。</p>
      </article>
      <article class="card">
        <p class="eyebrow">Profile</p>
        <h3>个人效率</h3>
        <p class="metric">—</p>
        <p class="muted">等待个人活动接口接入后展示收藏、历史和待办。</p>
      </article>
    </div>

    <div class="empty-state">
      <div>
        <div class="empty-icon">⌁</div>
        <h3>暂无用户动态</h3>
        <p class="muted">完成问答、会话或个人活动接口接入后，用户工作台会显示真实的最近活动。</p>
      </div>
    </div>
  </section>
</template>
