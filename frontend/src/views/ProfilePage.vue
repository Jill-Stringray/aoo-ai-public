<script setup>
defineProps({
  userName: {
    type: String,
    default: ''
  }
});
</script>

<template>
  <section class="profile-page">
    <h2>我的</h2>
    <p class="muted">你好，{{ userName }}。这里是你的个人中心。</p>

    <div class="profile-grid">
      <article class="profile-card">
        <h3>账号信息</h3>
        <p>用户名：{{ userName || '未设置' }}</p>
      </article>

      <article class="profile-card">
        <h3>学习目标</h3>
        <p>建议把本周目标拆成可执行任务：需求分析、编码实践、复盘总结。</p>
      </article>

      <article class="profile-card">
        <h3>最近使用</h3>
        <p>你可以从「聊天页面」继续对话，或在「角色工坊」管理角色卡。</p>
      </article>
    </div>
  </section>
</template>
