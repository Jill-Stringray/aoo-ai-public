<script setup>
import { computed, ref } from 'vue';
import { uploadKnowledgeDocument } from '../api/knowledge';

const MAX_FILE_SIZE = 50 * 1024 * 1024;
const acceptedTypes = ['.pdf', '.doc', '.docx', '.txt', '.md'];
const fileInput = ref(null);
const selectedFile = ref(null);
const uploadState = ref('idle');
const uploadMessage = ref('');
const uploadResult = ref(null);
const uploadedDocuments = ref([]);

const isUploading = computed(() => uploadState.value === 'uploading');
const canUpload = computed(() => Boolean(selectedFile.value) && !isUploading.value);
const statusLabel = computed(() => {
  if (isUploading.value) return '处理中';
  if (uploadState.value === 'success') return '已提交';
  if (uploadState.value === 'error') return '需处理';
  return '待上传';
});
const totalDocuments = computed(() => uploadedDocuments.value.length);
const readyDocuments = computed(() => uploadedDocuments.value.filter((item) => item.status === 'READY').length);
const totalChunks = computed(() => uploadedDocuments.value.reduce((sum, item) => sum + item.savedChunkCount, 0));

function formatFileSize(size) {
  if (!size) return '0 B';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function validateFile(file) {
  const extension = `.${file.name.split('.').pop()?.toLowerCase() || ''}`;
  if (!acceptedTypes.includes(extension)) {
    return '当前仅支持 PDF、Word、TXT、Markdown 文档';
  }
  if (file.size > MAX_FILE_SIZE) {
    return '文件不能超过 50MB，请拆分后再上传';
  }
  return '';
}

function setFile(file) {
  uploadResult.value = null;
  if (!file) {
    selectedFile.value = null;
    uploadMessage.value = '';
    uploadState.value = 'idle';
    return;
  }

  const error = validateFile(file);
  if (error) {
    selectedFile.value = null;
    uploadState.value = 'error';
    uploadMessage.value = error;
    return;
  }

  selectedFile.value = file;
  uploadState.value = 'idle';
  uploadMessage.value = '文件已选择，点击上传后将由后端完成存储、解析、切片，并投递 MQ 异步向量化。';
}

function handleFileChange(event) {
  setFile(event.target.files?.[0]);
}

function handleDrop(event) {
  setFile(event.dataTransfer.files?.[0]);
}

function openFilePicker() {
  fileInput.value?.click();
}

async function submitUpload() {
  if (!selectedFile.value) return;

  uploadState.value = 'uploading';
  uploadMessage.value = '正在上传并触发 RAG 入库 MQ 任务，请勿关闭页面。';

  try {
    const response = await uploadKnowledgeDocument(selectedFile.value);
    if (!response.success) {
      throw new Error(response.message || '上传失败，请稍后重试');
    }

    const result = response.data || {};
    const record = {
      id: result.documentId || Date.now(),
      fileName: selectedFile.value.name,
      fileSize: selectedFile.value.size,
      savedChunkCount: result.savedChunkCount || 0,
      indexedChunkCount: result.indexedChunkCount || 0,
      status: 'INDEXING',
      uploadedAt: new Date().toLocaleString('zh-CN', { hour12: false })
    };

    uploadedDocuments.value = [record, ...uploadedDocuments.value];
    uploadResult.value = result;
    uploadState.value = 'success';
    uploadMessage.value = response.message || '上传成功，文档已完成解析切片并提交 MQ 异步索引。';
    selectedFile.value = null;
    if (fileInput.value) fileInput.value.value = '';
  } catch (error) {
    uploadState.value = 'error';
    uploadMessage.value = error.message || '上传失败，请稍后重试';
  }
}
</script>

<template>
  <section class="page-section">
    <div class="section-header">
      <div>
        <h2>知识库管理</h2>
        <p class="muted">管理员在这里上传企业文档；后端会完成文件存储、内容解析、切片，并通过 MQ 异步执行 Embedding 与 RAG 索引入库。</p>
      </div>
      <span class="status-pill">管理员端</span>
    </div>

    <div class="grid">
      <article class="card">
        <h3>文档</h3>
        <p class="metric">{{ totalDocuments }}</p>
        <p class="muted">本次会话已上传并提交处理的企业文档数量。</p>
      </article>
      <article class="card">
        <h3>可检索文档</h3>
        <p class="metric">{{ readyDocuments }}</p>
        <p class="muted">MQ 消费完成后，文档会完成索引并进入问答检索链路。</p>
      </article>
      <article class="card">
        <h3>切片数</h3>
        <p class="metric">{{ totalChunks }}</p>
        <p class="muted">后端解析并保存的 Chunk 数量，用于评估知识覆盖度。</p>
      </article>
    </div>

    <article class="card upload-card">
      <div class="section-header">
        <div>
          <h3>上传企业文档</h3>
          <p class="muted">适合上传制度、流程、产品手册、FAQ 等正式知识来源。请确认文档不包含无授权敏感信息。</p>
        </div>
        <span class="status-pill">{{ statusLabel }}</span>
      </div>

      <div class="upload-dropzone" :class="{ active: selectedFile }" @click="openFilePicker" @dragover.prevent @drop.prevent="handleDrop">
        <input ref="fileInput" class="sr-only" type="file" :accept="acceptedTypes.join(',')" @change="handleFileChange">
        <div class="empty-icon">↑</div>
        <div>
          <h3>{{ selectedFile ? selectedFile.name : '拖拽文件到这里，或点击选择文件' }}</h3>
          <p class="muted">支持 PDF、DOC、DOCX、TXT、MD，单个文件不超过 50MB。</p>
          <p v-if="selectedFile" class="muted">文件大小：{{ formatFileSize(selectedFile.size) }}</p>
        </div>
      </div>

      <div class="upload-actions">
        <button :disabled="!canUpload" @click="submitUpload">{{ isUploading ? '上传处理中…' : '上传并入库' }}</button>
        <p :class="uploadState === 'error' ? 'error' : uploadState === 'success' ? 'success' : 'muted'">{{ uploadMessage || '选择文件后即可提交到后端 RAG 入库接口，并投递 MQ 异步索引。' }}</p>
      </div>

      <div v-if="uploadResult" class="result-grid">
        <span>文档 ID：{{ uploadResult.documentId }}</span>
        <span>保存切片：{{ uploadResult.savedChunkCount }}</span>
        <span>已索引切片：{{ uploadResult.indexedChunkCount }}</span>
      </div>
    </article>

    <article class="card">
      <div class="section-header">
        <div>
          <h3>最近上传</h3>
          <p class="muted">展示当前管理员刚提交的文档，便于确认业务操作是否成功。</p>
        </div>
      </div>

      <div v-if="uploadedDocuments.length" class="document-table">
        <div class="document-row table-head">
          <span>文件名</span>
          <span>大小</span>
          <span>切片</span>
          <span>状态</span>
          <span>时间</span>
        </div>
        <div v-for="item in uploadedDocuments" :key="item.id" class="document-row">
          <span>{{ item.fileName }}</span>
          <span>{{ formatFileSize(item.fileSize) }}</span>
          <span>{{ item.indexedChunkCount }}/{{ item.savedChunkCount }}</span>
          <span class="status-pill">{{ item.status }}</span>
          <span>{{ item.uploadedAt }}</span>
        </div>
      </div>

      <div v-else class="empty-state compact">
        <div>
          <div class="empty-icon">◇</div>
          <h3>暂无上传记录</h3>
          <p class="muted">完成一次上传后，可在这里查看文档 ID、切片数和入库状态。</p>
        </div>
      </div>
    </article>
  </section>
</template>
