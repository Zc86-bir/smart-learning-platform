<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const papers = ref([])
const loading = ref(true)
const showModal = ref(false)
const paperTitle = ref('')
const paperDesc = ref('')
const paperDuration = ref(60)
const paperScore = ref(100)

const loadPapers = async () => {
  loading.value = true
  try {
    const result = await api('/admin/papers?page=1&size=50')
    papers.value = result.records || []
  } catch {
    papers.value = []
  } finally {
    loading.value = false
  }
}

const createPaper = async () => {
  if (!paperTitle.value.trim()) return
  try {
    await api('/admin/papers', {
      method: 'POST',
      body: JSON.stringify({
        title: paperTitle.value,
        description: paperDesc.value,
        durationMinutes: paperDuration.value,
        totalScore: paperScore.value,
        questionIds: []
      })
    })
    showModal.value = false
    paperTitle.value = ''
    paperDesc.value = ''
    loadPapers()
  } catch (e) {
    alert('创建失败: ' + e.message)
  }
}

const deletePaper = async (id) => {
  if (!confirm('确定删除此试卷?')) return
  try {
    await api(`/admin/papers/${id}`, { method: 'DELETE' })
    loadPapers()
  } catch (e) {
    alert('删除失败: ' + e.message)
  }
}

onMounted(() => loadPapers())
</script>

<template>
  <div class="paper-management">
    <div class="page-header-row">
      <div>
        <h2 class="page-title-small">试卷管理</h2>
        <p class="page-subtitle">管理试卷列表,可关联题目</p>
      </div>
      <button class="btn btn-primary" @click="showModal = true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        新建试卷
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <div v-else-if="papers.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
      </svg>
      <p>暂无试卷</p>
    </div>

    <div v-else class="paper-grid">
      <div v-for="p in papers" :key="p.id" class="paper-card">
        <div class="paper-card-header">
          <h3 class="paper-card-title">{{ p.title }}</h3>
          <div class="paper-card-actions">
            <button class="btn btn-ghost btn-sm" @click="deletePaper(p.id)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
        <p class="paper-card-desc">{{ p.description || '暂无描述' }}</p>
        <div class="paper-card-meta">
          <span class="paper-meta-item">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
            </svg>
            {{ p.durationMinutes || 60 }} 分钟
          </span>
          <span class="paper-meta-item">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            {{ p.totalScore || 100 }} 分
          </span>
        </div>
      </div>
    </div>

    <!-- Create Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">新建试卷</h3>
          <button class="modal-close" @click="showModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">试卷名称 *</label>
            <input v-model="paperTitle" class="form-input" placeholder="例如: 数学期中测试" />
          </div>
          <div class="form-group">
            <label class="form-label">描述</label>
            <textarea v-model="paperDesc" class="form-textarea" rows="3" placeholder="试卷说明..."></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">时长(分钟)</label>
              <input v-model.number="paperDuration" type="number" class="form-input" min="10" max="300" />
            </div>
            <div class="form-group">
              <label class="form-label">总分</label>
              <input v-model.number="paperScore" type="number" class="form-input" min="10" max="200" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showModal = false">取消</button>
          <button class="btn btn-primary" @click="createPaper" :disabled="!paperTitle.trim()">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.paper-management {
  padding: 1.5rem;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.page-title-small {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 0.25rem;
}

.page-subtitle {
  color: var(--text-secondary, #64748b);
  font-size: 0.875rem;
  margin: 0;
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.paper-card {
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}

.paper-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.paper-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.paper-card-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  margin: 0;
  flex: 1;
}

.paper-card-desc {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0 0 0.75rem;
}

.paper-card-meta {
  display: flex;
  gap: 1rem;
}

.paper-meta-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--bg-secondary, #fff);
  border-radius: 16px;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border, #e2e8f0);
}

.modal-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border, #e2e8f0);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3rem;
  color: var(--text-secondary, #64748b);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border, #e2e8f0);
  border-top-color: var(--primary, #3b82f6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 0.75rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state svg {
  margin-bottom: 1rem;
  opacity: 0.3;
}
</style>
