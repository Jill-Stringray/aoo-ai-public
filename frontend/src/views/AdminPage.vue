<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>治理中心</h2>
        <p class="muted">用于承载系统监控、审计和成本治理。未接入的指标保持为空。</p>
      </div>
      <span class="status-pill">监控接口未接入</span>
    </div>

    <div class="grid">
      <article class="card">
        <p class="eyebrow">Index</p>
        <h3>索引任务</h3>
        <p class="metric">—</p>
        <p class="muted">接入任务接口后展示排队、处理中和失败任务。</p>
      </article>
      <article class="card">
        <p class="eyebrow">Audit</p>
        <h3>权限审计</h3>
        <p class="metric">—</p>
        <p class="muted">接入审计接口后展示登录、访问和权限变更记录。</p>
      </article>
      <article class="card">
        <p class="eyebrow">Cost</p>
        <h3>资源消耗</h3>
        <p class="metric">—</p>
        <p class="muted">接入用量接口后展示模型调用、Token 和检索成本。</p>
      </article>
    </div>

    <article class="card">
      <h3>告警与待处理事项</h3>
      <div class="empty-state">
        <div>
          <div class="empty-icon">◌</div>
          <h3>暂无告警</h3>
          <p class="muted">真实监控数据接入前不展示模拟告警或固定数值。</p>
        </div>
      </div>
    </article>
  </section>
</template>
