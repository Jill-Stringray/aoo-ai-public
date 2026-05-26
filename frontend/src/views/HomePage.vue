<script setup>
const emit = defineEmits(['start-chat']);

const roleCards = [
  {
    name: '产品经理伙伴',
    tag: '需求分析',
    desc: '帮你拆解需求，输出用户故事、优先级和里程碑计划。'
  },
  {
    name: '面试教练',
    tag: '求职提升',
    desc: '模拟技术面试，按 STAR 法给出反馈与改进建议。'
  },
  {
    name: '代码评审官',
    tag: '工程规范',
    desc: '按企业级标准审查命名、结构、测试与可维护性。'
  },
  {
    name: '学习督导',
    tag: '成长路线',
    desc: '根据你的目标生成周计划，并追踪完成进度。'
  }
];
</script>

<template>
  <section class="home-page">
    <div class="hero-card">
      <h2>推荐角色卡</h2>
      <p>选择一个角色开始对话，快速进入具体场景。</p>
    </div>

    <div class="role-grid">
      <article v-for="card in roleCards" :key="card.name" class="role-card">
        <span class="role-tag">{{ card.tag }}</span>
        <h3>{{ card.name }}</h3>
        <p>{{ card.desc }}</p>
        <button type="button" class="btn-primary" @click="emit('start-chat', card)">立即聊天</button>
      </article>
    </div>
  </section>
</template>
