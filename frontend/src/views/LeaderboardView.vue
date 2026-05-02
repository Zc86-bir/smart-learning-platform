<script setup>
import { ref } from 'vue'
import { api } from '../composables/api'

const papers = ref([])
const selectedPaperId = ref('')
const leaderboard = ref([])
const loading = ref(false)

const loadPapers = async () => {
  try {
    const page = await api('/student/papers')
    papers.value = page.records || []
    if (papers.value.length) {
      selectedPaperId.value = papers.value[0].id
    }
  } catch (e) {
    papers.value = []
  }
}

const loadLeaderboard = async () => {
  if (!selectedPaperId.value) return
  loading.value = true
  try {
    const list = await api(`/student/exams/leaderboard/${selectedPaperId.value}?limit=20`)
    leaderboard.value = list || []
  } catch (e) {
    leaderboard.value = []
  } finally {
    loading.value = false
  }
}

loadPapers().then(() => loadLeaderboard())
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>排行榜</h2>
        <p>查看各试卷的成绩排名</p>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="flex items-center gap-4" style="width: 100%;">
          <label class="text-sm font-semibold" style="color: var(--slate-600); white-space: nowrap;">选择试卷：</label>
          <select class="form-select" v-model="selectedPaperId" @change="loadLeaderboard" style="max-width: 400px;">
            <option v-for="p in papers" :key="p.id" :value="p.id">{{ p.title }}</option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="card-body text-center" style="padding: 40px; color: var(--slate-400);">
        <div class="spinner" style="margin: 0 auto; width: 32px; height: 32px; border-width: 3px;"></div>
        <p class="mt-2 text-sm">加载中...</p>
      </div>

      <div v-else-if="!leaderboard.length" class="empty-state">
        <div class="empty-state-icon">📊</div>
        <h4>暂无排名记录</h4>
        <p>完成考试后将出现在排行榜中</p>
      </div>

      <div v-else class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 80px;">排名</th>
              <th>用户名</th>
              <th style="width: 100px;">得分</th>
              <th style="width: 100px;">用时</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(entry, i) in leaderboard" :key="i">
              <td>
                <div :class="['lb-rank', i < 3 ? 'r' + (i + 1) : 'rn']" style="display: inline-flex;">
                  <span v-if="i === 0">🥇</span>
                  <span v-else-if="i === 1">🥈</span>
                  <span v-else-if="i === 2">🥉</span>
                  <span v-else>{{ i + 1 }}</span>
                </div>
              </td>
              <td class="font-semibold">{{ entry.nickname || 'User-' + entry.userId }}</td>
              <td>
                <span class="badge badge-blue font-semibold">{{ entry.score }} 分</span>
              </td>
              <td class="text-muted">{{ entry.formattedDuration || '00:00' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
