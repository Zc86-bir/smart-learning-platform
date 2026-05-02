<script setup>
import { ref, onMounted } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const category = ref('')
const difficulty = ref('MEDIUM')
const knowledgePoint = ref('')
const count = ref(3)
const selectedModel = ref('')
const selectedTypes = ref(['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'])
const generating = ref(false)
const generated = ref([])
const error = ref('')
const editingIndex = ref(-1)
const editStem = ref('')
const editAnswer = ref('')
const editAnalysis = ref('')
const models = ref([])

const levels = [
  { label: '小学', subjects: ['语文', '数学', '英语', '科学'] },
  { label: '初中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治'] },
  { label: '高中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理'] },
  { label: '大学', subjects: ['高等数学', '线性代数', '概率论', '数据结构', '操作系统', '计算机网络', '数据库', '大学英语', '计算机组成原理', '软件工程'] }
]

const selectedLevel = ref('')
const subjectOptions = ref([])

const onLevelChange = () => {
  category.value = ''
  subjectOptions.value = levels.find(l => l.label === selectedLevel.value)?.subjects || []
}

const loadModels = async () => {
  try {
    models.value = await api('/admin/questions/models')
    if (models.value.length) {
      selectedModel.value = models.value[0]
    }
  } catch (e) {
    models.value = ['mimo-v2.5-pro', 'mimo-v2-pro', 'mimo-v2.5']
    selectedModel.value = 'mimo-v2.5-pro'
  }
}

const typeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'SHORT_ANSWER', label: '简答题' }
]

const toggleType = (value) => {
  const idx = selectedTypes.value.indexOf(value)
  if (idx === -1) selectedTypes.value.push(value)
  else if (selectedTypes.value.length > 1) selectedTypes.value.splice(idx, 1)
}

const generate = async () => {
  error.value = ''
  if (!category.value.trim()) {
    error.value = '请选择分类'
    return
  }
  generating.value = true
  try {
    generated.value = await api('/admin/questions/generate', {
      method: 'POST',
      body: JSON.stringify({
        category: category.value,
        difficulty: difficulty.value,
        knowledgePoint: knowledgePoint.value || null,
        count: count.value,
        model: selectedModel.value || null,
        types: selectedTypes.value.length > 0 ? selectedTypes.value : null
      })
    })
    editingIndex.value = -1
  } catch (e) {
    error.value = '出题失败: ' + e.message
    generated.value = []
  } finally {
    generating.value = false
  }
}

const saveQuestion = async (q) => {
  try {
    await api('/admin/questions', {
      method: 'POST',
      body: JSON.stringify({
        type: q.type,
        category: q.category || category.value,
        stem: q.stem,
        options: q.options,
        answer: q.answer,
        analysis: q.analysis || '',
        difficulty: q.difficulty,
        knowledgePoint: q.knowledgePoint || knowledgePoint.value || ''
      })
    })
    q.saved = true
  } catch (e) {
    alert('保存失败: ' + e.message)
  }
}

const saveAll = async () => {
  for (const q of generated.value) {
    if (!q.saved) await saveQuestion(q)
  }
  alert('全部保存成功')
}

const startEdit = (i) => {
  const q = generated.value[i]
  editingIndex.value = i
  editStem.value = q.stem
  editAnswer.value = q.answer
  editAnalysis.value = q.analysis || ''
}

const saveEdit = () => {
  const i = editingIndex.value
  if (i >= 0) {
    generated.value[i].stem = editStem.value
    generated.value[i].answer = editAnswer.value
    generated.value[i].analysis = editAnalysis.value
  }
  editingIndex.value = -1
}

const removeQuestion = (i) => {
  generated.value.splice(i, 1)
  if (editingIndex.value === i) editingIndex.value = -1
}

const savedCount = () => generated.value.filter(q => q.saved).length

onMounted(() => loadModels())
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>AI 智能出题</h2>
        <p>配置参数后由 AI 自动生成题目，支持多题型和知识点定制</p>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>出题配置</h3>
      </div>
      <div class="card-body">
        <div class="form-grid">
          <div class="form-group">
            <label>学段</label>
            <select class="form-select" v-model="selectedLevel" @change="onLevelChange">
              <option value="">请选择学段</option>
              <option v-for="lv in levels" :key="lv.label" :value="lv.label">{{ lv.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>学科</label>
            <select class="form-select" v-model="category">
              <option value="">请选择学科</option>
              <option v-for="s in subjectOptions" :key="s" :value="`${selectedLevel}-${s}`">{{ s }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>难度</label>
            <select class="form-select" v-model="difficulty">
              <option value="EASY">简单</option>
              <option value="MEDIUM">中等</option>
              <option value="HARD">困难</option>
            </select>
          </div>
          <div class="form-group">
            <label>知识点</label>
            <input class="form-input" v-model="knowledgePoint" placeholder="例如: HashMap底层结构" />
          </div>
          <div class="form-group">
            <label>数量</label>
            <input class="form-input" type="number" v-model.number="count" min="1" max="10" />
          </div>
          <div class="form-group">
            <label>模型</label>
            <select class="form-select" v-model="selectedModel">
              <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
            </select>
          </div>
        </div>

        <!-- Type selection -->
        <div class="form-group mt-4">
          <label>题型选择</label>
          <div class="flex gap-2 flex-wrap mt-2">
            <button
              v-for="t in typeOptions"
              :key="t.value"
              :class="['btn btn-sm', selectedTypes.includes(t.value) ? 'btn-primary' : 'btn-outline']"
              @click="toggleType(t.value)"
            >{{ t.label }}</button>
          </div>
        </div>

        <p v-if="error" class="text-sm mt-2" style="color: var(--danger);">{{ error }}</p>
        <button class="btn btn-primary mt-4" @click="generate" :disabled="generating || !category">
          <svg v-if="!generating" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
          {{ generating ? 'AI生成中...' : '生成题目' }}
        </button>
      </div>
    </div>

    <!-- Results -->
    <div v-if="generated.length" class="card mt-4">
      <div class="card-header">
        <h3>生成结果 <span class="text-muted font-semibold" style="font-weight: 400;">({{ savedCount() }}/{{ generated.length }} 已保存)</span></h3>
        <button class="btn btn-primary btn-sm" @click="saveAll" :disabled="savedCount() === generated.length">全部保存</button>
      </div>

      <div v-for="(q, i) in generated" :key="i">
        <div class="q-card">
          <div class="q-header">
            <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ q.difficulty || difficulty }}</span>
            <span class="badge badge-gray">{{ q.category || category }}</span>
            <span class="badge badge-cyan">{{ questionTypeLabel(q.type) }}</span>
            <span v-if="q.saved" class="badge badge-green">✓ 已保存</span>
            <div style="flex: 1;"></div>
            <button class="btn btn-ghost btn-sm btn-icon" @click="removeQuestion(i)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>

          <!-- Edit mode -->
          <div v-if="editingIndex === i">
            <div class="form-group">
              <label>题干</label>
              <textarea class="form-input" v-model="editStem" rows="3"></textarea>
            </div>
            <div class="form-group">
              <label>答案</label>
              <input class="form-input" v-model="editAnswer" />
            </div>
            <div class="form-group">
              <label>解析</label>
              <textarea class="form-input" v-model="editAnalysis" rows="2"></textarea>
            </div>
            <div class="flex gap-2 mt-2">
              <button class="btn btn-sm btn-primary" @click="saveEdit">保存修改</button>
              <button class="btn btn-sm btn-outline" @click="editingIndex = -1">取消</button>
            </div>
          </div>

          <!-- View mode -->
          <template v-else>
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
          </template>

          <!-- Actions -->
          <div v-if="editingIndex !== i" class="flex gap-2 mt-2">
            <button v-if="!q.saved" class="btn btn-sm btn-primary" @click="saveQuestion(q)">保存</button>
            <button v-if="editingIndex !== i" class="btn btn-sm btn-outline" @click="startEdit(i)">编辑</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
