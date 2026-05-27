<script setup>
import { onMounted, ref } from 'vue';
import { createDocument, createKnowledgeBase, listDocuments, listKnowledgeBases } from '../api/knowledge';

const list = ref([]);
const docs = ref([]);
const form = ref({ name: '', description: '', owner: 'student' });
const docForm = ref({ knowledgeBaseId: '', fileName: '', scene: '入职培训资料' });

const load = async () => (list.value = await listKnowledgeBases());
const onCreateKb = async () => {
  await createKnowledgeBase(form.value);
  form.value = { name: '', description: '', owner: 'student' };
  await load();
};
const onLoadDocs = async (id) => {
  docs.value = await listDocuments(id);
  docForm.value.knowledgeBaseId = String(id);
};
const onCreateDoc = async () => {
  await createDocument({ knowledgeBaseId: Number(docForm.value.knowledgeBaseId), fileName: `${docForm.value.scene}-${docForm.value.fileName}` });
  await onLoadDocs(Number(docForm.value.knowledgeBaseId));
  docForm.value.fileName = '';
};
onMounted(load);
</script>

<template>
  <section>
    <h2>知识资产工作台</h2>
    <p class="muted">先定义业务语义，再做技术实现：每份文档都应指向明确业务场景。</p>
    <div class="grid">
      <article class="card">
        <h3>1）创建知识库</h3>
        <input v-model="form.name" placeholder="示例：售后支持知识库" />
        <input v-model="form.description" placeholder="示例：覆盖安装、报警处理、维护手册" />
        <input v-model="form.owner" placeholder="负责人" />
        <button @click="onCreateKb">创建知识库</button>
      </article>
      <article class="card">
        <h3>2）录入文档占位</h3>
        <input v-model="docForm.knowledgeBaseId" placeholder="知识库ID" />
        <select v-model="docForm.scene">
          <option>入职培训资料</option><option>售后排障资料</option><option>财务制度资料</option>
        </select>
        <input v-model="docForm.fileName" placeholder="文件名，如 SOP-v1.pdf" />
        <button @click="onCreateDoc">录入文档</button>
      </article>
    </div>

    <h3>知识库列表</h3>
    <ul>
      <li v-for="item in list" :key="item.id">#{{ item.id }} {{ item.name }}（{{ item.owner }}） - {{ item.description }}
        <button @click="onLoadDocs(item.id)">查看文档</button></li>
    </ul>

    <h3>文档列表</h3>
    <ul>
      <li v-for="doc in docs" :key="doc.id">#{{ doc.id }} {{ doc.fileName }} · 状态：{{ doc.status }}</li>
    </ul>
  </section>
</template>
