<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../composables/api'

const students = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const selectedStudent = ref(null)
const studentExams = ref([])
const examLoading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const loadStudents = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, size: pageSize.value })
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const result = await api(`/admin/students?${params}`)
    students.value = result.records || []
    total.value = result.total || 0
  } catch (e) {
    students.value = []
  } finally {
    loading.value = false
  }
}

const loadStudentExams = async (student) => {
  selectedStudent.value = student
  examLoading.value = true
  try {
    const result = await api(`/admin/students/${student.id}/exams?page=1&size=10`)
    studentExams.value = result.records || []
  } catch {
    studentExams.value = []
  } finally {
    examLoading.value = false
  }
}

const closeStudentDetail = () => {
  selectedStudent.value = null
  studentExams.value = []
}

const goToPage = (p) => {
  if (p >= 1 && p <= totalPages.value) {
    page.value = p
    loadStudents()
  }
}

const onSearch = () => {
  page.value = 1
  loadStudents()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getExamStatus = (status) => {
  const map = {
    IN_PROGRESS: { label: '进行中', class: 'badge-blue' },
    SUBMITTED: { label: '已提交', class: 'badge-yellow' },
    GRADED: { label: '已批改', class: 'badge-green' },
    FORCE_SUBMITTED: { label: '强制提交', class: 'badge-red' }
  }
  return map[status] || { label: status, class: 'badge-gray' }
}

onMounted(loadStudents)
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>学生管理</h2>
        <p>查看学生信息、考试记录和学习数据</p>
      </div>
    </div>

    <!-- Student Detail Modal -->
    <div v-if="selectedStudent" class="card mb-4">
      <div class="card-header">
        <h3>学生详情 — {{ selectedStudent.nickname }} ({{ selectedStudent.username }})</h3>
        <button class="btn btn-ghost btn-sm" @click="closeStudentDetail">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
          关闭
        </button>
      </div>
      <div class="card-body">
        <div class="stats-grid mb-4" style="grid-template-columns: repeat(4, 1fr);">
          <div class="stat-card">
            <div class="stat-card-label">考试次数</div>
            <div class="stat-card-value">{{ selectedStudent.examCount || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-card-label">已完成</div>
            <div class="stat-card-value">{{ selectedStudent.completedExams || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-card-label">最近活跃</div>
            <div class="stat-card-value" style="font-size: 14px;">{{ formatDate(selectedStudent.lastActiveTime) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-card-label">注册时间</div>
            <div class="stat-card-value" style="font-size: 14px;">{{ formatDate(selectedStudent.createdAt) }}</div>
          </div>
        </div>

        <h4 style="font-size: 14px; font-weight: 600; margin-bottom: 12px; color: var(--slate-700);">最近考试记录</h4>
        <div v-if="examLoading" class="text-center" style="padding: 20px;">
          <div class="spinner" style="margin: 0 auto 8px; width: 24px; height: 24px; border-width: 3px;"></div>
          <p class="text-sm text-muted">加载中...</p>
        </div>
        <div v-else-if="studentExams.length" class="table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th>试卷</th>
                <th>状态</th>
                <th>得分</th>
                <th>开始时间</th>
                <th>用时</th>
                <th>切屏</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in studentExams" :key="e.id">
                <td style="font-weight: 500;">{{ e.paperTitle || '试卷 #' + e.paperId }}</td>
                <td>
                  <span :class="['badge', getExamStatus(e.status).class]">
                    {{ getExamStatus(e.status).label }}
                  </span>
                </td>
                <td>{{ e.score !== null ? e.score + '/' + e.totalScore : '-' }}</td>
                <td>{{ formatDateTime(e.startTime) }}</td>
                <td v-if="e.durationSeconds">{{ Math.floor(e.durationSeconds / 60) }}分{{ e.durationSeconds % 60 }}秒</td>
                <td v-else>-</td>
                <td>{{ e.cutScreenCount || 0 }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="text-center text-muted" style="padding: 20px;">暂无考试记录</div>
      </div>
    </div>

    <!-- Student List -->
    <div class="card mb-4">
      <div class="card-body">
        <div class="flex gap-2 items-center">
          <input class="form-input" v-model="keyword" @keyup.enter="onSearch" placeholder="搜索用户名或昵称..." style="max-width: 240px;" />
          <button class="btn btn-primary btn-sm" @click="onSearch">搜索</button>
          <div style="flex: 1;"></div>
          <span class="text-sm text-muted">共 {{ total }} 名学生</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="card text-center" style="padding: 40px;">
      <div class="spinner" style="margin: 0 auto; width: 32px; height: 32px; border-width: 3px;"></div>
      <p class="mt-2 text-sm text-muted">加载中...</p>
    </div>

    <div v-else-if="students.length" class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>考试次数</th>
            <th>已完成</th>
            <th>最近活跃</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in students" :key="s.id">
            <td class="text-muted">{{ s.id }}</td>
            <td style="font-weight: 500;">{{ s.username }}</td>
            <td>{{ s.nickname }}</td>
            <td>{{ s.examCount || 0 }}</td>
            <td>{{ s.completedExams || 0 }}</td>
            <td>{{ formatDate(s.lastActiveTime) }}</td>
            <td>{{ formatDate(s.createdAt) }}</td>
            <td>
              <button class="btn btn-ghost btn-sm" @click="loadStudentExams(s)">详情</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty-state">
      <p class="text-muted">暂无学生数据</p>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination mt-4">
      <span class="pagination-info">第 {{ page }}/{{ totalPages }} 页，共 {{ total }} 名学生</span>
      <div class="pagination-btns">
        <button :disabled="page === 1" @click="goToPage(1)">&laquo;</button>
        <button :disabled="page === 1" @click="goToPage(page - 1)">&lsaquo;</button>
        <button class="active">{{ page }}</button>
        <button :disabled="page === totalPages" @click="goToPage(page + 1)">&rsaquo;</button>
        <button :disabled="page === totalPages" @click="goToPage(totalPages)">&raquo;</button>
      </div>
    </div>
  </div>
</template>
