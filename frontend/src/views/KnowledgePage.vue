<script setup>
import { computed, onMounted, ref } from 'vue';
import {
  createKnowledgeBase,
  deleteDocument,
  deleteKnowledgeBase,
  getDocumentFileUrl,
  getKnowledgeStats,
  listDocuments,
  listKnowledgeBases,
  missingKnowledgeEndpoints,
  updateKnowledgeBase,
  uploadKnowledgeDocument
} from '../api/knowledge';

const MAX_FILE_SIZE = 50 * 1024 * 1024;
const acceptedTypes = ['.pdf', '.doc', '.docx', '.txt', '.md'];
const fileInput = ref(null);
const selectedFile = ref(null);
const uploadKnowledgeBaseId = ref(null);
const uploadState = ref('idle');
const uploadMessage = ref('');
const uploadedDocuments = ref([]);
const stats = ref({
  documentCount: 0,
  searchableDocumentCount: 0,
  chunkCount: 0
});
const statsState = ref('idle');
const statsMessage = ref('');
const knowledgeBases = ref([]);
const activeKnowledgeBaseId = ref(null);
const documents = ref([]);
const selectedDocumentContent = ref(null);
const browserState = ref('idle');
const browserMessage = ref('');
const documentState = ref('idle');
const activeKnowledgeMenuId = ref(null);
const activeDocumentMenuId = ref(null);
const renameDialogOpen = ref(false);
const renameKnowledgeBaseId = ref(null);
const renameKnowledgeBaseName = ref('');
const renameSaving = ref(false);
const deleteDialogOpen = ref(false);
const deleteKnowledgeBaseId = ref(null);
const deleteKnowledgeBaseName = ref('');
const deleteSaving = ref(false);
const deleteDocumentDialogOpen = ref(false);
const deleteDocumentId = ref(null);
const deleteDocumentName = ref('');
const deleteDocumentSaving = ref(false);
const createDialogOpen = ref(false);
const createKnowledgeBaseName = ref('');
const createKnowledgeBaseDescription = ref('');
const createKnowledgeBaseVisibility = ref('COMPANY');
const createSaving = ref(false);

const isUploading = computed(() => uploadState.value === 'uploading');
const canUpload = computed(() => Boolean(selectedFile.value) && Boolean(uploadKnowledgeBaseId.value) && !isUploading.value);
const totalDocuments = computed(() => stats.value.documentCount);
const readyDocuments = computed(() => stats.value.searchableDocumentCount);
const totalChunks = computed(() => stats.value.chunkCount);
const pendingDocuments = computed(() => Math.max(totalDocuments.value - readyDocuments.value, 0));
const searchableRate = computed(() => {
  if (!totalDocuments.value) return 0;
  return Math.round((readyDocuments.value / totalDocuments.value) * 100);
});
const chunksPerDocument = computed(() => {
  if (!totalDocuments.value) return 0;
  return Math.round(totalChunks.value / totalDocuments.value);
});
const statusLabel = computed(() => {
  if (statsState.value === 'loading') return '刷新中';
  if (statsState.value === 'error') return '统计异常';
  if (!totalDocuments.value) return '待建设';
  if (pendingDocuments.value) return '需关注';
  return '运行正常';
});
const healthTone = computed(() => {
  if (statsState.value === 'error') return 'danger';
  if (!totalDocuments.value || pendingDocuments.value) return 'warning';
  return 'success';
});
const healthSummary = computed(() => {
  if (statsState.value === 'error') return statsMessage.value || '统计接口暂不可用，请检查后端服务。';
  if (!totalDocuments.value) return '当前知识资产为空，管理员需要先上传正式文档建立可检索数据面。';
  if (pendingDocuments.value) return `有 ${pendingDocuments.value} 份文档尚未进入可检索状态，建议检查解析或索引任务。`;
  return '全部文档处于可检索状态，知识资产可以支撑用户问答。';
});
const governanceItems = computed(() => [
  {
    label: '知识库范围',
    value: `${knowledgeBases.value.length} 个`,
    detail: '管理员可创建多个知识库，用户问答时可按知识库范围检索。'
  },
  {
    label: '数据可见性',
    value: '汇总级',
    detail: '当前只能读取文档/切片统计，无法分页查看所有文档明细。'
  },
  {
    label: '操作闭环',
    value: `${missingKnowledgeEndpoints.length} 类缺口`,
    detail: '新增、分页和状态刷新等治理能力仍需后端接口补齐。'
  }
]);
const endpointStatus = computed(() => [
  { name: '知识资产统计', path: 'GET /api/v1/knowledge/stats', state: '已接入', tone: 'success' },
  { name: '文档上传入库', path: 'POST /api/v1/knowledge/upload', state: '已接入', tone: 'success' },
  ...missingKnowledgeEndpoints.map((item) => ({
    name: item,
    path: '待后端暴露',
    state: '待接入',
    tone: 'danger'
  }))
].filter((item) => item.path !== '/admin/knowledge'));
const activeKnowledgeBase = computed(() => knowledgeBases.value.find((item) => item.id === activeKnowledgeBaseId.value));
async function loadKnowledgeStats() {
  statsState.value = 'loading';
  try {
    const response = await getKnowledgeStats();
    if (!response.success) {
      throw new Error(response.message || '知识库统计获取失败');
    }

    stats.value = {
      documentCount: response.data?.documentCount || 0,
      searchableDocumentCount: response.data?.searchableDocumentCount || 0,
      chunkCount: response.data?.chunkCount || 0
    };
    statsState.value = 'success';
    statsMessage.value = '';
  } catch (error) {
    statsState.value = 'error';
    statsMessage.value = error.message || '知识库统计获取失败，请稍后重试';
  }
}

async function loadKnowledgeBrowser() {
  browserState.value = 'loading';
  browserMessage.value = '';
  try {
    const response = await listKnowledgeBases();
    if (!response.success) {
      throw new Error(response.message || '知识库列表获取失败');
    }

    knowledgeBases.value = response.data || [];
    if (!activeKnowledgeBaseId.value && knowledgeBases.value.length) {
      activeKnowledgeBaseId.value = knowledgeBases.value[0].id;
    }
    if (activeKnowledgeBaseId.value) {
      await loadDocuments(activeKnowledgeBaseId.value);
    } else {
      documents.value = [];
    }
    browserState.value = 'success';
  } catch (error) {
    browserState.value = 'error';
    browserMessage.value = error.message || '知识库数据获取失败';
  }
}

async function loadDocuments(knowledgeBaseId) {
  if (!knowledgeBaseId) return;
  browserState.value = 'loading';
  browserMessage.value = '';
  try {
    const response = await listDocuments(knowledgeBaseId);
    if (!response.success) {
      throw new Error(response.message || '文档列表获取失败');
    }
    documents.value = response.data || [];
    browserState.value = 'success';
  } catch (error) {
    browserState.value = 'error';
    browserMessage.value = error.message || '文档列表获取失败';
  }
}

async function chooseKnowledgeBase(knowledgeBase) {
  if (!knowledgeBase || knowledgeBase.id === activeKnowledgeBaseId.value) return;
  activeKnowledgeBaseId.value = knowledgeBase.id;
  selectedDocumentContent.value = null;
  await loadDocuments(knowledgeBase.id);
}

async function openDocument(document) {
  if (!document?.id) return;
  window.open(getDocumentFileUrl(document.id), '_blank', 'noopener');
}

function closeDocumentViewer() {
  selectedDocumentContent.value = null;
}

function closeKnowledgeMenu() {
  activeKnowledgeMenuId.value = null;
}

function closeDocumentMenu() {
  activeDocumentMenuId.value = null;
}

function toggleKnowledgeMenu(knowledgeBaseId) {
  activeKnowledgeMenuId.value = activeKnowledgeMenuId.value === knowledgeBaseId ? null : knowledgeBaseId;
  closeDocumentMenu();
}

function toggleDocumentMenu(documentId) {
  activeDocumentMenuId.value = activeDocumentMenuId.value === documentId ? null : documentId;
  closeKnowledgeMenu();
}

function startUploadForKnowledgeBase(knowledgeBase) {
  if (!knowledgeBase) return;
  uploadKnowledgeBaseId.value = knowledgeBase.id;
  selectedFile.value = null;
  uploadMessage.value = `将上传到「${knowledgeBase.name}」。请选择文件。`;
  uploadState.value = 'idle';
  closeKnowledgeMenu();
  fileInput.value?.click();
}

function openCreateDialog() {
  createKnowledgeBaseName.value = '';
  createKnowledgeBaseDescription.value = '';
  createKnowledgeBaseVisibility.value = 'COMPANY';
  createDialogOpen.value = true;
  closeKnowledgeMenu();
}

function closeCreateDialog() {
  createDialogOpen.value = false;
  createKnowledgeBaseName.value = '';
  createKnowledgeBaseDescription.value = '';
  createKnowledgeBaseVisibility.value = 'COMPANY';
  createSaving.value = false;
}

async function submitCreateKnowledgeBase() {
  if (createSaving.value) return;
  const name = createKnowledgeBaseName.value.trim();
  if (!name) {
    browserMessage.value = '知识库名称不能为空';
    return;
  }

  createSaving.value = true;
  browserMessage.value = '';
  try {
    const response = await createKnowledgeBase({
      name,
      description: createKnowledgeBaseDescription.value.trim(),
      visibility: createKnowledgeBaseVisibility.value
    });
    if (!response.success) throw new Error(response.message || '新建知识库失败');
    activeKnowledgeBaseId.value = response.data?.id || activeKnowledgeBaseId.value;
    await loadKnowledgeBrowser();
    closeCreateDialog();
  } catch (error) {
    browserMessage.value = error.message || '新建知识库失败';
  } finally {
    createSaving.value = false;
  }
}

function openRenameDialog(knowledgeBase) {
  if (!knowledgeBase) return;
  renameKnowledgeBaseId.value = knowledgeBase.id;
  renameKnowledgeBaseName.value = knowledgeBase.name || '';
  renameDialogOpen.value = true;
  closeKnowledgeMenu();
}

function closeRenameDialog() {
  renameDialogOpen.value = false;
  renameKnowledgeBaseId.value = null;
  renameKnowledgeBaseName.value = '';
  renameSaving.value = false;
}

async function submitRenameKnowledgeBase() {
  if (!renameKnowledgeBaseId.value || renameSaving.value) return;
  const name = renameKnowledgeBaseName.value.trim();
  if (!name) {
    browserMessage.value = '知识库名称不能为空';
    return;
  }

  renameSaving.value = true;
  try {
    const response = await updateKnowledgeBase({
      knowledgeBaseId: renameKnowledgeBaseId.value,
      name
    });
    if (!response.success) throw new Error(response.message || '修改知识库名称失败');
    await loadKnowledgeBrowser();
    closeRenameDialog();
  } catch (error) {
    browserMessage.value = error.message || '修改知识库名称失败';
  } finally {
    renameSaving.value = false;
  }
}

function openDeleteDialog(knowledgeBase) {
  if (!knowledgeBase) return;
  deleteKnowledgeBaseId.value = knowledgeBase.id;
  deleteKnowledgeBaseName.value = knowledgeBase.name || '';
  deleteDialogOpen.value = true;
  closeKnowledgeMenu();
}

function openDeleteDocumentDialog(document) {
  if (!document) return;
  deleteDocumentId.value = document.id;
  deleteDocumentName.value = document.title || document.fileName || '';
  deleteDocumentDialogOpen.value = true;
  closeDocumentMenu();
}

function closeDeleteDocumentDialog() {
  deleteDocumentDialogOpen.value = false;
  deleteDocumentId.value = null;
  deleteDocumentName.value = '';
  deleteDocumentSaving.value = false;
}

async function submitDeleteDocument() {
  if (!deleteDocumentId.value || deleteDocumentSaving.value) return;
  deleteDocumentSaving.value = true;
  try {
    const response = await deleteDocument(deleteDocumentId.value);
    if (!response.success) throw new Error(response.message || '删除文档失败');
    await Promise.all([
      loadKnowledgeStats(),
      activeKnowledgeBaseId.value ? loadDocuments(activeKnowledgeBaseId.value) : Promise.resolve()
    ]);
    closeDeleteDocumentDialog();
  } catch (error) {
    browserMessage.value = error.message || '删除文档失败';
  } finally {
    deleteDocumentSaving.value = false;
  }
}

function closeDeleteDialog() {
  deleteDialogOpen.value = false;
  deleteKnowledgeBaseId.value = null;
  deleteKnowledgeBaseName.value = '';
  deleteSaving.value = false;
}

async function submitDeleteKnowledgeBase() {
  if (!deleteKnowledgeBaseId.value || deleteSaving.value) return;
  deleteSaving.value = true;
  try {
    const response = await deleteKnowledgeBase(deleteKnowledgeBaseId.value);
    if (!response.success) throw new Error(response.message || '删除知识库失败');
    if (activeKnowledgeBaseId.value === deleteKnowledgeBaseId.value) {
      activeKnowledgeBaseId.value = null;
      documents.value = [];
    }
    await Promise.all([
      loadKnowledgeStats(),
      loadKnowledgeBrowser()
    ]);
    closeDeleteDialog();
  } catch (error) {
    browserMessage.value = error.message || '删除知识库失败';
  } finally {
    deleteSaving.value = false;
  }
}

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
  const target = knowledgeBases.value.find((item) => item.id === uploadKnowledgeBaseId.value);
  uploadMessage.value = `文件已选择，将上传到「${target?.name || '当前知识库'}」。`;
}

async function handleFileChange(event) {
  setFile(event.target.files?.[0]);
  if (selectedFile.value && uploadKnowledgeBaseId.value) {
    await submitUpload();
  }
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
  uploadMessage.value = '正在上传并触发 RAG 入库链路，请勿关闭页面。';

  try {
    const response = await uploadKnowledgeDocument(selectedFile.value, uploadKnowledgeBaseId.value);
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
      status: 'READY',
      uploadedAt: new Date().toLocaleString('zh-CN', { hour12: false })
    };

    uploadedDocuments.value = [record, ...uploadedDocuments.value];
    await loadKnowledgeStats();
    await loadKnowledgeBrowser();
    uploadState.value = 'success';
    uploadMessage.value = response.message || '上传成功，文档已完成解析并进入可检索知识库。';
    selectedFile.value = null;
    uploadKnowledgeBaseId.value = null;
    if (fileInput.value) fileInput.value.value = '';
  } catch (error) {
    uploadState.value = 'error';
    uploadMessage.value = error.message || '上传失败，请稍后重试';
  }
}

onMounted(async () => {
  await Promise.all([
    loadKnowledgeStats(),
    loadKnowledgeBrowser()
  ]);
});
</script>

<template>
  <section class="page-section knowledge-admin-page">
    <div class="knowledge-hero">
      <div>
        <p class="eyebrow">Knowledge Operations</p>
        <h2>知识库管理</h2>
        <p class="muted">管理员在这里感知企业知识资产规模、可检索状态、入库链路和治理缺口，并处理正式文档入库。</p>
      </div>
      <div class="hero-actions">
        <span :class="['status-pill', `${healthTone}-pill`]">{{ statusLabel }}</span>
        <button type="button" class="button secondary compact-button" :disabled="statsState === 'loading'" @click="loadKnowledgeStats">
          {{ statsState === 'loading' ? '刷新中...' : '刷新数据' }}
        </button>
      </div>
    </div>

    <article class="admin-signal-card">
      <div>
        <span :class="['signal-dot', healthTone]"></span>
        <h3>系统数据态势</h3>
        <p>{{ healthSummary }}</p>
      </div>
      <div class="signal-meter" aria-label="可检索比例">
        <strong>{{ searchableRate }}%</strong>
        <span>可检索覆盖率</span>
      </div>
    </article>

    <div class="admin-metric-grid">
      <article class="admin-metric-card">
        <span>资产总量</span>
        <strong>{{ totalDocuments }}</strong>
        <p>系统内未删除的企业文档</p>
      </article>
      <article class="admin-metric-card">
        <span>可检索文档</span>
        <strong>{{ readyDocuments }}</strong>
        <p>已完成索引、可支撑问答</p>
      </article>
      <article class="admin-metric-card">
        <span>待处理文档</span>
        <strong>{{ pendingDocuments }}</strong>
        <p>需要关注解析或索引状态</p>
      </article>
      <article class="admin-metric-card">
        <span>知识切片</span>
        <strong>{{ totalChunks }}</strong>
        <p>平均 {{ chunksPerDocument }} 片/文档</p>
      </article>
    </div>

    <article class="card knowledge-browser-card">
      <div class="section-header">
        <div>
          <h3>知识库文件浏览</h3>
          <p class="muted">管理员可以切换知识库、查看库内文件列表，并打开文档切片内容核查数据质量。</p>
        </div>
        <div class="browser-actions">
          <button type="button" class="button secondary compact-button" @click="openCreateDialog">新建知识库</button>
          <button type="button" class="button secondary compact-button" :disabled="browserState === 'loading'" @click="loadKnowledgeBrowser">
            {{ browserState === 'loading' ? '刷新中...' : '刷新列表' }}
          </button>
        </div>
      </div>
      <p v-if="browserMessage" class="error">{{ browserMessage }}</p>
      <input ref="fileInput" class="sr-only" type="file" :accept="acceptedTypes.join(',')" @change="handleFileChange">
      <div class="knowledge-browser">
        <aside class="knowledge-base-list" aria-label="知识库列表">
          <div
            v-for="base in knowledgeBases"
            :key="base.id"
            :class="['knowledge-base-item', { active: base.id === activeKnowledgeBaseId }]"
          >
            <button type="button" class="knowledge-base-main" @click="chooseKnowledgeBase(base)">
              <strong>{{ base.name }}</strong>
              <span>{{ base.description || '暂无描述' }}</span>
              <small>{{ base.status }} · {{ base.visibility }}</small>
            </button>
            <button type="button" class="knowledge-base-more" aria-label="知识库更多操作" @click.stop="toggleKnowledgeMenu(base.id)">⋯</button>
            <div v-if="activeKnowledgeMenuId === base.id" class="knowledge-base-menu">
              <button type="button" @click="startUploadForKnowledgeBase(base)">上传文件</button>
              <button type="button" @click="openRenameDialog(base)">修改知识库名</button>
              <button type="button" class="danger" @click="openDeleteDialog(base)">删除知识库</button>
            </div>
          </div>
          <div v-if="!knowledgeBases.length && browserState !== 'loading'" class="empty-upload muted">
            暂无知识库数据，请先新建知识库。
          </div>
        </aside>

        <section class="knowledge-document-list">
          <div class="document-list-head">
            <div>
              <h4>{{ activeKnowledgeBase?.name || '未选择知识库' }}</h4>
              <p class="muted">{{ documents.length }} 个文件</p>
            </div>
          </div>
          <div v-if="documents.length" :class="['admin-document-table', { 'has-open-menu': activeDocumentMenuId !== null }]">
            <div
              v-for="document in documents"
              :key="document.id"
              class="admin-document-row"
            >
              <button type="button" class="admin-document-main" @click="openDocument(document)">
                <span>
                  <strong>{{ document.title || document.fileName }}</strong>
                  <small>{{ document.fileName }}</small>
                </span>
                <span>{{ document.fileType || '-' }}</span>
                <span>{{ formatFileSize(document.fileSize) }}</span>
                <span :class="['status-pill', document.status === 'READY' ? 'success-pill' : 'warning-pill']">{{ document.status }}</span>
              </button>
              <button type="button" class="knowledge-base-more document-more" aria-label="文档更多操作" @click.stop="toggleDocumentMenu(document.id)">⋯</button>
              <div v-if="activeDocumentMenuId === document.id" class="knowledge-base-menu document-menu">
                <button type="button" @click="openDocument(document)">打开文件</button>
                <button type="button" class="danger" @click="openDeleteDocumentDialog(document)">删除文件</button>
              </div>
            </div>
          </div>
          <div v-else class="empty-state compact">
            <div>
              <div class="empty-icon">□</div>
              <h3>{{ browserState === 'loading' ? '正在加载文件' : '暂无文件' }}</h3>
              <p class="muted">选择知识库后会在这里展示库内文档。</p>
            </div>
          </div>
        </section>
      </div>
      <p :class="uploadState === 'error' ? 'error' : uploadState === 'success' ? 'success' : 'muted'">
        {{ uploadMessage }}
      </p>
    </article>

    <div class="knowledge-admin-layout">
      <div class="admin-main-column">
        <article class="card">
          <div class="section-header">
            <div>
              <h3>治理能力地图</h3>
              <p class="muted">展示管理员当前能看到什么、还缺什么，避免误以为页面已经具备完整数据治理能力。</p>
            </div>
          </div>
          <div class="governance-grid">
            <div v-for="item in governanceItems" :key="item.label" class="governance-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.detail }}</p>
            </div>
          </div>
        </article>

        <article class="card">
          <div class="section-header">
            <div>
              <h3>接口接入状态</h3>
              <p class="muted">管理感知能力取决于后端数据面，当前不展示模拟文档或虚构任务。</p>
            </div>
          </div>
          <div class="endpoint-status-list">
            <div v-for="item in endpointStatus" :key="item.name" class="endpoint-status-row">
              <div>
                <strong>{{ item.name }}</strong>
                <span>{{ item.path }}</span>
              </div>
              <span :class="['status-pill', `${item.tone}-pill`]">{{ item.state }}</span>
            </div>
          </div>
        </article>

        <article class="card">
          <div class="section-header">
            <div>
              <h3>本页上传记录</h3>
              <p class="muted">这里保留当前页面本次上传成功的记录，完整文件以知识库文件浏览列表为准。</p>
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
              <span class="status-pill success-pill">{{ item.status }}</span>
              <span>{{ item.uploadedAt }}</span>
            </div>
          </div>
          <div v-else class="empty-state compact">
            <div>
              <div class="empty-icon">◇</div>
              <h3>暂无本页上传记录</h3>
              <p class="muted">完成一次上传后，可在这里查看文档 ID、切片数和入库状态。</p>
            </div>
          </div>
        </article>
      </div>

      <aside class="admin-side-column">
        <article class="card admin-insight-card">
          <h3>管理员关注点</h3>
          <ul class="admin-check-list">
            <li>知识资产是否为空或覆盖不足</li>
            <li>文档是否全部完成可检索索引</li>
            <li>切片规模是否随上传持续增长</li>
            <li>缺失接口是否阻塞删除、分页和状态追踪</li>
          </ul>
        </article>
      </aside>
    </div>

    <div v-if="createDialogOpen" class="dialog-mask" @click.self="closeCreateDialog">
      <div class="dialog-card" role="dialog" aria-modal="true" aria-labelledby="create-kb-title">
        <div class="dialog-head">
          <div>
            <p class="status-pill">新建知识库</p>
            <h3 id="create-kb-title">创建知识库</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭新建知识库弹窗" @click="closeCreateDialog">x</button>
        </div>
        <label class="dialog-field">
          <span class="muted">知识库名称</span>
          <input v-model="createKnowledgeBaseName" type="text" maxlength="128" placeholder="请输入知识库名称" @keydown.enter.prevent="submitCreateKnowledgeBase">
        </label>
        <label class="dialog-field">
          <span class="muted">描述</span>
          <textarea v-model="createKnowledgeBaseDescription" rows="3" maxlength="1024" placeholder="可选，用于说明知识范围"></textarea>
        </label>
        <label class="dialog-field">
          <span class="muted">可见性</span>
          <select v-model="createKnowledgeBaseVisibility">
            <option value="COMPANY">公司可见</option>
            <option value="DEPARTMENT">部门可见</option>
            <option value="PRIVATE">私有</option>
          </select>
        </label>
        <div class="dialog-actions">
          <button type="button" class="button secondary" @click="closeCreateDialog">取消</button>
          <button type="button" :disabled="createSaving" @click="submitCreateKnowledgeBase">
            {{ createSaving ? '创建中...' : '确认创建' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="renameDialogOpen" class="dialog-mask" @click.self="closeRenameDialog">
      <div class="dialog-card" role="dialog" aria-modal="true" aria-labelledby="rename-kb-title">
        <div class="dialog-head">
          <div>
            <p class="status-pill">修改知识库名</p>
            <h3 id="rename-kb-title">编辑知识库名称</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭修改名称弹窗" @click="closeRenameDialog">x</button>
        </div>
        <label class="dialog-field">
          <span class="muted">知识库名称</span>
          <input v-model="renameKnowledgeBaseName" type="text" maxlength="128" placeholder="请输入知识库名称" @keydown.enter.prevent="submitRenameKnowledgeBase">
        </label>
        <div class="dialog-actions">
          <button type="button" class="button secondary" @click="closeRenameDialog">取消</button>
          <button type="button" :disabled="renameSaving" @click="submitRenameKnowledgeBase">
            {{ renameSaving ? '保存中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteDialogOpen" class="dialog-mask" @click.self="closeDeleteDialog">
      <div class="dialog-card" role="dialog" aria-modal="true" aria-labelledby="delete-kb-title">
        <div class="dialog-head">
          <div>
            <p class="status-pill danger-pill">删除知识库</p>
            <h3 id="delete-kb-title">确认删除「{{ deleteKnowledgeBaseName }}」？</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭删除弹窗" @click="closeDeleteDialog">x</button>
        </div>
        <p class="muted">删除后该知识库将不再出现在管理列表中；其下文档会随数据库外键策略处理。</p>
        <div class="dialog-actions">
          <button type="button" class="button secondary" @click="closeDeleteDialog">取消</button>
          <button type="button" class="danger-button" :disabled="deleteSaving" @click="submitDeleteKnowledgeBase">
            {{ deleteSaving ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteDocumentDialogOpen" class="dialog-mask" @click.self="closeDeleteDocumentDialog">
      <div class="dialog-card" role="dialog" aria-modal="true" aria-labelledby="delete-document-title">
        <div class="dialog-head">
          <div>
            <p class="status-pill danger-pill">删除文件</p>
            <h3 id="delete-document-title">确认删除「{{ deleteDocumentName }}」？</h3>
          </div>
          <button type="button" class="source-close" aria-label="关闭删除文件弹窗" @click="closeDeleteDocumentDialog">x</button>
        </div>
        <p class="muted">删除后该文件将从当前知识库文件列表中移除，关联切片也会标记为已删除。</p>
        <div class="dialog-actions">
          <button type="button" class="button secondary" @click="closeDeleteDocumentDialog">取消</button>
          <button type="button" class="danger-button" :disabled="deleteDocumentSaving" @click="submitDeleteDocument">
            {{ deleteDocumentSaving ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
