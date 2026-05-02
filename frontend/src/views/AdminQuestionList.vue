<script setup>
import { ref, computed } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const questions = ref([])
const loading = ref(false)
const keyword = ref('')
const selectedCat = ref('')
const selectedDiff = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const error = ref('')
const stats = ref({})
const editingId = ref(null)
const expandedId = ref(null)
const editForm = ref({ type: '', category: '', stem: '', options: {}, answer: '', analysis: '', difficulty: '', knowledgePoint: '' })
const editLevel = ref('')
const editSubjectOptions = ref([])
const selectedIds = ref(new Set())

const levels = [
  { label: '小学', subjects: ['语文', '数学', '英语', '科学'] },
  { label: '初中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治'] },
  { label: '高中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理'] },
  { label: '大学', subjects: ['高等数学', '线性代数', '概率论', '数据结构', '操作系统', '计算机网络', '数据库', '大学英语', '计算机组成原理', '软件工程'] }
]
const selectedLevel = ref('')
const subjectOptions = ref([])

const onLevelChange = () => {
  selectedCat.value = ''
  subjectOptions.value = levels.find(l => l.label === selectedLevel.value)?.subjects || []
}
const typeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'SHORT_ANSWER', label: '简答题' }
]

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const totalQuestions = computed(() => Object.values(stats.value).reduce((a, b) => a + b, 0))
const allSelected = computed(() => questions.value.length > 0 && selectedIds.value.size === questions.value.length)
const hasSelection = computed(() => selectedIds.value.size > 0)

const loadStats = async () => {
  try {
    stats.value = await api('/admin/questions/stats')
  } catch {
    stats.value = {}
  }
}

const loadQuestions = async () => {
  loading.value = true
  error.value = ''
  selectedIds.value.clear()
  try {
    const params = new URLSearchParams({ page: page.value, size: pageSize.value })
    if (selectedCat.value) params.set('category', selectedCat.value)
    if (selectedDiff.value) params.set('difficulty', selectedDiff.value)
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const result = await api(`/admin/questions?${params}`)
    questions.value = result.records || []
    total.value = result.total || 0
  } catch (e) {
    error.value = '加载失败: ' + e.message
    questions.value = []
  } finally {
    loading.value = false
  }
}

const startEdit = (q) => {
  editingId.value = q.id
  editForm.value = {
    type: q.type,
    category: q.category,
    stem: q.stem,
    options: q.options || {},
    answer: q.answer,
    analysis: q.analysis || '',
    difficulty: q.difficulty,
    knowledgePoint: q.knowledgePoint || ''
  }
  const parts = (q.category || '').split('-')
  if (parts.length === 2) {
    editLevel.value = parts[0]
    editSubjectOptions.value = levels.find(l => l.label === parts[0])?.subjects || []
  } else {
    editLevel.value = ''
    editSubjectOptions.value = []
  }
}

const cancelEdit = () => {
  editingId.value = null
}

const saveEdit = async () => {
  try {
    await api(`/admin/questions/${editingId.value}`, {
      method: 'PUT',
      body: JSON.stringify(editForm.value)
    })
    editingId.value = null
    await loadQuestions()
  } catch (e) {
    alert('保存失败: ' + e.message)
  }
}

const deleteQuestion = async (id) => {
  if (!confirm('确定删除此题？')) return
  try {
    await api(`/admin/questions/${id}`, { method: 'DELETE' })
    selectedIds.value.delete(id)
    await loadQuestions()
    await loadStats()
  } catch (e) {
    alert('删除失败: ' + e.message)
  }
}

const toggleSelect = (id) => {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
}

const selectAll = () => {
  if (allSelected.value) {
    selectedIds.value.clear()
  } else {
    questions.value.forEach(q => selectedIds.value.add(q.id))
  }
}

const batchDelete = async () => {
  if (!hasSelection.value) return
  if (!confirm(`确定删除选中的 ${selectedIds.value.size} 道题目？`)) return
  try {
    await api('/admin/questions/batch', {
      method: 'DELETE',
      body: JSON.stringify([...selectedIds.value])
    })
    selectedIds.value.clear()
    await loadQuestions()
    await loadStats()
  } catch (e) {
    alert('批量删除失败: ' + e.message)
  }
}

const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

const goToPage = (p) => {
  if (p >= 1 && p <= totalPages.value) {
    page.value = p
    loadQuestions()
  }
}

const onSearch = () => {
  page.value = 1
  loadQuestions()
}

const onEditLevelChange = () => {
  editForm.value.category = ''
  editSubjectOptions.value = levels.find(l => l.label === editLevel.value)?.subjects || []
}

const resetFilters = () => {
  keyword.value = ''
  selectedCat.value = ''
  selectedDiff.value = ''
  selectedLevel.value = ''
  page.value = 1
  loadQuestions()
}

loadStats()
loadQuestions()
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>题库管理</h2>
        <p>管理和编辑题库中的所有题目</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid mb-4">
      <div class="stat-card">
        <div class="stat-card-header">
          <div>
            <div class="stat-card-label">题目总数</div>
            <div class="stat-card-value">{{ totalQuestions }}</div>
          </div>
          <div class="stat-card-icon blue">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
        </div>
      </div>
      <div v-for="(count, cat) in stats" :key="cat" class="stat-card">
        <div class="stat-card-header">
          <div>
            <div class="stat-card-label">{{ cat }}</div>
            <div class="stat-card-value">{{ count }}</div>
          </div>
          <div class="stat-card-icon green">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- Search & Filters -->
    <div class="card mb-4">
      <div class="card-body">
        <div class="flex gap-2 flex-wrap" style="align-items: flex-end;">
          <div class="form-group" style="flex: 1; min-width: 200px; margin-bottom: 0;">
            <label>关键词搜索</label>
            <input class="form-input" v-model="keyword" @keyup.enter="onSearch" placeholder="搜索题干或答案..." />
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

    <!-- Error -->
    <p v-if="error" class="text-sm" style="color: var(--danger); margin-bottom: 12px;">{{ error }}</p>

    <!-- Batch action bar -->
    <div v-if="hasSelection" class="card mb-4" style="background: var(--primary-50); border: 1px solid var(--primary);">
      <div class="card-body flex gap-2" style="align-items: center;">
        <span style="font-weight: 500;">已选择 <strong>{{ selectedIds.size }}</strong> 道题目</span>
        <button class="btn btn-sm" style="background: var(--danger); color: white; margin-left: auto;" @click="batchDelete">批量删除</button>
        <button class="btn btn-ghost btn-sm" @click="selectedIds.clear()">取消选择</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card text-center" style="padding: 40px;">
      <div class="spinner" style="margin: 0 auto; width: 32px; height: 32px; border-width: 3px;"></div>
      <p class="mt-2 text-sm text-muted">加载中...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="!questions.length" class="empty-state">
      <div class="empty-state-icon">📭</div>
      <h4>暂无题目</h4>
      <p>请在「AI出题」中生成题目</p>
    </div>

    <!-- Question List -->
    <template v-else>
      <div class="table-wrapper mb-4">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 40px;">
                <input type="checkbox" :checked="allSelected" @change="selectAll" />
              </th>
              <th style="width: 60px;">ID</th>
              <th>题目</th>
              <th style="width: 90px;">分类</th>
              <th style="width: 80px;">题型</th>
              <th style="width: 80px;">难度</th>
              <th style="width: 200px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in questions" :key="q.id" :class="{ 'expanded': expandedId === q.id }">
              <td>
                <input type="checkbox" :checked="selectedIds.has(q.id)" @change="toggleSelect(q.id)" />
              </td>
              <td class="text-muted">#{{ q.id }}</td>
              <td>
                <div v-latex="q.stem" class="truncate" style="max-width: 350px;" :style="{ cursor: 'pointer', fontWeight: 500 }" @click="toggleExpand(q.id)">
                </div>
              </td>
              <td><span class="badge badge-gray">{{ q.category?.split('-')[0] || '' }}</span></td>
              <td>{{ questionTypeLabel(q.type) }}</td>
              <td>
                <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">
                  {{ q.difficulty }}
                </span>
              </td>
              <td>
                <div class="flex gap-2">
                  <button class="btn btn-ghost btn-sm" @click="toggleExpand(q.id)">{{ expandedId === q.id ? '收起' : '展开' }}</button>
                  <button class="btn btn-ghost btn-sm" @click="startEdit(q)">编辑</button>
                  <button class="btn btn-ghost btn-sm" style="color: var(--danger);" @click="deleteQuestion(q.id)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Expanded view -->
      <div v-if="expandedId" class="card mb-4">
        <div class="card-header">
          <h3>题目详情</h3>
          <button class="btn btn-ghost btn-sm" @click="expandedId = null">关闭</button>
        </div>
        <div class="card-body" v-for="q in questions.filter(q => q.id === expandedId)" :key="q.id">
          <div class="flex items-center gap-2 mb-4">
            <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ q.difficulty }}</span>
            <span class="badge badge-gray">{{ q.category }}</span>
            <span class="badge badge-cyan">{{ questionTypeLabel(q.type) }}</span>
          </div>
          <div class="q-stem" v-latex="q.stem"></div>
          <div v-if="q.options" class="mt-4">
            <div v-for="[k, v] in Object.entries(q.options)" :key="k" class="q-option" style="cursor: default;">
              <span class="q-option-key">{{ k }}</span>
              <span v-latex="v"></span>
            </div>
          </div>
          <div class="mt-4 p-4" style="background: var(--success-bg); border-radius: var(--radius-md);">
            <div style="font-weight: 600; color: var(--success); font-size: 13px; margin-bottom: 4px;">答案</div>
            <span v-latex="q.answer"></span>
          </div>
          <div v-if="q.analysis" class="mt-2 p-4" style="background: var(--primary-50); border-radius: var(--radius-md);">
            <div style="font-weight: 600; color: var(--primary); font-size: 13px; margin-bottom: 4px;">解析</div>
            <span v-latex="q.analysis"></span>
          </div>
          <div v-if="q.knowledgePoint" class="mt-2 text-sm text-muted">知识点：{{ q.knowledgePoint }}</div>
        </div>
      </div>

      <!-- Edit form -->
      <div v-if="editingId" class="card mb-4">
        <div class="card-header">
          <h3>编辑题目 #{{ editingId }}</h3>
          <button class="btn btn-ghost btn-sm" @click="cancelEdit">取消</button>
        </div>
        <div class="card-body">
          <div class="form-grid">
            <div class="form-group">
              <label>题型</label>
              <select class="form-select" v-model="editForm.type">
                <option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>难度</label>
              <select class="form-select" v-model="editForm.difficulty">
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </div>
            <div class="form-group">
              <label>学段</label>
              <select class="form-select" v-model="editLevel" @change="onEditLevelChange">
                <option value="">选择学段</option>
                <option v-for="lv in levels" :key="lv.label" :value="lv.label">{{ lv.label }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>学科</label>
              <select class="form-select" v-model="editForm.category">
                <option value="">选择学科</option>
                <option v-for="s in editSubjectOptions" :key="s" :value="`${editLevel}-${s}`">{{ s }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>题干</label>
            <textarea class="form-input" v-model="editForm.stem" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>答案</label>
            <input class="form-input" v-model="editForm.answer" />
          </div>
          <div class="form-group">
            <label>解析</label>
            <textarea class="form-input" v-model="editForm.analysis" rows="2"></textarea>
          </div>
          <div class="form-group">
            <label>知识点</label>
            <input class="form-input" v-model="editForm.knowledgePoint" />
          </div>
          <div class="flex gap-2 mt-4">
            <button class="btn btn-primary" @click="saveEdit">保存修改</button>
            <button class="btn btn-outline" @click="cancelEdit">取消</button>
          </div>
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
