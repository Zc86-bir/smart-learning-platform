<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const props = defineProps({
  totalStudents: Number,
  totalExams: Number,
  completedExams: Number
})

const categoryStats = ref([])
const examTrend = ref([])
const topPapers = ref([])
const monthlyStats = ref([])

const loadAll = async () => {
  try {
    const stats = await api('/admin/dashboard/stats')
    categoryStats.value = stats.categoryStats || []
  } catch {}
  try {
    examTrend.value = await api('/admin/dashboard/trend') || []
  } catch {}
  try {
    topPapers.value = await api('/admin/dashboard/papers/top') || []
  } catch {}
  try {
    monthlyStats.value = await api('/admin/dashboard/monthly') || []
  } catch {}
}

const maxCatCount = () => {
  const counts = categoryStats.value.map(s => s.cnt || s.count || 0)
  return Math.max(...counts, 1)
}

onMounted(() => loadAll())
</script>

<template>
  <div class="teacher-dashboard">
    <!-- Stat Cards -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ props.totalStudents }}</div>
          <div class="stat-label">学生总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ props.totalExams }}</div>
          <div class="stat-label">考试总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon orange">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ props.completedExams }}</div>
          <div class="stat-label">已完成考试</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon purple">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ props.totalStudents > 0 ? Math.round(props.completedExams / props.totalStudents * 100) : 0 }}%</div>
          <div class="stat-label">参考率</div>
        </div>
      </div>
    </div>

    <!-- Category Chart -->
    <div class="dashboard-card">
      <div class="card-title">学科分布</div>
      <div class="bar-chart">
        <div v-for="s in categoryStats.slice(0, 8)" :key="s.category" class="bar-row">
          <span class="bar-label">{{ s.category }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: (s.cnt / maxCatCount() * 100) + '%' }"></div>
          </div>
          <span class="bar-value">{{ s.cnt }}</span>
        </div>
        <div v-if="!categoryStats.length" class="chart-empty">暂无数据</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <!-- Exam Trend -->
      <div class="dashboard-card">
        <div class="card-title">近30天考试趋势</div>
        <div class="trend-chart">
          <div v-for="(t, i) in examTrend.slice(-14)" :key="t.date" class="trend-col">
            <div class="trend-bar" :style="{ height: Math.max((t.cnt / Math.max(...examTrend.map(x => x.cnt || 0), 1)) * 100, 4) + 'px' }"></div>
            <div class="trend-date">{{ t.date?.substring(5) || '' }}</div>
          </div>
          <div v-if="!examTrend.length" class="chart-empty">暂无数据</div>
        </div>
      </div>

      <!-- Top Papers -->
      <div class="dashboard-card">
        <div class="card-title">热门试卷 TOP 5</div>
        <div class="top-papers-list">
          <div v-for="(p, i) in topPapers.slice(0, 5)" :key="i" class="top-paper-item">
            <span class="top-paper-rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
            <div class="top-paper-info">
              <div class="top-paper-title">{{ p.title }}</div>
              <div class="top-paper-meta">
                <span>{{ p.examCount || 0 }} 次考试</span>
                <span v-if="p.avgScoreRate">平均分 {{ Math.round(p.avgScoreRate) }}%</span>
              </div>
            </div>
          </div>
          <div v-if="!topPapers.length" class="chart-empty">暂无数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.teacher-dashboard {
  padding: 1.5rem;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-icon.blue { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.stat-icon.green { background: linear-gradient(135deg, #22c55e, #16a34a); }
.stat-icon.orange { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-icon.purple { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  line-height: 1;
}

.stat-label {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
  margin-top: 0.25rem;
}

.dashboard-card {
  background: var(--bg-secondary, #fff);
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 1rem;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.bar-label {
  width: 80px;
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
  text-align: right;
  flex-shrink: 0;
}

.bar-track {
  flex: 1;
  height: 20px;
  background: var(--bg-tertiary, #f1f5f9);
  border-radius: 10px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border-radius: 10px;
  transition: width 0.5s ease;
  min-width: 2px;
}

.bar-value {
  width: 32px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  text-align: right;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-top: 1rem;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 120px;
  padding-top: 0.5rem;
}

.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.trend-bar {
  width: 100%;
  min-width: 8px;
  background: linear-gradient(180deg, #3b82f6, #93c5fd);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.trend-date {
  font-size: 0.65rem;
  color: var(--text-secondary, #64748b);
  white-space: nowrap;
}

.top-papers-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.top-paper-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.top-paper-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.rank-1 { background: linear-gradient(135deg, #f59e0b, #d97706); }
.rank-2 { background: linear-gradient(135deg, #94a3b8, #64748b); }
.rank-3 { background: linear-gradient(135deg, #d97706, #b45309); }
.rank-4, .rank-5 { background: var(--bg-tertiary, #f1f5f9); color: var(--text-secondary, #64748b); }

.top-paper-info {
  flex: 1;
  min-width: 0;
}

.top-paper-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary, #1a1a2e);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.top-paper-meta {
  font-size: 0.75rem;
  color: var(--text-secondary, #64748b);
  display: flex;
  gap: 0.75rem;
}

.chart-empty {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted, #94a3b8);
  font-size: 0.875rem;
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
