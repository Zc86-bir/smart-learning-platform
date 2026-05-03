<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const students = ref([])
const loading = ref(true)
const keyword = ref('')
const filtered = ref([])

const loadStudents = async () => {
  loading.value = true
  try {
    students.value = await api('/admin/students') || []
    filtered.value = students.value
  } catch {
    students.value = []
    filtered.value = []
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) {
    filtered.value = students.value
    return
  }
  filtered.value = students.value.filter(s =>
    (s.username || '').toLowerCase().includes(kw) ||
    (s.nickname || '').toLowerCase().includes(kw)
  )
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => loadStudents())
</script>

<template>
  <div class="class-management">
    <!-- Header -->
    <div class="page-header-row">
      <div>
        <h2 class="page-title-small">班级管理</h2>
        <p class="page-subtitle">查看学生列表和学习情况</p>
      </div>
      <div class="search-box">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="keyword" @input="onSearch" placeholder="搜索学生..." class="search-input" />
      </div>
    </div>

    <!-- Student List -->
    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <p>加载中...</p>
    </div>

    <div v-else-if="filtered.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
      </svg>
      <p>暂无学生数据</p>
    </div>

    <div v-else class="student-table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th style="width: 50px;">#</th>
            <th>学生</th>
            <th>考试次数</th>
            <th>完成次数</th>
            <th>最近活跃</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(s, i) in filtered" :key="s.id">
            <td>{{ i + 1 }}</td>
            <td>
              <div class="student-cell">
                <div class="student-avatar">{{ (s.nickname || s.username || '?').charAt(0) }}</div>
                <div>
                  <div class="student-name">{{ s.nickname || s.username }}</div>
                  <div class="student-username">{{ s.username }}</div>
                </div>
              </div>
            </td>
            <td>{{ s.examCount || 0 }}</td>
            <td>{{ s.completedExams || 0 }}</td>
            <td>{{ formatTime(s.lastActiveTime) }}</td>
            <td>
              <span :class="['status-badge', s.status === 1 ? 'status-active' : 'status-inactive']">
                {{ s.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.class-management {
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

.search-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  background: var(--bg-secondary, #fff);
}

.search-box svg {
  color: var(--text-muted, #94a3b8);
}

.search-input {
  border: none;
  outline: none;
  font-size: 0.875rem;
  background: transparent;
  color: var(--text-primary, #1a1a2e);
  width: 200px;
}

.student-table-wrapper {
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.student-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
}

.student-name {
  font-weight: 500;
  color: var(--text-primary, #1a1a2e);
  font-size: 0.875rem;
}

.student-username {
  font-size: 0.75rem;
  color: var(--text-secondary, #64748b);
}

.status-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
}

.status-active { background: #dcfce7; color: #16a34a; }
.status-inactive { background: #fef2f2; color: #dc2626; }

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
