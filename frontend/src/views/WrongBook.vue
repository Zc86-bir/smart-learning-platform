<script setup>
import { ref, computed, onMounted } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const wrongQuestions = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedDiff = ref('')
const masteredFilter = ref('all')

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const unmasteredCount = computed(() => wrongQuestions.value.filter(q => !q.mastered).length)
const masteredCount = computed(() => wrongQuestions.value.filter(q => q.mastered).length)

const loadQuestions = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({ page: page.value, size: pageSize.value })
    if (selectedDiff.value) params.set('difficulty', selectedDiff.value)
    if (masteredFilter.value === 'mastered') params.set('mastered', 'true')
    else if (masteredFilter.value === 'unmastered') params.set('mastered', 'false')
    const result = await api(`/student/wrong-questions?${params}`)
    wrongQuestions.value = result.records || []
    total.value = result.total || 0
  } catch (e) {
    wrongQuestions.value = []
  } finally {
    loading.value = false
  }
}

const markMastered = async (wrongId) => {
  try {
    await api(`/student/wrong-questions/${wrongId}/master`, { method: 'POST' })
    await loadQuestions()
  } catch (e) {
    alert('操作失败: ' + e.message)
  }
}

const resetMastered = async (wrongId) => {
  try {
    await api(`/student/wrong-questions/${wrongId}/reset`, { method: 'POST' })
    await loadQuestions()
  } catch (e) {
    alert('操作失败: ' + e.message)
  }
}

const goToPage = (p) => {
  if (p >= 1 && p <= totalPages.value) {
    page.value = p
    loadQuestions()
  }
}

onMounted(() => { loadQuestions() })
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>错题本</h2>
        <p>考试中的错题会自动收集到这里，帮助你查漏补缺</p>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-grid mb-4">
      <div class="stat-card">
        <div class="stat-card-header">
          <div>
            <div class="stat-card-label">错题总数</div>
            <div class="stat-card-value">{{ total }}</div>
          </div>
          <div class="stat-card-icon red">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-header">
          <div>
            <div class="stat-card-label">未掌握</div>
            <div class="stat-card-value">{{ unmasteredCount }}</div>
          </div>
          <div class="stat-card-icon yellow">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-card-header">
          <div>
            <div class="stat-card-label">已掌握</div>
            <div class="stat-card-value">{{ masteredCount }}</div>
          </div>
          <div class="stat-card-icon green">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-4">
      <div class="card-body">
        <div class="flex gap-2 items-center">
          <label class="text-sm font-semibold text-muted">筛选：</label>
          <button :class="['btn btn-sm', masteredFilter === 'all' ? 'btn-primary' : 'btn-outline']" @click="masteredFilter = 'all'; page = 1; loadQuestions()">全部</button>
          <button :class="['btn btn-sm', masteredFilter === 'unmastered' ? 'btn-primary' : 'btn-outline']" @click="masteredFilter = 'unmastered'; page = 1; loadQuestions()">未掌握</button>
          <button :class="['btn btn-sm', masteredFilter === 'mastered' ? 'btn-primary' : 'btn-outline']" @click="masteredFilter = 'mastered'; page = 1; loadQuestions()">已掌握</button>
          <div style="flex: 1;"></div>
          <select class="form-select" v-model="selectedDiff" @change="page = 1; loadQuestions()" style="width: 120px; padding: 6px 10px; font-size: 13px;">
            <option value="">全部难度</option>
            <option value="EASY">简单</option>
            <option value="MEDIUM">中等</option>
            <option value="HARD">困难</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card text-center" style="padding: 40px;">
      <div class="spinner" style="margin: 0 auto; width: 32px; height: 32px; border-width: 3px;"></div>
      <p class="mt-2 text-sm text-muted">加载中...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="!wrongQuestions.length" class="empty-state">
      <div class="empty-state-icon">🎉</div>
      <h4>暂无错题</h4>
      <p>考试中的错题会自动出现在这里</p>
    </div>

    <!-- Wrong Questions List -->
    <template v-else>
      <div v-for="q in wrongQuestions" :key="q.id" class="card mb-4">
        <div class="card-header">
          <div class="flex items-center gap-2">
            <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ q.difficulty }}</span>
            <span class="badge badge-gray">{{ q.category?.split('-')[0] || q.category }}</span>
            <span class="badge badge-cyan" v-if="q.type">{{ questionTypeLabel(q.type) }}</span>
            <span v-if="q.mastered" class="badge badge-green">✓ 已掌握</span>
            <span v-else class="badge badge-red">✗ 未掌握</span>
            <span class="text-xs text-muted" v-if="q.wrongCount > 1">错误 {{ q.wrongCount }} 次</span>
          </div>
          <div class="flex gap-2">
            <button class="btn btn-sm btn-success" v-if="!q.mastered" @click="markMastered(q.id)">标记已掌握</button>
            <button class="btn btn-sm btn-outline" v-if="q.mastered" @click="resetMastered(q.id)">取消掌握</button>
          </div>
        </div>
        <div class="card-body">
          <div class="q-stem" :key="'wb-stem-' + q.id" v-latex="q.stem"></div>

          <div class="mt-4 p-4" style="background: var(--danger-bg); border-radius: var(--radius-md);">
            <div style="font-weight: 600; color: var(--danger); font-size: 13px; margin-bottom: 4px;">你的答案</div>
            <span>{{ q.studentAnswer || '未作答' }}</span>
          </div>
          <div class="mt-2 p-4" style="background: var(--success-bg); border-radius: var(--radius-md);">
            <div style="font-weight: 600; color: var(--success); font-size: 13px; margin-bottom: 4px;">正确答案</div>
            <span :key="'wb-ca-' + q.id" v-latex="q.correctAnswer"></span>
          </div>
          <div v-if="q.analysis" class="mt-2 p-4" style="background: var(--primary-50); border-radius: var(--radius-md);">
            <div style="font-weight: 600; color: var(--primary); font-size: 13px; margin-bottom: 4px;">解析</div>
            <span :key="'wb-ana-' + q.id" v-latex="q.analysis"></span>
          </div>
          <div v-if="q.knowledgePoint" class="mt-2 text-sm text-muted">知识点：{{ q.knowledgePoint }}</div>
          <div v-if="q.lastWrongTime" class="mt-1 text-xs text-muted">最近错误：{{ q.lastWrongTime }}</div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <span class="pagination-info">共 {{ total }} 题，第 {{ page }}/{{ totalPages }} 页</span>
        <div class="pagination-btns">
          <button :disabled="page === 1" @click="goToPage(1)">&laquo;</button>
          <button :disabled="page === 1" @click="goToPage(page - 1)">&lsaquo;</button>
          <button class="active">{{ page }}</button>
          <button :disabled="page === totalPages" @click="goToPage(page + 1)">&rsaquo;</button>
          <button :disabled="page === totalPages" @click="goToPage(totalPages)">&raquo;</button>
        </div>
      </div>
    </template>
  </div>
</template>
