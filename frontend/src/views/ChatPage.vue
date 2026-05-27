<script setup>
import { ref } from 'vue';
import { askQuestion } from '../api/qa';

const question = ref('');
const answer = ref('');

const submit = async () => {
  if (!question.value) return;
  const resp = await askQuestion(question.value);
  answer.value = resp.answer;
};
</script>

<template>
  <section>
    <h2>智能问答</h2>
    <textarea v-model="question" placeholder="输入问题..." rows="5" />
    <br />
    <button @click="submit">提问</button>
    <p><strong>回答：</strong>{{ answer }}</p>
  </section>
</template>
