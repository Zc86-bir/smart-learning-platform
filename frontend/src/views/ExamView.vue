<script setup>
import { ref, onUnmounted } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const papers = ref([])
const examRecordId = ref(null)
const questions = ref([])
const showExam = ref(false)
const showResult = ref(false)
const examTimer = ref('00:00')
const elapsed = ref(0)
const cutScreenCount = ref(0)
const clipboardCount = ref(0)
const paperTitle = ref('')
const report = ref(null)
const warning = ref('')
const heartbeatTimer = ref(null)
const examTimerInterval = ref(null)

const onVisibilityChange = () => {
  if (document.visibilityState === 'hidden' && examRecordId.value) {
    cutScreenCount.value++
    warning.value = `已切屏 ${cutScreenCount.value} 次！连续切屏5次将强制交卷！`
    if (cutScreenCount.value >= 5) {
      submitExam(true)
    }
  }
}

const onClipboardCopy = () => {
  if (examRecordId.value) {
    clipboardCount.value++
  }
}

const loadPapers = async () => {
  try {
    const page = await api('/student/papers')
    papers.value = page.records || []
  } catch (e) {
    papers.value = []
  }
}

const startExam = async (paper) => {
  try {
    const record = await api(`/student/exams/start/${paper.id}`, { method: 'POST' })
    examRecordId.value = record.id
    cutScreenCount.value = 0
    clipboardCount.value = 0
    warning.value = ''
    elapsed.value = 0
    showResult.value = false

    document.addEventListener('visibilitychange', onVisibilityChange)
    document.addEventListener('copy', onClipboardCopy)

    heartbeatTimer.value = setInterval(() => {
      fetch(`/api/student/exams/heartbeat/${examRecordId.value}`, {
        method: 'POST',
        headers: {
          'X-User-Id': localStorage.getItem('userId'),
          'X-User-Role': localStorage.getItem('userRole')
        }
      })
    }, 30000)

    examTimerInterval.value = setInterval(() => {
      elapsed.value++
      const mm = String(Math.floor(elapsed.value / 60)).padStart(2, '0')
      const ss = String(elapsed.value % 60).padStart(2, '0')
      examTimer.value = `${mm}:${ss}`
    }, 1000)

    const paperDetail = await api(`/student/papers/${paper.id}`)
    const qIds = JSON.parse(paperDetail.questionIds)
    const results = await Promise.all(qIds.map(id => api(`/student/questions/${id}`).catch(() => null)))
    questions.value = results.filter(Boolean)
    paperTitle.value = paperDetail.title
    showExam.value = true
  } catch (e) {
    alert('开始考试失败: ' + e.message)
  }
}

const submitExam = async (force = false) => {
  if (!force && !confirm('确定提交答卷吗？')) return
  if (heartbeatTimer.value) clearInterval(heartbeatTimer.value)
  if (examTimerInterval.value) clearInterval(examTimerInterval.value)

  const answers = {}
  questions.value.forEach(q => {
    if (q.type === 'SINGLE_CHOICE') {
      const sel = document.querySelector(`input[name="q${q.id}"]:checked`)
      if (sel) answers[q.id] = sel.value
    } else if (q.type === 'MULTIPLE_CHOICE') {
      const checked = document.querySelectorAll(`input[name="q${q.id}"]:checked`)
      if (checked.length) answers[q.id] = Array.from(checked).map(el => el.value).join(',')
    } else if (q.type === 'TRUE_FALSE') {
      const sel = document.querySelector(`input[name="q${q.id}"]:checked`)
      if (sel) answers[q.id] = sel.value === 'true' ? 'true' : 'false'
    } else {
      const ta = document.querySelector(`textarea[name="q${q.id}"]`)
      if (ta) answers[q.id] = ta.value
    }
  })

  const duration = elapsed.value
  const idempotencyKey = `exam-${examRecordId.value}-${Date.now()}`
  try {
    await api('/student/exams/submit', {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey },
      body: JSON.stringify({
        examRecordId: examRecordId.value,
        answers,
        cutScreenCount: cutScreenCount.value,
        clipboardCount: clipboardCount.value
      })
    })

    document.removeEventListener('visibilitychange', onVisibilityChange)
    document.removeEventListener('copy', onClipboardCopy)

    const grading = await api(`/student/exams/${examRecordId.value}/report`)
    report.value = grading
    showExam.value = false
    showResult.value = true
  } catch (e) {
    alert('提交失败: ' + e.message)
  }
}

const backToPapers = () => {
  showExam.value = false
  showResult.value = false
  examRecordId.value = null
  report.value = null
  loadPapers()
}

const formatDuration = (seconds) => {
  const mm = String(Math.floor(seconds / 60)).padStart(2, '0')
  const ss = String(seconds % 60).padStart(2, '0')
  return `${mm}:${ss}`
}

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('copy', onClipboardCopy)
  if (heartbeatTimer.value) clearInterval(heartbeatTimer.value)
  if (examTimerInterval.value) clearInterval(examTimerInterval.value)
})

loadPapers()
</script>

<template>
  <div>
    <!-- Paper List -->
    <div v-if="!showExam && !showResult">
      <div class="page-header mb-4">
        <div>
          <h2>可考试卷</h2>
          <p>选择试卷开始考试，请遵守考试纪律</p>
        </div>
      </div>

      <div v-if="!papers.length" class="empty-state">
        <div class="empty-state-icon">📋</div>
        <h4>暂无可考试卷</h4>
        <p>请等待管理员发布新试卷</p>
      </div>

      <div v-else class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>试卷名称</th>
              <th>满分</th>
              <th>时长</th>
              <th>描述</th>
              <th style="width: 100px;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="paper in papers" :key="paper.id">
              <td>
                <span class="font-semibold">{{ paper.title }}</span>
              </td>
              <td><span class="badge badge-blue">{{ paper.total_score }} 分</span></td>
              <td>{{ paper.duration_minutes }} 分钟</td>
              <td class="text-muted">{{ paper.description || '暂无描述' }}</td>
              <td>
                <button class="btn btn-primary btn-sm" @click="startExam(paper)">开始考试</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Exam In Progress -->
    <div v-if="showExam">
      <div class="exam-header">
        <div class="flex items-center gap-4">
          <span class="font-semibold">{{ paperTitle }}</span>
          <span class="badge badge-blue">共 {{ questions.length }} 题</span>
        </div>
        <div class="flex items-center gap-4">
          <span v-if="warning" style="color: var(--danger); font-size: 13px; font-weight: 600;">{{ warning }}</span>
          <div class="exam-timer">{{ examTimer }}</div>
        </div>
      </div>
      <div class="page-content">
        <div v-for="(q, i) in questions" :key="q.id" class="card">
          <div class="card-header">
            <div class="flex items-center gap-2">
              <span class="text-sm text-muted">第 {{ i + 1 }} 题</span>
              <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ q.difficulty }}</span>
              <span class="badge badge-gray">{{ questionTypeLabel(q.type) }}</span>
            </div>
          </div>
          <div class="card-body">
            <div class="q-stem">{{ q.stem }}</div>

            <!-- Single Choice -->
            <div v-if="q.type === 'SINGLE_CHOICE'">
              <label v-for="[k, v] in Object.entries(q.options || {})" :key="k" class="q-option">
                <input type="radio" :name="`q${q.id}`" :value="k">
                <span class="q-option-key">{{ k }}</span>
                <span>{{ v }}</span>
              </label>
            </div>

            <!-- Multiple Choice -->
            <div v-else-if="q.type === 'MULTIPLE_CHOICE'">
              <label v-for="[k, v] in Object.entries(q.options || {})" :key="k" class="q-option">
                <input type="checkbox" :name="`q${q.id}`" :value="k">
                <span class="q-option-key">{{ k }}</span>
                <span>{{ v }}</span>
              </label>
            </div>

            <!-- True/False -->
            <div v-else-if="q.type === 'TRUE_FALSE'">
              <label class="q-option">
                <input type="radio" :name="`q${q.id}`" value="true">
                <span class="q-option-key">✓</span>
                <span>正确</span>
              </label>
              <label class="q-option">
                <input type="radio" :name="`q${q.id}`" value="false">
                <span class="q-option-key">✗</span>
                <span>错误</span>
              </label>
            </div>

            <!-- Short Answer -->
            <div v-else>
              <textarea :name="`q${q.id}`" rows="4" placeholder="请输入答案..." class="form-input"></textarea>
            </div>
          </div>
        </div>

        <div class="flex justify-center mt-6">
          <button class="btn btn-primary btn-lg" @click="submitExam()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
            提交答卷
          </button>
        </div>
      </div>
    </div>

    <!-- Result -->
    <div v-if="showResult">
      <div class="page-header mb-4">
        <div>
          <h2>考试结果</h2>
          <p>查看你的成绩和答题详情</p>
        </div>
        <button class="btn btn-outline" @click="backToPapers">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          返回试卷列表
        </button>
      </div>

      <div class="card text-center">
        <div class="card-body">
          <div class="result-display">
            <div class="result-score">{{ report?.totalScore || 0 }}</div>
            <p class="result-meta">满分 {{ report?.totalScore || 0 }} 分 · 用时 {{ formatDuration(elapsed) }} · 切屏 {{ cutScreenCount }} 次</p>
          </div>

          <div v-if="report?.details?.length" class="mt-6 text-left">
            <div class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>题号</th>
                    <th>结果</th>
                    <th>得分</th>
                    <th>你的答案</th>
                    <th>标准答案</th>
                    <th>AI点评</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, i) in report.details" :key="i">
                    <td class="font-semibold">第 {{ i + 1 }} 题</td>
                    <td>
                      <span :class="['badge', item.correct ? 'badge-green' : 'badge-red']">
                        {{ item.correct ? '✓ 正确' : '✗ 错误' }}
                      </span>
                    </td>
                    <td class="font-semibold">{{ item.score }}/{{ item.maxScore }}</td>
                    <td>{{ item.studentAnswer || '未作答' }}</td>
                    <td>{{ item.standardAnswer }}</td>
                    <td v-if="item.aiComment" style="color: var(--primary);">{{ item.aiComment }}</td>
                    <td v-else class="text-muted">-</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
