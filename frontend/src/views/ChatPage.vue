<script setup>
import { ref } from 'vue';
import { askQuestion } from '../api/qa';

const question = ref('请根据售后手册，给出设备E02报警的标准排查步骤');
const answer = ref('');
const loading = ref(false);

const submit = async () => {
  if (!question.value) return;
  loading.value = true;
  try { answer.value = (await askQuestion(question.value)).answer; }
  finally { loading.value = false; }
};
</script>
<template>
  <section>
    <h2>智能问答演练台</h2>
    <p class="muted">推荐先用“真实业务问题”做提示词，而不是泛化提问。</p>
    <textarea v-model="question" rows="5" />
    <button @click="submit">{{ loading ? '生成中...' : '开始问答' }}</button>
    <article class="card" style="margin-top:10px">
      <h3>模型回答</h3>
      <p>{{ answer || '等待你发起一次提问。' }}</p>
    </article>
  </section>
</template>
