<script setup>
import { ref, computed } from 'vue'
import { api, headers, questionTypeLabel } from '../composables/api'

const levels = [
  { label: '小学', subjects: ['语文', '数学', '英语', '科学'] },
  { label: '初中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '道德与法治'] },
  { label: '高中', subjects: ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理'] },
  { label: '大学', subjects: ['高等数学', '线性代数', '概率论', '数据结构', '操作系统', '计算机网络', '数据库', '大学英语', '计算机组成原理', '软件工程'] }
]

// Phase: 'setup' | 'practice' | 'report'
const phase = ref('setup')

// Setup
const selectedLevel = ref('')
const selectedSubject = ref('')
const subjectOptions = ref([])
const selectedDifficulty = ref('')
const questionCount = ref(10)

// Practice
const sessionId = ref(null)
const questions = ref([])
const currentIndex = ref(0)
const answers = ref([])
const immediateResults = ref({})

// Report
const report = ref(null)

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const totalQuestions = computed(() => questions.value.length)
const answeredCount = computed(() => Object.keys(immediateResults.value).length)
const correctCount = computed(() => Object.values(immediateResults.value).filter(r => r.correct).length)

function onLevelChange() {
  selectedSubject.value = ''
  subjectOptions.value = levels.find(l => l.label === selectedLevel.value)?.subjects || []
}

function buildCategory() {
  if (!selectedLevel.value || !selectedSubject.value) return ''
  return `${selectedLevel.value}-${selectedSubject.value}`
}

async function startPractice() {
  try {
    const body = {
      category: buildCategory(),
      difficulty: selectedDifficulty.value || undefined,
      count: questionCount.value
    }
    const result = await api('/student/practice/start', {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(body)
    })
    sessionId.value = result.sessionId
    questions.value = result.questions || []
    answers.value = new Array(questions.value.length).fill('')
    immediateResults.value = {}
    currentIndex.value = 0
    phase.value = 'practice'
  } catch (e) {
    alert('开始练习失败: ' + e.message)
  }
}

async function submitCurrentAnswer() {
  if (!currentQuestion.value) return
  const answer = answers.value[currentIndex.value]
  if (!answer || !answer.trim()) return

  try {
    const result = await api(`/student/practice/${sessionId.value}/answer`, {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify({
        questionId: currentQuestion.value.id,
        answer: answer.trim()
      })
    })
    immediateResults.value[currentQuestion.value.id] = result
  } catch (e) {
    alert('提交答案失败: ' + e.message)
  }
}

async function finishPractice() {
  try {
    const result = await api(`/student/practice/${sessionId.value}/finish`, {
      method: 'POST',
      headers: headers()
    })
    report.value = result
    phase.value = 'report'
  } catch (e) {
    alert('结束练习失败: ' + e.message)
  }
}

async function viewReport() {
  try {
    const result = await api(`/student/practice/${sessionId.value}/report`, {
      method: 'GET',
      headers: headers()
    })
    report.value = result
    phase.value = 'report'
  } catch (e) {
    alert('加载报告失败: ' + e.message)
  }
}

function backToSetup() {
  phase.value = 'setup'
  sessionId.value = null
  questions.value = []
  report.value = null
}

const difficultyLabel = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
</script>

<template>
  <div class="practice-page">
    <!-- Setup Phase -->
    <div v-if="phase === 'setup'" class="setup-card">
      <div class="setup-header">
        <h2 class="setup-title">题库练习</h2>
        <p class="setup-subtitle">选择练习范围，开始高效学习</p>
      </div>

      <div class="setup-form">
        <div class="form-group">
          <label class="form-label">学段</label>
          <select class="form-select" v-model="selectedLevel" @change="onLevelChange">
            <option value="">全部学段</option>
            <option v-for="lv in levels" :key="lv.label" :value="lv.label">{{ lv.label }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">学科</label>
          <select class="form-select" v-model="selectedSubject">
            <option value="">全部学科</option>
            <option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">难度</label>
          <div class="difficulty-selector">
            <button
              v-for="d in [{v: '', l: '不限'}, {v: 'EASY', l: '简单'}, {v: 'MEDIUM', l: '中等'}, {v: 'HARD', l: '困难'}]"
              :key="d.v"
              :class="['diff-btn', { active: selectedDifficulty === d.v }]"
              @click="selectedDifficulty = d.v"
            >
              {{ d.l }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">题目数量</label>
          <div class="count-selector">
            <button
              v-for="c in [5, 10, 15, 20, 30]"
              :key="c"
              :class="['count-btn', { active: questionCount === c }]"
              @click="questionCount = c"
            >
              {{ c }}
            </button>
          </div>
        </div>
      </div>

      <button class="btn btn-primary btn-lg btn-start" @click="startPractice">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="5,3 19,12 5,21"/>
        </svg>
        开始练习
      </button>
    </div>

    <!-- Practice Phase -->
    <div v-if="phase === 'practice'">
      <div class="practice-header">
        <div class="practice-progress">
          <span class="progress-text">第 {{ currentIndex + 1 }} / {{ totalQuestions }} 题</span>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: ((currentIndex + 1) / totalQuestions * 100) + '%' }"></div>
          </div>
        </div>
        <div class="practice-stats">
          <span class="stat-item">
            <span class="stat-dot stat-correct"></span>
            正确: {{ correctCount }}
          </span>
          <span class="stat-item">
            <span class="stat-dot stat-total"></span>
            已答: {{ answeredCount }}
          </span>
        </div>
      </div>

      <div v-if="currentQuestion" class="practice-question-card">
        <div class="question-badges">
          <span class="badge badge-cyan">{{ questionTypeLabel(currentQuestion.type) }}</span>
          <span class="badge badge-gray">{{ currentQuestion.category }}</span>
          <span :class="['badge',
            currentQuestion.difficulty === 'EASY' ? 'badge-green' :
            currentQuestion.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow'
          ]">{{ difficultyLabel[currentQuestion.difficulty] || currentQuestion.difficulty }}</span>
        </div>

        <div class="question-stem" :key="'stem-' + currentQuestion.id" v-latex="currentQuestion.stem"></div>

        <!-- Options for choice/true-false questions -->
        <div v-if="currentQuestion.options" class="question-options">
          <button
            v-for="[k, v] in Object.entries(currentQuestion.options)"
            :key="k"
            :class="['option-btn', { selected: answers[currentIndex] === k }]"
            @click="answers[currentIndex] = k"
          >
            <span class="option-key">{{ k }}</span>
            <span :key="'opt-' + currentQuestion.id + '-' + k" v-latex="v"></span>
          </button>
        </div>

        <!-- Text input for short-answer questions -->
        <div v-if="currentQuestion.type === 'SHORT_ANSWER' || currentQuestion.type === 'CODING'" class="question-textarea">
          <textarea
            v-model="answers[currentIndex]"
            rows="6"
            placeholder="请输入你的答案..."
            class="answer-textarea"
          ></textarea>
        </div>

        <!-- Immediate feedback after submitting answer -->
        <div v-if="immediateResults[currentQuestion.id]" class="feedback-section">
          <div :class="['feedback-card', immediateResults[currentQuestion.id].correct ? 'feedback-correct' : 'feedback-wrong']">
            <div class="feedback-header">
              <svg v-if="immediateResults[currentQuestion.id].correct" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
              <span class="feedback-text">
                {{ immediateResults[currentQuestion.id].correct ? '回答正确!' : '回答错误' }}
              </span>
            </div>
            <div class="feedback-answer">
              <strong>正确答案: </strong>
              <span :key="'ans-' + currentQuestion.id" v-latex="immediateResults[currentQuestion.id].correctAnswer"></span>
            </div>
            <div v-if="immediateResults[currentQuestion.id].analysis" class="feedback-analysis">
              <strong>解析: </strong>
              <span :key="'ana-' + currentQuestion.id" v-latex="immediateResults[currentQuestion.id].analysis"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="practice-actions">
        <button
          v-if="!immediateResults[currentQuestion?.id] && currentQuestion"
          class="btn btn-primary"
          @click="submitCurrentAnswer"
        >
          提交答案
        </button>

        <button
          v-if="immediateResults[currentQuestion?.id]"
          class="btn btn-outline"
          @click="currentIndex < totalQuestions - 1 ? currentIndex++ : finishPractice()"
        >
          {{ currentIndex < totalQuestions - 1 ? '下一题' : '结束练习' }}
        </button>

        <button class="btn btn-ghost" @click="finishPractice">
          直接结束
        </button>
      </div>
    </div>

    <!-- Report Phase -->
    <div v-if="phase === 'report' && report" class="report-page">
      <div class="report-header">
        <h2 class="report-title">练习报告</h2>
        <button class="btn btn-ghost" @click="backToSetup">返回练习首页</button>
      </div>

      <div class="report-summary">
        <div class="report-score">
          <div class="score-number">{{ report.accuracy }}%</div>
          <div class="score-label">正确率</div>
        </div>
        <div class="report-stats">
          <div class="report-stat">
            <div class="stat-value">{{ report.questionCount }}</div>
            <div class="stat-name">题目总数</div>
          </div>
          <div class="report-stat stat-correct">
            <div class="stat-value">{{ report.correctCount }}</div>
            <div class="stat-name">正确数</div>
          </div>
          <div class="report-stat stat-wrong">
            <div class="stat-value">{{ report.questionCount - report.correctCount }}</div>
            <div class="stat-name">错误数</div>
          </div>
        </div>
      </div>

      <div class="report-details">
        <h3 class="details-title">详细结果</h3>
        <div v-for="(q, idx) in report.questions" :key="q.id" class="report-question">
          <div class="report-question-header">
            <span class="report-question-num">第 {{ idx + 1 }} 题</span>
            <span :class="['report-result-badge', q.correct ? 'badge-green' : 'badge-red']">
              {{ q.correct ? '正确' : '错误' }}
            </span>
          </div>
          <div class="report-question-stem" :key="'rstem-' + q.id" v-latex="q.stem"></div>
          <div class="report-question-answers">
            <div class="report-answer-item">
              <span class="report-answer-label">你的答案:</span>
              <span :key="'rua-' + q.id" v-latex="q.userAnswer || '未作答'"></span>
            </div>
            <div v-if="!q.correct" class="report-answer-item">
              <span class="report-answer-label">正确答案:</span>
              <span :key="'rca-' + q.id" v-latex="q.correctAnswer"></span>
            </div>
            <div v-if="q.analysis" class="report-answer-item report-analysis">
              <span class="report-answer-label">解析:</span>
              <span :key="'ran-' + q.id" v-latex="q.analysis"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.practice-page {
  padding: 2rem;
  max-width: 900px;
  margin: 0 auto;
}

/* Setup Phase */
.setup-card {
  background: var(--bg-secondary, #fff);
  border-radius: 16px;
  padding: 2.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.setup-header {
  text-align: center;
  margin-bottom: 2rem;
}

.setup-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 0.25rem;
}

.setup-subtitle {
  color: var(--text-secondary, #64748b);
  font-size: 0.95rem;
  margin: 0;
}

.setup-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  font-size: 0.9rem;
}

.form-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  font-size: 0.875rem;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1a1a2e);
}

.difficulty-selector {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.diff-btn {
  padding: 0.4rem 1.25rem;
  border-radius: 2rem;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-primary, #fff);
  color: var(--text-secondary, #64748b);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.diff-btn:hover {
  border-color: var(--primary, #3b82f6);
  color: var(--primary, #3b82f6);
}

.diff-btn.active {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

.count-selector {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.count-btn {
  width: 3rem;
  height: 2.5rem;
  border-radius: 8px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1a1a2e);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.count-btn:hover {
  border-color: var(--primary, #3b82f6);
}

.count-btn.active {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

.btn-start {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  font-size: 1rem;
  font-weight: 600;
}

/* Practice Phase */
.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 1rem 1.25rem;
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.practice-progress {
  flex: 1;
}

.progress-text {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
}

.progress-bar {
  height: 6px;
  background: var(--bg-tertiary, #f1f5f9);
  border-radius: 3px;
  margin-top: 0.5rem;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--primary, #3b82f6);
  border-radius: 3px;
  transition: width 0.3s;
}

.practice-stats {
  display: flex;
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
}

.stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.stat-correct { background: #22c55e; }
.stat-total { background: #3b82f6; }

.practice-question-card {
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 1.5rem;
}

.question-badges {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.badge {
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-cyan { background: #dbeafe; color: #1d4ed8; }
.badge-gray { background: #f1f5f9; color: #475569; }
.badge-green { background: #dcfce7; color: #16a34a; }
.badge-yellow { background: #fef9c3; color: #a16207; }
.badge-red { background: #fecaca; color: #dc2626; }

.question-stem {
  font-size: 1.05rem;
  line-height: 1.7;
  color: var(--text-primary, #1a1a2e);
  margin-bottom: 1.5rem;
}

.question-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.option-btn {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border: 2px solid var(--border, #e2e8f0);
  border-radius: 10px;
  background: var(--bg-primary, #fff);
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  font-size: 0.95rem;
  color: var(--text-primary, #1a1a2e);
}

.option-btn:hover {
  border-color: var(--primary, #3b82f6);
  background: #eff6ff;
}

.option-btn.selected {
  border-color: var(--primary, #3b82f6);
  background: #dbeafe;
}

.option-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  background: var(--bg-tertiary, #f1f5f9);
  font-weight: 600;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.option-btn.selected .option-key {
  background: var(--primary, #3b82f6);
  color: #fff;
}

.question-textarea {
  margin-top: 0.5rem;
}

.answer-textarea {
  width: 100%;
  padding: 1rem;
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  font-size: 0.95rem;
  font-family: inherit;
  resize: vertical;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1a1a2e);
}

.answer-textarea:focus {
  outline: none;
  border-color: var(--primary, #3b82f6);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.practice-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

/* Feedback */
.feedback-section {
  margin-top: 1.5rem;
}

.feedback-card {
  padding: 1rem 1.25rem;
  border-radius: 10px;
  border-left: 4px solid;
}

.feedback-correct {
  background: #f0fdf4;
  border-color: #22c55e;
}

.feedback-wrong {
  background: #fef2f2;
  border-color: #ef4444;
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.feedback-correct .feedback-header {
  color: #16a34a;
}

.feedback-wrong .feedback-header {
  color: #dc2626;
}

.feedback-text {
  font-weight: 600;
  font-size: 0.95rem;
}

.feedback-answer {
  font-size: 0.9rem;
  color: var(--text-secondary, #64748b);
  margin-bottom: 0.5rem;
}

.feedback-analysis {
  font-size: 0.875rem;
  color: var(--text-primary, #1a1a2e);
}

/* Report */
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.report-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  margin: 0;
}

.report-summary {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 2rem;
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 2rem;
}

.report-score {
  text-align: center;
}

.score-number {
  font-size: 3rem;
  font-weight: 800;
  color: var(--primary, #3b82f6);
}

.score-label {
  font-size: 0.875rem;
  color: var(--text-secondary, #64748b);
}

.report-stats {
  display: flex;
  gap: 2rem;
}

.report-stat {
  text-align: center;
}

.report-stat .stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
}

.report-stat.stat-correct .stat-value { color: #16a34a; }
.report-stat.stat-wrong .stat-value { color: #dc2626; }

.report-stat .stat-name {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.report-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.details-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  margin: 0;
}

.report-question {
  background: var(--bg-secondary, #fff);
  border-radius: 10px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.report-question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.report-question-num {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-primary, #1a1a2e);
}

.report-result-badge {
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
}

.report-question-stem {
  font-size: 0.95rem;
  line-height: 1.6;
  color: var(--text-primary, #1a1a2e);
  margin-bottom: 0.75rem;
}

.report-question-answers {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.report-answer-item {
  font-size: 0.875rem;
  color: var(--text-secondary, #64748b);
}

.report-answer-label {
  font-weight: 600;
  margin-right: 0.25rem;
}

.report-analysis {
  padding-top: 0.5rem;
  border-top: 1px solid var(--border, #e2e8f0);
}
</style>
