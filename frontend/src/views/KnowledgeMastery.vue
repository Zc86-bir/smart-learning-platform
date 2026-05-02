<script setup>
import { ref, computed, onMounted } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const wrongQuestions = ref([])
const loading = ref(false)
const groupBy = ref('knowledgePoint')

// Group wrong questions by knowledge point
const grouped = computed(() => {
  const map = {}
  for (const q of wrongQuestions.value) {
    const key = groupBy.value === 'knowledgePoint'
      ? (q.knowledgePoint || '未分类')
      : (q.category || '未知')
    if (!map[key]) map[key] = []
    map[key].push(q)
  }
  return Object.entries(map).sort((a, b) => b[1].length - a[1].length)
})

const loadWrongQuestions = async () => {
  loading.value = true
  try {
    const result = await api('/student/wrong-questions?page=1&size=100')
    wrongQuestions.value = result.records || []
  } catch (e) {
    wrongQuestions.value = []
  } finally {
    loading.value = false
  }
}

const masteryPercent = (q) => {
  if (!q.wrongCount) return 100
  return Math.max(0, Math.round((1 - q.wrongCount / 5) * 100))
}

onMounted(() => loadWrongQuestions())
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>知识点掌握</h2>
        <p>按知识点查看掌握情况，针对性复习薄弱环节</p>
      </div>
    </div>

    <div v-if="loading" class="card text-center" style="padding: 40px;">
      <div class="spinner" style="margin: 0 auto; width: 32px; height: 32px; border-width: 3px;"></div>
      <p class="mt-2 text-sm text-muted">加载中...</p>
    </div>

    <div v-else-if="!wrongQuestions.length" class="empty-state">
      <div class="empty-state-icon">🎯</div>
      <h4>暂无数据</h4>
      <p>完成考试后会自动统计知识点掌握情况</p>
    </div>

    <template v-else>
      <!-- Group toggle -->
      <div class="card mb-4">
        <div class="card-body">
          <div class="flex items-center gap-2">
            <label class="text-sm font-semibold text-muted">分组方式：</label>
            <button :class="['btn btn-sm', groupBy === 'knowledgePoint' ? 'btn-primary' : 'btn-outline']" @click="groupBy = 'knowledgePoint'">按知识点</button>
            <button :class="['btn btn-sm', groupBy === 'category' ? 'btn-primary' : 'btn-outline']" @click="groupBy = 'category'">按学科</button>
          </div>
        </div>
      </div>

      <!-- Grouped View -->
      <div v-for="[groupName, questions] in grouped" :key="groupName" class="card mb-4">
        <div class="card-header">
          <h3>{{ groupName }}</h3>
          <span class="badge badge-red">{{ questions.length }} 道错题</span>
        </div>
        <div class="card-body">
          <div v-for="q in questions" :key="q.id" class="mb-2" style="padding: 8px 0; border-bottom: 1px solid var(--slate-100);">
            <div class="flex justify-between items-center mb-1">
              <div class="flex items-center gap-2">
                <span class="text-sm font-semibold" :style="{ maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }">{{ q.stem }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']">{{ q.difficulty }}</span>
                <span class="text-xs text-muted" v-if="q.wrongCount > 1">错 {{ q.wrongCount }} 次</span>
                <span :class="['badge', q.mastered ? 'badge-green' : 'badge-red']">{{ q.mastered ? '已掌握' : '未掌握' }}</span>
              </div>
            </div>
            <div style="height: 4px; background: var(--slate-100); border-radius: 2px; overflow: hidden;">
              <div :style="{
                height: '100%',
                width: masteryPercent(q) + '%',
                background: masteryPercent(q) >= 60 ? 'var(--success)' : 'var(--danger)',
                borderRadius: '2px'
              }"></div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
