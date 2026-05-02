<script setup>
import { ref, computed } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const router = (await import('vue-router')).useRouter()

const levels = [
  { label: '小学', subjects: ['语文', '数学', '英语', '科学'] },
  { label: '初中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治'] },
  { label: '高中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理'] },
  { label: '大学', subjects: ['高等数学', '线性代数', '概率论', '数据结构', '操作系统', '计算机网络', '数据库', '大学英语', '计算机组成原理', '软件工程'] }
]
const questions = ref([])
const selectedLevel = ref('')
const subjectOptions = ref([])
const selectedCat = ref('')
const selectedDiff = ref('')
const keyword = ref('')
const selectedQuestion = ref(null)
const showAnswer = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const highlightKeyword = computed(() => keyword.value?.trim() || '')

function highlightText(text, kw) {
  if (!kw || !text) return text
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

function stripLatexMarkers(text) {
  if (!text) return text
  return text.replace(/\$\$?([^$]*?)\$\$?/g, '$1')
}

const onLevelChange = () => {
  selectedCat.value = ''
  subjectOptions.value = levels.find(l => l.label === selectedLevel.value)?.subjects || []
}

const totalPages = () => Math.ceil(total.value / pageSize.value)

const loadQuestions = async () => {
  try {
    const params = new URLSearchParams({ page: page.value, size: pageSize.value })
    if (selectedCat.value) params.set('category', selectedCat.value)
    if (selectedDiff.value) params.set('difficulty', selectedDiff.value)
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const result = await api(`/student/questions?${params}`)
    questions.value = result.records || []
    total.value = result.total || 0
  } catch (e) {
    questions.value = []
  }
}

const selectQuestion = async (id) => {
  try {
    selectedQuestion.value = await api(`/student/questions/${id}`)
    showAnswer.value = false
  } catch (e) {
    alert('加载题目失败: ' + e.message)
  }
}

const backToList = () => {
  selectedQuestion.value = null
  showAnswer.value = false
}

const goToPage = (p) => {
  if (p >= 1 && p <= totalPages()) {
    page.value = p
    loadQuestions()
  }
}

const onSearch = () => {
  page.value = 1
  loadQuestions()
}

const resetFilters = () => {
  keyword.value = ''
  selectedCat.value = ''
  selectedDiff.value = ''
  selectedLevel.value = ''
  subjectOptions.value = []
  page.value = 1
  loadQuestions()
}

const askAboutQuestion = (q) => {
  localStorage.setItem('tutorAutoQuestion', q.stem)
  localStorage.setItem('tutorAutoAnswer', q.answer || '')
  localStorage.setItem('tutorAutoQuestionId', q.id)
  router.push('/student/tutor')
}

loadQuestions()
</script>

<template>
  <div>
    <!-- Question List -->
    <div v-if="!selectedQuestion">
      <div class="page-header mb-4">
        <div>
          <h2>题库练习</h2>
          <p>共 {{ total }} 道题目</p>
        </div>
      </div>

      <!-- Search & Filters -->
      <div class="card mb-4">
        <div class="card-body">
          <div class="flex gap-2 flex-wrap" style="align-items: flex-end;">
            <div class="form-group" style="flex: 1; min-width: 200px; margin-bottom: 0;">
              <label>关键词搜索</label>
              <input class="form-input" v-model="keyword" @keyup.enter="onSearch" placeholder="搜索题目内容..." />
            </div>
            <div class="form-group" style="min-width: 140px; margin-bottom: 0;">
              <label>学段</label>
              <select class="form-select" v-model="selectedLevel" @change="onLevelChange">
                <option value="">全部学段</option>
                <option v-for="lv in levels" :key="lv.label" :value="lv.label">{{ lv.label }}</option>
              </select>
            </div>
            <div class="form-group" style="min-width: 140px; margin-bottom: 0;">
              <label>学科</label>
              <select class="form-select" v-model="selectedCat" @change="onSearch">
                <option value="">全部学科</option>
                <option v-for="s in subjectOptions" :key="s" :value="`${selectedLevel}-${s}`">{{ s }}</option>
              </select>
            </div>
            <div class="form-group" style="min-width: 140px; margin-bottom: 0;">
              <label>难度</label>
              <select class="form-select" v-model="selectedDiff" @change="onSearch">
                <option value="">全部难度</option>
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </div>
            <button class="btn btn-primary" @click="onSearch">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              搜索
            </button>
            <button class="btn btn-outline" @click="resetFilters">重置</button>
          </div>
        </div>
      </div>

      <!-- Results Table -->
      <div class="table-wrapper" v-if="questions.length">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 80px;">题号</th>
              <th>题目</th>
              <th style="width: 100px;">分类</th>
              <th style="width: 80px;">题型</th>
              <th style="width: 80px;">难度</th>
              <th style="width: 60px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in questions" :key="q.id" style="cursor: pointer;" @click="selectQuestion(q.id)">
              <td class="text-muted">#{{ q.id }}</td>
              <td>
                <div v-if="highlightKeyword" v-html="highlightText(stripLatexMarkers(q.stem), highlightKeyword)" style="max-width: 500px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;"></div>
                <div v-else v-latex="q.stem" style="max-width: 500px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;"></div>
              </td>
              <td><span class="badge badge-gray">{{ q.category?.split('-')[0] || '' }}</span></td>
              <td>{{ questionTypeLabel(q.type) }}</td>
              <td>
                <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">
                  {{ q.difficulty }}
                </span>
              </td>
              <td><button class="btn btn-ghost btn-sm">查看</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-state">
        <div class="empty-state-icon"></div>
        <h4>暂无题目</h4>
        <p>请调整筛选条件后重试</p>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages() > 1" class="pagination mt-4">
        <span class="pagination-info">第 {{ page }}/{{ totalPages() }} 页，共 {{ total }} 题</span>
        <div class="pagination-btns">
          <button :disabled="page === 1" @click="goToPage(1)">&laquo;</button>
          <button :disabled="page === 1" @click="goToPage(page - 1)">&lsaquo;</button>
          <button class="active">{{ page }}</button>
          <button :disabled="page === totalPages()" @click="goToPage(page + 1)">&rsaquo;</button>
          <button :disabled="page === totalPages()" @click="goToPage(totalPages())">&raquo;</button>
        </div>
      </div>
    </div>

    <!-- Question Detail -->
    <div v-if="selectedQuestion">
      <div class="page-header mb-4">
        <div>
          <button class="btn btn-ghost" @click="backToList" style="padding-left: 0;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
            返回列表
          </button>
        </div>
        <div>
          <button class="btn btn-primary btn-sm" @click="askAboutQuestion(selectedQuestion)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            基于当前题目提问
          </button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="flex items-center gap-2">
            <span :class="['badge', selectedQuestion.difficulty === 'EASY' ? 'badge-green' : selectedQuestion.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ selectedQuestion.difficulty }}</span>
            <span class="badge badge-gray">{{ selectedQuestion.category }}</span>
            <span class="badge badge-cyan">{{ questionTypeLabel(selectedQuestion.type) }}</span>
          </div>
        </div>
        <div class="card-body">
          <div class="q-stem" v-latex="selectedQuestion.stem"></div>

          <div v-if="selectedQuestion.options" class="mt-4">
            <div v-for="[k, v] in Object.entries(selectedQuestion.options)" :key="k" class="q-option" style="cursor: default;">
              <span class="q-option-key">{{ k }}</span>
              <span v-latex="v"></span>
            </div>
          </div>

          <div class="mt-6">
            <button class="btn btn-outline" @click="showAnswer = !showAnswer">
              {{ showAnswer ? '隐藏答案' : '查看答案' }}
            </button>
            <div v-if="showAnswer" class="mt-4 p-4" style="background: var(--success-bg); border-radius: var(--radius-md);">
              <div style="font-weight: 600; color: var(--success); margin-bottom: 4px;">参考答案</div>
              <span v-latex="selectedQuestion.answer || '见解析'"></span>
            </div>
          </div>

          <div v-if="selectedQuestion.analysis" class="mt-4 p-4" style="background: var(--primary-50); border-radius: var(--radius-md);">
            <div style="font-weight: 600; color: var(--primary); margin-bottom: 4px;">解析</div>
            <span v-latex="selectedQuestion.analysis"></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
