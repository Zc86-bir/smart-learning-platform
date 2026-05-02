<script setup>
import { ref, computed, onMounted } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const levels = [
  { label: '小学', subjects: ['语文', '数学', '英语', '科学'] },
  { label: '初中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治'] },
  { label: '高中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理'] },
  { label: '大学', subjects: ['高等数学', '线性代数', '概率论', '数据结构', '操作系统', '计算机网络', '数据库', '大学英语', '计算机组成原理', '软件工程'] }
]

const selectedLevel = ref('')
const selectedSubject = ref('')
const subjectOptions = ref([])
const title = ref('')
const totalScore = ref(100)
const durationMinutes = ref(60)
const knowledgePoint = ref('')
const selectedModel = ref('')
const models = ref([])

const difficultyDist = ref({ EASY: 30, MEDIUM: 50, HARD: 20 })
const typeDist = ref({ SINGLE_CHOICE: 40, MULTIPLE_CHOICE: 30, TRUE_FALSE: 20, SHORT_ANSWER: 10 })

const generating = ref(false)
const generated = ref([])
const error = ref('')
const saving = ref(false)
const toast = ref({ show: false, message: '', type: '' })

const showPreview = ref(false)
const showHistory = ref(false)
const paperHistory = ref([])
const historyLoading = ref(false)

const editingScores = ref({})
const showScoreEditor = ref(false)

// Distribution validation
const difficultyValid = computed(() => {
  const total = Object.values(difficultyDist.value).reduce((s, v) => s + v, 0)
  return total === 100
})
const typeValid = computed(() => {
  const total = Object.values(typeDist.value).reduce((s, v) => s + v, 0)
  return total === 100
})

// Score summary
const totalGeneratedScore = computed(() => generated.value.reduce((s, q) => s + (q.score || 0), 0))
const scoreMismatch = computed(() => totalScore.value !== totalGeneratedScore.value)
const scoreDiff = computed(() => totalScore.value - totalGeneratedScore.value)

// Preview groups
const previewGroups = computed(() => {
  const groups = {}
  for (const q of generated.value) {
    const label = questionTypeLabel(q.type)
    if (!groups[label]) groups[label] = []
    groups[label].push(q)
  }
  return groups
})

// Question stats summary
const questionStats = computed(() => {
  const byType = {}
  const byDiff = {}
  for (const q of generated.value) {
    const tLabel = questionTypeLabel(q.type)
    byType[tLabel] = (byType[tLabel] || 0) + 1
    byDiff[q.difficulty] = (byDiff[q.difficulty] || 0) + 1
  }
  return { byType, byDiff }
})

const onLevelChange = () => {
  selectedSubject.value = ''
  subjectOptions.value = levels.find(l => l.label === selectedLevel.value)?.subjects || []
}

const showToast = (message, type = 'success') => {
  toast.value = { show: true, message, type }
  setTimeout(() => { toast.value.show = false }, 3000)
}

const loadModels = async () => {
  try {
    models.value = await api('/admin/questions/models')
    if (models.value.length) selectedModel.value = models.value[0]
  } catch {
    models.value = ['mimo-v2.5-pro', 'mimo-v2-pro', 'mimo-v2.5']
    selectedModel.value = 'mimo-v2.5-pro'
  }
}

const loadPaperHistory = async () => {
  historyLoading.value = true
  try {
    const result = await api('/admin/papers?page=1&size=10')
    paperHistory.value = result?.records || []
  } catch {
    paperHistory.value = []
  } finally {
    historyLoading.value = false
  }
}

const adjustDist = (dist, key, delta) => {
  const newVal = Math.max(0, Math.min(100, dist.value[key] + delta))
  dist.value[key] = newVal
  const others = Object.keys(dist.value).filter(k => k !== key)
  const sumOthers = others.reduce((s, k) => s + dist.value[k], 0)
  const target = 100 - newVal
  if (sumOthers !== target && sumOthers > 0) {
    const ratio = target / sumOthers
    others.forEach(k => {
      dist.value[k] = Math.round(dist.value[k] * ratio)
    })
    const total = Object.values(dist.value).reduce((s, v) => s + v, 0)
    if (total !== 100) {
      dist.value[others[0]] += 100 - total
    }
  }
}

const validateDist = (dist) => {
  const total = Object.values(dist).reduce((s, v) => s + v, 0)
  return total === 100
}

const generate = async () => {
  error.value = ''
  if (!selectedSubject.value) { error.value = '请选择学科'; return }
  if (!title.value.trim()) { error.value = '请输入试卷标题'; return }
  if (!validateDist(difficultyDist.value)) { error.value = '难度分布之和必须为100%'; return }
  if (!validateDist(typeDist.value)) { error.value = '题型分布之和必须为100%'; return }

  generating.value = true
  const category = `${selectedLevel.value}-${selectedSubject.value}`

  try {
    generated.value = await api('/admin/papers/smart-generate', {
      method: 'POST',
      body: JSON.stringify({
        title: title.value,
        category,
        level: selectedLevel.value,
        totalScore: totalScore.value,
        durationMinutes: durationMinutes.value,
        difficultyDist: difficultyDist.value,
        typeDist: typeDist.value,
        knowledgePoint: knowledgePoint.value || null,
        model: selectedModel.value || null
      })
    })
    editingScores.value = {}
    showScoreEditor.value = false
    showToast(`成功生成 ${generated.value.length} 道题目`)
  } catch (e) {
    error.value = 'AI组卷失败: ' + e.message
    generated.value = []
    showToast('AI组卷失败: ' + e.message, 'error')
  } finally {
    generating.value = false
  }
}

const regenerateQuestion = async (index) => {
  if (!generated.value[index]) return
  const q = generated.value[index]

  try {
    const category = `${selectedLevel.value}-${selectedSubject.value}`
    const result = await api('/admin/papers/smart-generate', {
      method: 'POST',
      body: JSON.stringify({
        title: title.value,
        category,
        level: selectedLevel.value,
        totalScore: q.score || 10,
        durationMinutes: durationMinutes.value,
        difficultyDist: { [q.difficulty]: 100 },
        typeDist: { [q.type]: 100 },
        knowledgePoint: knowledgePoint.value || null,
        model: selectedModel.value || null,
        count: 1
      })
    })
    if (result && result.length > 0) {
      result[0].score = q.score
      generated.value[index] = result[0]
      showToast('已重新生成第 ' + (index + 1) + ' 题')
    }
  } catch (e) {
    showToast('重新生成失败: ' + e.message, 'error')
  }
}

const removeQuestion = (index) => {
  generated.value.splice(index, 1)
  showToast('已删除该题目')
}

const saveAll = async () => {
  saving.value = true
  const category = `${selectedLevel.value}-${selectedSubject.value}`
  const savedQuestions = generated.value.map(q => ({ id: q.questionId, score: q.score }))
  try {
    await api('/admin/papers', {
      method: 'POST',
      body: JSON.stringify({
        title: title.value,
        description: category,
        totalScore: totalGeneratedScore.value,
        durationMinutes: durationMinutes.value,
        questionIds: JSON.stringify(savedQuestions)
      })
    })
    showToast('试卷保存成功')
  } catch (e) {
    showToast('保存失败: ' + e.message, 'error')
  } finally {
    saving.value = false
  }
}

function highlightText(text) {
  if (!text) return ''
  return text
    .replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\$(.*?)\$/g, '<strong>$1</strong>')
}

function stripLatexMarkers(text) {
  if (!text) return ''
  return text.replace(/\$\$?([^$]*?)\$\$?/g, '$1')
}

onMounted(() => { loadModels(); loadPaperHistory() })
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>智能组卷</h2>
        <p>配置试卷参数，AI 智能生成试卷内容</p>
      </div>
      <button class="btn btn-outline" @click="showHistory = !showHistory">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
        </svg>
        历史试卷
      </button>
    </div>

    <!-- Paper History -->
    <div v-if="showHistory" class="card mb-4">
      <div class="card-header">
        <h3>已保存试卷</h3>
        <button class="btn btn-ghost btn-sm" @click="loadPaperHistory" :disabled="historyLoading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
          </svg>
          刷新
        </button>
      </div>
      <div class="card-body">
        <div v-if="historyLoading" class="text-center text-muted" style="padding: 20px;">
          <div class="spinner" style="margin: 0 auto;"></div>
          <div class="mt-2">加载中...</div>
        </div>
        <div v-else-if="!paperHistory.length" class="empty-state" style="padding: 30px;">
          <div class="text-muted">暂无已保存的试卷</div>
        </div>
        <div v-else class="table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>分类</th>
                <th>总分</th>
                <th>时长</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in paperHistory" :key="p.id">
                <td class="font-semibold">{{ p.title }}</td>
                <td><span class="badge badge-blue">{{ p.description }}</span></td>
                <td>{{ p.totalScore }} 分</td>
                <td>{{ p.durationMinutes }} 分钟</td>
                <td class="text-muted">{{ p.createdAt }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Config Form -->
    <div class="card">
      <div class="card-header">
        <h3>试卷配置</h3>
      </div>
      <div class="card-body">
        <div class="form-grid">
          <div class="form-group" style="grid-column: span 2;">
            <label>试卷标题</label>
            <input class="form-input" v-model="title" placeholder="例如: 2024年秋季期中考试" />
          </div>
          <div class="form-group">
            <label>学段</label>
            <select class="form-select" v-model="selectedLevel" @change="onLevelChange">
              <option value="">请选择学段</option>
              <option v-for="lv in levels" :key="lv.label" :value="lv.label">{{ lv.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>学科</label>
            <select class="form-select" v-model="selectedSubject">
              <option value="">请选择学科</option>
              <option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>总分</label>
            <input class="form-input" type="number" v-model.number="totalScore" min="10" max="500" />
          </div>
          <div class="form-group">
            <label>考试时长（分钟）</label>
            <input class="form-input" type="number" v-model.number="durationMinutes" min="10" max="300" />
          </div>
          <div class="form-group">
            <label>知识点（可选）</label>
            <input class="form-input" v-model="knowledgePoint" placeholder="例如: 函数与导数" />
          </div>
          <div class="form-group">
            <label>模型</label>
            <select class="form-select" v-model="selectedModel">
              <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
            </select>
          </div>
        </div>

        <!-- Difficulty distribution -->
        <div class="mt-4">
          <label style="display: block; font-size: 13px; font-weight: 500; color: var(--slate-700); margin-bottom: 8px;">
            难度分布
            <span v-if="!difficultyValid" style="color: var(--danger); font-weight: 400; font-size: 12px; margin-left: 8px;">
              （当前合计 {{ Object.values(difficultyDist).reduce((a, b) => a + b, 0) }}%，需为 100%）
            </span>
          </label>
          <div class="flex gap-4 flex-wrap" style="align-items: flex-start;">
            <div v-for="(val, key) in difficultyDist" :key="key"
                 :style="{ flex: '1', minWidth: '150px', padding: '12px', border: '1px solid', borderColor: difficultyValid ? 'var(--slate-200)' : 'var(--danger)', borderRadius: 'var(--radius-md)', background: difficultyValid ? 'var(--slate-50)' : 'var(--danger-bg)' }">
              <div class="text-sm font-semibold mb-2">{{ key === 'EASY' ? '简单' : key === 'MEDIUM' ? '中等' : '困难' }}</div>
              <div class="flex items-center gap-2">
                <button class="btn btn-sm btn-outline" @click="adjustDist(difficultyDist, key, -5)">-5</button>
                <input class="form-input" type="number" v-model.number="difficultyDist[key]" style="width: 60px; text-align: center; padding: 4px;" />
                <button class="btn btn-sm btn-outline" @click="adjustDist(difficultyDist, key, 5)">+5</button>
                <span class="text-sm text-muted">%</span>
              </div>
              <!-- Visual bar -->
              <div style="margin-top: 8px; height: 4px; background: var(--slate-200); border-radius: 2px; overflow: hidden;">
                <div :style="{ height: '100%', width: val + '%', background: key === 'EASY' ? 'var(--success)' : key === 'HARD' ? 'var(--danger)' : 'var(--warning)', transition: 'width 0.2s' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Type distribution -->
        <div class="mt-4">
          <label style="display: block; font-size: 13px; font-weight: 500; color: var(--slate-700); margin-bottom: 8px;">
            题型分布
            <span v-if="!typeValid" style="color: var(--danger); font-weight: 400; font-size: 12px; margin-left: 8px;">
              （当前合计 {{ Object.values(typeDist).reduce((a, b) => a + b, 0) }}%，需为 100%）
            </span>
          </label>
          <div class="flex gap-4 flex-wrap" style="align-items: flex-start;">
            <div v-for="(val, key) in typeDist" :key="key"
                 :style="{ flex: '1', minWidth: '150px', padding: '12px', border: '1px solid', borderColor: typeValid ? 'var(--slate-200)' : 'var(--danger)', borderRadius: 'var(--radius-md)', background: typeValid ? 'var(--slate-50)' : 'var(--danger-bg)' }">
              <div class="text-sm font-semibold mb-2">{{ questionTypeLabel(key) }}</div>
              <div class="flex items-center gap-2">
                <button class="btn btn-sm btn-outline" @click="adjustDist(typeDist, key, -5)">-5</button>
                <input class="form-input" type="number" v-model.number="typeDist[key]" style="width: 60px; text-align: center; padding: 4px;" />
                <button class="btn btn-sm btn-outline" @click="adjustDist(typeDist, key, 5)">+5</button>
                <span class="text-sm text-muted">%</span>
              </div>
              <div style="margin-top: 8px; height: 4px; background: var(--slate-200); border-radius: 2px; overflow: hidden;">
                <div :style="{ height: '100%', width: val + '%', background: 'var(--primary)', transition: 'width 0.2s' }"></div>
              </div>
            </div>
          </div>
        </div>

        <p v-if="error" class="text-sm mt-4" style="color: var(--danger);">{{ error }}</p>
        <button class="btn btn-primary mt-4" @click="generate"
                :disabled="generating || !selectedSubject || !title || !difficultyValid || !typeValid">
          <svg v-if="generating" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation: spin 0.6s linear infinite;">
            <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
          </svg>
          <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
          {{ generating ? 'AI组卷中...' : '智能组卷' }}
        </button>
      </div>
    </div>

    <!-- Toast -->
    <div v-if="toast.show"
         :class="['toast', toast.type === 'error' ? 'toast-error' : 'toast-success']">
      <svg v-if="toast.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
      </svg>
      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      {{ toast.message }}
    </div>

    <!-- Results -->
    <div v-if="generated.length" class="card mt-4">
      <div class="card-header">
        <h3>组卷结果</h3>
        <div class="flex items-center gap-2">
          <span class="badge badge-blue">共 {{ generated.length }} 题</span>
          <span class="badge badge-green">总分 {{ totalGeneratedScore }}</span>
          <span v-if="scoreMismatch" class="badge badge-yellow">
            {{ scoreDiff > 0 ? '少 ' : '多 ' }}{{ Math.abs(scoreDiff) }} 分
          </span>
          <button class="btn btn-outline btn-sm" @click="showPreview = true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
            </svg>
            预览试卷
          </button>
          <button class="btn btn-outline btn-sm" @click="showScoreEditor = !showScoreEditor">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
            </svg>
            调分
          </button>
          <button class="btn btn-primary btn-sm" @click="saveAll" :disabled="saving">
            <svg v-if="saving" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="animation: spin 0.6s linear infinite;">
              <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
            </svg>
            <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/>
            </svg>
            {{ saving ? '保存中...' : '保存试卷' }}
          </button>
        </div>
      </div>

      <!-- Score Editor -->
      <div v-if="showScoreEditor" class="p-4" style="background: var(--slate-50); border-bottom: 1px solid var(--slate-200);">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm font-semibold">分数调整</span>
          <span class="text-sm text-muted">当前总分: {{ totalGeneratedScore }} / {{ totalScore }}</span>
        </div>
        <div style="display: flex; flex-wrap: wrap; gap: 8px;">
          <div v-for="(q, i) in generated" :key="i" style="display: flex; align-items: center; gap: 4px; padding: 4px 8px; background: #fff; border-radius: var(--radius-sm); border: 1px solid var(--slate-200); font-size: 12px;">
            <span class="text-muted">Q{{ i + 1 }}</span>
            <button class="btn btn-sm btn-outline" style="padding: 2px 6px;" @click="q.score = Math.max(1, (q.score || 1) - 1)">-</button>
            <input class="form-input" type="number" v-model.number="q.score" style="width: 45px; padding: 2px 4px; text-align: center; font-size: 12px;" min="1" />
            <button class="btn btn-sm btn-outline" style="padding: 2px 6px;" @click="q.score = (q.score || 0) + 1">+</button>
          </div>
        </div>
      </div>

      <!-- Question Stats -->
      <div v-if="generated.length" class="p-4" style="background: var(--primary-50); border-bottom: 1px solid var(--slate-200);">
        <div class="flex gap-4 flex-wrap">
          <div class="text-sm">
            <span class="text-muted">题型: </span>
            <span v-for="(cnt, type) in questionStats.byType" :key="type">
              {{ type }}×{{ cnt }}
            </span>
          </div>
          <div class="text-sm">
            <span class="text-muted">难度: </span>
            <span :class="['badge', 'badge-green']">简单 {{ questionStats.byDiff.EASY || 0 }}</span>
            <span :class="['badge', 'badge-yellow']">中等 {{ questionStats.byDiff.MEDIUM || 0 }}</span>
            <span :class="['badge', 'badge-red']">困难 {{ questionStats.byDiff.HARD || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- Question Cards -->
      <div v-for="(q, i) in generated" :key="i" class="q-card">
        <div class="q-header">
          <span class="text-sm text-muted" style="font-weight: 600;">Q{{ i + 1 }}</span>
          <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ q.difficulty }}</span>
          <span class="badge badge-cyan">{{ questionTypeLabel(q.type) }}</span>
          <span class="badge badge-blue font-semibold">{{ q.score }} 分</span>
          <div style="margin-left: auto; display: flex; gap: 4px;">
            <button class="btn btn-ghost btn-sm" @click="regenerateQuestion(i)" :disabled="generating" title="重新生成">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
              </svg>
            </button>
            <button class="btn btn-ghost btn-sm" @click="removeQuestion(i)" title="删除" style="color: var(--danger);">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="q-stem" v-latex="q.stem"></div>
        <div v-if="q.options" class="mt-2">
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
      </div>
    </div>

    <!-- Preview Modal -->
    <div v-if="showPreview" class="modal-overlay" @click.self="showPreview = false">
      <div class="modal-content" style="max-width: 800px; max-height: 85vh; overflow-y: auto;">
        <div class="flex items-center justify-between mb-4">
          <h3 style="font-size: 18px; font-weight: 700;">试卷预览</h3>
          <button class="btn btn-ghost btn-sm" @click="showPreview = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div style="text-align: center; padding: 20px 0; border-bottom: 2px solid var(--slate-200); margin-bottom: 20px;">
          <h2 style="font-size: 20px; font-weight: 700; margin-bottom: 8px;">{{ title || '智能组卷' }}</h2>
          <div class="text-sm text-muted">总分：{{ totalGeneratedScore }} 分 &nbsp;|&nbsp; 时长：{{ durationMinutes }} 分钟 &nbsp;|&nbsp; 共 {{ generated.length }} 题</div>
        </div>
        <div v-for="(questions, typeName) in previewGroups" :key="typeName">
          <h4 style="font-size: 15px; font-weight: 600; margin-bottom: 12px; color: var(--slate-700); border-bottom: 1px solid var(--slate-200); padding-bottom: 6px;">
            {{ typeName }}（共 {{ questions.length }} 题）
          </h4>
          <div v-for="(q, qi) in questions" :key="qi" class="mb-4">
            <div class="text-sm" style="color: var(--slate-600);">
              <strong>{{ qi + 1 }}.</strong>
              <span style="margin-left: 4px;" v-html="highlightText(stripLatexMarkers(q.stem)) || ''"></span>
              <span style="color: var(--slate-400);">（{{ q.score }} 分）</span>
            </div>
            <div v-if="q.options" class="ml-6 mt-1">
              <div v-for="[k, v] in Object.entries(q.options)" :key="k" style="margin: 2px 0; font-size: 13px;">
                {{ k }}. <span v-html="highlightText(stripLatexMarkers(v))"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
