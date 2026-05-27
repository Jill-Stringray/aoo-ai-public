<template>
  <section>
    <h2>运维治理看板（产品化占位）</h2>
    <div class="grid">
      <article class="card">
        <h3>索引任务状态</h3>
        <p>待解析：12</p><p>向量化中：3</p><p>失败重试：1</p>
      </article>
      <article class="card">
        <h3>Token 成本观察</h3>
        <p>今日请求：1,284</p><p>平均输入：682 tokens</p><p>平均输出：214 tokens</p>
      </article>
      <article class="card">
        <h3>权限审计</h3>
        <p>租户：3</p><p>知识库：17</p><p>最近越权拦截：0</p>
      </article>
    </div>
    <p class="muted">这些数值当前是静态演示，后续后端逐步替换为真实指标接口。</p>
  </section>
</template>
