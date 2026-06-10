<script setup>
import { ref } from 'vue';

const question = ref('');
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>智能问答</h2>
        <p class="muted">用于面向企业知识发起问题。问答接口未接入前，输入区仅保留为产品界面。</p>
      </div>
      <span class="status-pill">问答接口未接入</span>
    </div>

    <div class="grid">
      <article class="card wide-card">
        <h3>提出问题</h3>
        <p class="muted">接入检索与生成接口后，这里将返回答案、引用来源和置信提示。</p>
        <textarea v-model="question" rows="7" placeholder="请输入业务问题，例如：某项流程应该如何办理？" />
        <button disabled>发送问题</button>
      </article>
      <article class="card">
        <h3>回答设置</h3>
        <select disabled>
          <option>全部可访问知识库</option>
        </select>
        <select disabled>
          <option>标准回答</option>
        </select>
        <p class="muted">知识范围、回答风格和追问策略将在接口接入后生效。</p>
      </article>
    </div>

    <article class="card">
      <div class="section-header">
        <div>
          <h3>会话</h3>
          <p class="muted">真实会话产生后会显示在这里。</p>
        </div>
      </div>
      <div class="empty-state">
        <div>
          <div class="empty-icon">✦</div>
          <h3>暂无会话内容</h3>
          <p class="muted">等待问答接口接入后，你可以在这里查看回答、来源文档和后续追问。</p>
        </div>
      </div>
    </article>
  </section>
</template>
