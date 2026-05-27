<script setup>
import { onMounted, ref } from 'vue';
import { createDocument, createKnowledgeBase, listDocuments, listKnowledgeBases } from '../api/knowledge';

const list = ref([]);
const docs = ref([]);
const form = ref({ name: '', description: '', owner: 'student' });
const docForm = ref({ knowledgeBaseId: '', fileName: '' });

const load = async () => {
  list.value = await listKnowledgeBases();
};

const onCreateKb = async () => {
  await createKnowledgeBase(form.value);
  form.value = { name: '', description: '', owner: 'student' };
  await load();
};

const onLoadDocs = async (knowledgeBaseId) => {
  docs.value = await listDocuments(knowledgeBaseId);
  docForm.value.knowledgeBaseId = String(knowledgeBaseId);
};

const onCreateDoc = async () => {
  await createDocument({
    knowledgeBaseId: Number(docForm.value.knowledgeBaseId),
    fileName: docForm.value.fileName
  });
  await onLoadDocs(Number(docForm.value.knowledgeBaseId));
  docForm.value.fileName = '';
};

onMounted(load);
</script>

<template>
  <section>
    <h2>知识库管理</h2>
    <h3>创建知识库</h3>
    <input v-model="form.name" placeholder="知识库名称" />
    <input v-model="form.description" placeholder="描述" />
    <button @click="onCreateKb">创建</button>

    <h3>知识库列表</h3>
    <ul>
      <li v-for="item in list" :key="item.id">
        #{{ item.id }} {{ item.name }} - {{ item.description }}
        <button @click="onLoadDocs(item.id)">查看文档</button>
      </li>
    </ul>

    <h3>新增文档（占位）</h3>
    <input v-model="docForm.knowledgeBaseId" placeholder="知识库ID" />
    <input v-model="docForm.fileName" placeholder="文件名，如 handbook.pdf" />
    <button @click="onCreateDoc">新增文档</button>

    <h3>文档列表</h3>
    <ul>
      <li v-for="doc in docs" :key="doc.id">#{{ doc.id }} {{ doc.fileName }} [{{ doc.status }}]</li>
    </ul>
  </section>
</template>
