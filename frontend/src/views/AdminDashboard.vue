<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const stats = ref({})
const trend = ref([])
const topPapers = ref([])
const monthlyStats = ref([])
const loading = ref(true)

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const loadAll = async () => {
  loading.value = true
  try {
    const [s, t, p, m] = await Promise.all([
      api('/admin/dashboard/stats'),
      api('/admin/dashboard/trend'),
      api('/admin/dashboard/papers/top'),
      api('/admin/dashboard/monthly')
    ])
    stats.value = s
    trend.value = t || []
    topPapers.value = p || []
    monthlyStats.value = m || []
  } catch (e) {
    console.error('Load dashboard failed:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>数据看板</h2>
        <p>平台运营数据总览</p>
      </div>
    </div>

    <div v-if="loading" class="card text-center" style="padding: 60px;">
      <div class="spinner" style="margin: 0 auto 12px; width: 32px; height: 32px; border-width: 3px;"></div>
      <p class="text-muted">加载数据中...</p>
    </div>

    <template v-else>
      <!-- Overview Cards -->
      <div class="stats-grid mb-4">
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">学生总数</div>
              <div class="stat-card-value">{{ stats.totalStudents || 0 }}</div>
            </div>
            <div class="stat-card-icon blue">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">题库总量</div>
              <div class="stat-card-value">{{ stats.totalQuestions || 0 }}</div>
            </div>
            <div class="stat-card-icon green">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">考试总次数</div>
              <div class="stat-card-value">{{ stats.totalExams || 0 }}</div>
            </div>
            <div class="stat-card-icon yellow">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">已完成考试</div>
              <div class="stat-card-value">{{ stats.completedExams || 0 }}</div>
            </div>
            <div class="stat-card-icon purple">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <div class="grid-2col" style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
        <!-- Exam Trend -->
        <div class="card">
          <div class="card-header">
            <h3>近30天考试趋势</h3>
          </div>
          <div class="card-body">
            <div v-if="trend.length" style="display: flex; align-items: flex-end; gap: 4px; height: 180px; padding-top: 10px;">
              <div v-for="(item, i) in trend" :key="i" style="flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;">
                <span style="font-size: 11px; color: var(--slate-500); font-weight: 600;">{{ item.cnt }}</span>
                <div style="width: 100%; background: var(--primary); border-radius: 4px 4px 0 0; min-height: 2px; transition: height 0.3s;"
                     :style="{ height: Math.max(2, (item.cnt / Math.max(...trend.map(t => t.cnt))) * 140) + 'px' }"></div>
                <span style="font-size: 10px; color: var(--slate-400);">{{ formatDate(item.date) }}</span>
              </div>
            </div>
            <div v-else class="empty-state" style="padding: 40px 0;">
              <p class="text-muted">暂无数据</p>
            </div>
          </div>
        </div>

        <!-- Monthly Stats -->
        <div class="card">
          <div class="card-header">
            <h3>月度考试统计</h3>
          </div>
          <div class="card-body">
            <div v-if="monthlyStats.length">
              <div v-for="(item, i) in monthlyStats" :key="i" class="flex items-center gap-3 mb-3">
                <span style="font-size: 13px; color: var(--slate-600); width: 80px; flex-shrink: 0;">{{ item.month }}</span>
                <div style="flex: 1; display: flex; align-items: center; gap: 8px;">
                  <div style="flex: 1; background: var(--slate-100); border-radius: 4px; height: 8px; overflow: hidden;">
                    <div style="height: 100%; background: var(--primary); border-radius: 4px; transition: width 0.5s;"
                         :style="{ width: (item.total / Math.max(...monthlyStats.map(m => m.total))) * 100 + '%' }"></div>
                  </div>
                  <span style="font-size: 12px; color: var(--slate-500); width: 40px;">{{ item.total }} 次</span>
                </div>
                <span style="font-size: 12px; color: var(--success); width: 70px; text-align: right;">
                  均分 {{ (item.avgRate || 0).toFixed(1) }}%
                </span>
              </div>
            </div>
            <div v-else class="empty-state" style="padding: 40px 0;">
              <p class="text-muted">暂无数据</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Top Papers -->
      <div class="card mt-4">
        <div class="card-header">
          <h3>热门试卷排行</h3>
        </div>
        <div class="card-body" style="padding: 0;">
          <div v-if="topPapers.length">
            <table class="data-table" style="margin: 0;">
              <thead>
                <tr>
                  <th style="width: 60px;">排名</th>
                  <th>试卷</th>
                  <th style="width: 80px;">考试次数</th>
                  <th style="width: 100px;">平均分</th>
                  <th style="width: 100px;">通过率</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, i) in topPapers" :key="i">
                  <td>
                    <span :class="['badge', i === 0 ? 'badge-yellow' : i === 1 ? 'badge-gray' : i === 2 ? 'badge-cyan' : 'badge-gray']">
                      {{ i + 1 }}
                    </span>
                  </td>
                  <td style="font-weight: 500;">{{ p.title }}</td>
                  <td>{{ p.examCount }}</td>
                  <td>{{ (p.avgScoreRate || 0).toFixed(1) }}%</td>
                  <td>
                    <span :class="['badge', (p.passCount / p.totalCount || 0) >= 0.6 ? 'badge-green' : 'badge-red']">
                      {{ ((p.passCount / p.totalCount || 0) * 100).toFixed(0) }}%
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state" style="padding: 40px 0;">
            <p class="text-muted">暂无数据</p>
          </div>
        </div>
      </div>

      <!-- Category Distribution -->
      <div class="card mt-4">
        <div class="card-header">
          <h3>各分类题目数量</h3>
        </div>
        <div class="card-body">
          <div v-if="stats.categoryStats && stats.categoryStats.length">
            <div v-for="cat in stats.categoryStats" :key="cat.category" class="flex items-center gap-3 mb-3">
              <span style="font-size: 13px; color: var(--slate-600); width: 120px; flex-shrink: 0;">{{ cat.category }}</span>
              <div style="flex: 1; display: flex; align-items: center; gap: 8px;">
                <div style="flex: 1; background: var(--slate-100); border-radius: 4px; height: 8px; overflow: hidden;">
                  <div style="height: 100%; background: var(--primary); border-radius: 4px; transition: width 0.5s;"
                       :style="{ width: (cat.cnt / Math.max(...stats.categoryStats.map(c => c.cnt))) * 100 + '%' }"></div>
                </div>
                <span style="font-size: 13px; font-weight: 600; color: var(--slate-700); width: 50px; text-align: right;">{{ cat.cnt }}</span>
              </div>
            </div>
          </div>
          <div v-else class="text-center text-muted" style="padding: 20px;">暂无分类数据</div>
        </div>
      </div>
    </template>
  </div>
</template>
