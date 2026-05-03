<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const videos = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const category = ref('')

const categories = ref([])

async function fetchCategories() {
  try {
    const res = await api.get('/student/categories')
    if (res.data) {
      categories.value = res.data.map(c => c.name)
    }
  } catch {
    categories.value = ['数学', '物理', '化学', '编程', '英语']
  }
}

async function fetchVideos() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (category.value) params.category = category.value
    const res = await api.get('/student/videos', { params })
    if (res.data) {
      videos.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load videos:', e)
    videos.value = []
  } finally {
    loading.value = false
  }
}

function filterByCategory(cat) {
  category.value = category.value === cat ? '' : cat
  page.value = 1
  fetchVideos()
}

function changePage(newPage) {
  page.value = newPage
  fetchVideos()
}

function formatDuration(seconds) {
  if (!seconds) return ''
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

onMounted(() => {
  fetchCategories()
  fetchVideos()
})
</script>

<template>
  <div class="video-list-page">
    <!-- Header -->
    <div class="video-header">
      <h2 class="video-title">视频学习</h2>
      <p class="video-subtitle">精选优质学习资源，助力高效学习</p>
    </div>

    <!-- Category Filter -->
    <div class="category-bar">
      <button
        :class="['category-tag', { active: !category }]"
        @click="filterByCategory('')"
      >
        全部
      </button>
      <button
        v-for="cat in categories"
        :key="cat"
        :class="['category-tag', { active: category === cat }]"
        @click="filterByCategory(cat)"
      >
        {{ cat }}
      </button>
    </div>

    <!-- Video Grid -->
    <div v-if="loading" class="video-loading">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <div v-else-if="videos.length === 0" class="video-empty">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <rect x="2" y="4" width="20" height="16" rx="2"/>
        <polygon points="10,8 16,12 10,16"/>
      </svg>
      <p>暂无视频内容</p>
    </div>

    <div v-else class="video-grid">
      <div
        v-for="video in videos"
        :key="video.id"
        class="video-card"
      >
        <div class="video-thumbnail">
          <img
            v-if="video.coverUrl"
            :src="video.coverUrl"
            :alt="video.title"
            loading="lazy"
          />
          <div v-else class="video-placeholder">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <polygon points="10,8 16,12 10,16"/>
              <rect x="2" y="4" width="20" height="16" rx="2"/>
            </svg>
          </div>
          <span v-if="video.durationDisplay || video.durationSeconds" class="video-duration">
            {{ video.durationDisplay || formatDuration(video.durationSeconds) }}
          </span>
        </div>
        <div class="video-info">
          <h3 class="video-card-title">{{ video.title }}</h3>
          <p v-if="video.description" class="video-card-desc">{{ video.description }}</p>
          <div class="video-meta">
            <span v-if="video.category" class="video-badge">{{ video.category }}</span>
            <span v-if="video.isOfficial" class="video-badge official">官方</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="total > size" class="video-pagination">
      <button
        :disabled="page <= 1"
        @click="changePage(page - 1)"
        class="page-btn"
      >
        上一页
      </button>
      <span class="page-info">第 {{ page }} 页 / 共 {{ Math.ceil(total / size) }} 页</span>
      <button
        :disabled="page >= Math.ceil(total / size)"
        @click="changePage(page + 1)"
        class="page-btn"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.video-list-page {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.video-header {
  margin-bottom: 1.5rem;
}

.video-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 0.25rem;
}

.video-subtitle {
  color: var(--text-secondary, #64748b);
  font-size: 0.95rem;
  margin: 0;
}

/* Category Bar */
.category-bar {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.category-tag {
  padding: 0.4rem 1rem;
  border-radius: 2rem;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-secondary, #fff);
  color: var(--text-secondary, #64748b);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.category-tag:hover {
  border-color: var(--primary, #3b82f6);
  color: var(--primary, #3b82f6);
}

.category-tag.active {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

/* Video Grid */
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.25rem;
}

.video-card {
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.video-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.video-thumbnail {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--bg-tertiary, #f1f5f9);
  overflow: hidden;
}

.video-thumbnail img {
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

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
}

.video-info {
  padding: 1rem;
}

.video-card-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 0.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-card-desc {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0 0 0.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-meta {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.video-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  background: var(--bg-tertiary, #f1f5f9);
  color: var(--text-secondary, #64748b);
}

.video-badge.official {
  background: #dbeafe;
  color: #1d4ed8;
}

/* Loading & Empty */
.video-loading,
.video-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: var(--text-secondary, #64748b);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border, #e2e8f0);
  border-top-color: var(--primary, #3b82f6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.video-empty svg {
  margin-bottom: 1rem;
  opacity: 0.4;
}

/* Pagination */
.video-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
}

.page-btn {
  padding: 0.5rem 1.25rem;
  border-radius: 8px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-secondary, #fff);
  color: var(--text-primary, #1a1a2e);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.875rem;
  color: var(--text-secondary, #64748b);
}
</style>
