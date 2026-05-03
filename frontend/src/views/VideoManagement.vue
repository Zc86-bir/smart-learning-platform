<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const emit = defineEmits(['refresh'])

const videos = ref([])
const loading = ref(true)
const statusFilter = ref('')
const uploading = ref(false)
const uploadProgress = ref(0)

const statusMap = {
  PENDING: { label: '待审核', cls: 'badge-yellow' },
  APPROVED: { label: '已通过', cls: 'badge-green' },
  REJECTED: { label: '已拒绝', cls: 'badge-red' }
}

const loadVideos = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (statusFilter.value) params.status = statusFilter.value
    const result = await api(`/admin/videos?${new URLSearchParams(params)}`)
    videos.value = result.records || []
  } catch {
    videos.value = []
  } finally {
    loading.value = false
  }
}

const handleUpload = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  // TODO: implement actual upload with progress
  alert('视频上传功能需要配置OSS,当前仅展示上传入口')
}

onMounted(() => loadVideos())
</script>

<template>
  <div class="video-management">
    <div class="page-header-row">
      <div>
        <h2 class="page-title-small">视频管理</h2>
        <p class="page-subtitle">上传教学视频,查看审核状态</p>
      </div>
      <div class="header-actions">
        <div class="filter-group">
          <button
            v-for="[k, v] in Object.entries({ '': '全部', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' })"
            :key="k"
            :class="['filter-btn', { active: statusFilter === k }]"
            @click="statusFilter = k; loadVideos()"
          >
            {{ v }}
          </button>
        </div>
        <label class="btn btn-primary">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          上传视频
          <input type="file" accept="video/*" @change="handleUpload" style="display: none" />
        </label>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <div v-else-if="videos.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <rect x="2" y="4" width="20" height="16" rx="2"/><polygon points="10,8 16,12 10,16"/>
      </svg>
      <p>暂无视频</p>
    </div>

    <div v-else class="video-list">
      <div v-for="v in videos" :key="v.id" class="video-item">
        <div class="video-item-thumb">
          <img v-if="v.coverUrl" :src="v.coverUrl" :alt="v.title" />
          <div v-else class="video-placeholder">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <polygon points="10,8 16,12 10,16"/>
              <rect x="2" y="4" width="20" height="16" rx="2"/>
            </svg>
          </div>
        </div>
        <div class="video-item-info">
          <h3 class="video-item-title">{{ v.title }}</h3>
          <p class="video-item-desc">{{ v.description || '暂无描述' }}</p>
          <div class="video-item-meta">
            <span :class="['badge', statusMap[v.status]?.cls || 'badge-gray']">
              {{ statusMap[v.status]?.label || v.status }}
            </span>
            <span class="video-meta-text" v-if="v.category">{{ v.category }}</span>
            <span class="video-meta-text" v-if="v.createdAt">{{ new Date(v.createdAt).toLocaleDateString('zh-CN') }}</span>
          </div>
          <div v-if="v.status === 'REJECTED' && v.rejectReason" class="video-reject-reason">
            <strong>拒绝原因: </strong>{{ v.rejectReason }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-management {
  padding: 1.5rem;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  gap: 1rem;
  flex-wrap: wrap;
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

.header-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 0.25rem;
}

.filter-btn {
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-secondary, #fff);
  color: var(--text-secondary, #64748b);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  border-color: var(--primary, #3b82f6);
  color: var(--primary, #3b82f6);
}

.filter-btn.active {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.video-item {
  display: flex;
  gap: 1rem;
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.video-item-thumb {
  width: 160px;
  height: 90px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-tertiary, #f1f5f9);
}

.video-item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-muted, #94a3b8);
}

.video-item-info {
  flex: 1;
  min-width: 0;
}

.video-item-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-item-desc {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0 0 0.5rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-item-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.video-meta-text {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.video-reject-reason {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #dc2626;
}

.badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-yellow { background: #fef9c3; color: #a16207; }
.badge-green { background: #dcfce7; color: #16a34a; }
.badge-red { background: #fecaca; color: #dc2626; }
.badge-gray { background: #f1f5f9; color: #475569; }

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

@media (max-width: 640px) {
  .video-item {
    flex-direction: column;
  }
  .video-item-thumb {
    width: 100%;
    height: 180px;
  }
}
</style>
