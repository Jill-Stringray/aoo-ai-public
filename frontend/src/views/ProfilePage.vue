<script setup>
defineProps({
  userName: {
    type: String,
    default: ''
  }
});

const personalMessages = [
  {
    id: 1,
    title: '系统通知',
    content: '你的角色卡「代码评审官」已同步完成。'
  },
  {
    id: 2,
    title: '学习提醒',
    content: '本周建议完成 2 次模拟面试和 1 次项目复盘。'
  },
  {
    id: 3,
    title: '活动消息',
    content: '你已获得一次企业级代码规范专项练习机会。'
  }
];
</script>

<template>
  <section class="profile-page">
    <h2>我的</h2>
    <p class="muted">你好，{{ userName }}。这里是你的个人中心。</p>

    <div class="profile-grid">
      <article class="profile-card">
        <h3>余额</h3>
        <p class="profile-balance">¥ 128.50</p>
        <p class="muted">可用于功能体验与增值服务。</p>
      </article>

      <article class="profile-card">
        <h3>账号信息</h3>
        <p>用户名：{{ userName || '未设置' }}</p>
        <p>账号状态：正常</p>
      </article>

      <article class="profile-card profile-card-wide">
        <h3>个人消息</h3>
        <ul class="profile-message-list">
          <li v-for="message in personalMessages" :key="message.id" class="profile-message-item">
            <strong>{{ message.title }}：</strong>
            <span>{{ message.content }}</span>
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>
