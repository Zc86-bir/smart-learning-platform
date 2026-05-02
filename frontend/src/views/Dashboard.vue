<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../composables/api'

const loading = ref(true)
const dashboard = ref(null)

const loadDashboard = async () => {
  loading.value = true
  try {
    dashboard.value = await api('/student/dashboard')
  } catch (e) {
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

const scoreBarWidth = (score, total) => {
  if (!total || total === 0) return 0
  return Math.round((score / total) * 100)
}

const masteryColor = (pct) => {
  if (pct >= 80) return 'var(--success)'
  if (pct >= 60) return 'var(--warning)'
  return 'var(--danger)'
}

onMounted(() => loadDashboard())
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>学习数据看板</h2>
        <p>查看你的学习进度和成绩趋势</p>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card text-center" style="padding: 60px;">
      <div class="spinner" style="margin: 0 auto; width: 32px; height: 32px; border-width: 3px;"></div>
      <p class="mt-2 text-sm text-muted">加载中...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="!dashboard || dashboard.totalExams === 0" class="empty-state">
      <div class="empty-state-icon">📊</div>
      <h4>暂无学习数据</h4>
      <p>完成考试后将在这里展示你的学习数据</p>
    </div>

    <!-- Dashboard Content -->
    <template v-else>
      <!-- Core Stats -->
      <div class="stats-grid mb-4">
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">考试次数</div>
              <div class="stat-card-value">{{ dashboard.totalExams }}</div>
            </div>
            <div class="stat-card-icon blue">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">平均分</div>
              <div class="stat-card-value">{{ dashboard.avgScore }}</div>
            </div>
            <div class="stat-card-icon green">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">平均正确率</div>
              <div class="stat-card-value">{{ dashboard.avgAccuracy }}%</div>
            </div>
            <div class="stat-card-icon cyan">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-card-header">
            <div>
              <div class="stat-card-label">待复习错题</div>
              <div class="stat-card-value">{{ dashboard.totalQuestions }}</div>
            </div>
            <div class="stat-card-icon red">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;" class="mb-4">
        <!-- Score Trend -->
        <div class="card">
          <div class="card-header">
            <h3>成绩趋势</h3>
          </div>
          <div class="card-body">
            <div v-if="dashboard.scoreTrend?.length" style="display: flex; align-items: flex-end; gap: 12px; height: 180px; padding: 12px 0;">
              <div v-for="(item, i) in dashboard.scoreTrend" :key="i" style="flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; height: 100%;">
                <span class="text-xs font-semibold" :style="{ color: scoreBarWidth(item.score, item.totalScore) >= 60 ? 'var(--success)' : 'var(--danger)' }">
                  {{ item.score }}/{{ item.totalScore }}
                </span>
                <div style="width: 100%; background: var(--slate-100); border-radius: var(--radius-sm); flex: 1; position: relative; overflow: hidden;">
                  <div :style="{
                    position: 'absolute', bottom: 0, left: 0, right: 0,
                    height: scoreBarWidth(item.score, item.totalScore) + '%',
                    background: scoreBarWidth(item.score, item.totalScore) >= 60 ? 'var(--primary)' : 'var(--danger)',
                    borderRadius: 'var(--radius-sm)',
                    transition: 'height 0.5s ease'
                  }"></div>
                </div>
                <span class="text-xs text-muted">{{ item.date }}</span>
              </div>
            </div>
            <div v-else class="text-center text-muted" style="padding: 40px 0;">暂无成绩记录</div>
          </div>
        </div>

        <!-- Wrong Question Distribution by Category -->
        <div class="card">
          <div class="card-header">
            <h3>错题分类分布</h3>
          </div>
          <div class="card-body">
            <div v-if="dashboard.wrongCategoryDist && Object.keys(dashboard.wrongCategoryDist).length">
              <div v-for="(count, cat) in dashboard.wrongCategoryDist" :key="cat" class="mb-2">
                <div class="flex justify-between mb-1">
                  <span class="text-sm">{{ cat }}</span>
                  <span class="text-sm font-semibold">{{ count }} 题</span>
                </div>
                <div style="height: 6px; background: var(--slate-100); border-radius: 3px; overflow: hidden;">
                  <div :style="{
                    height: '100%',
                    width: (count / dashboard.totalQuestions * 100) + '%',
                    background: 'var(--primary)',
                    borderRadius: '3px',
                    transition: 'width 0.3s ease'
                  }"></div>
                </div>
              </div>
            </div>
            <div v-else class="text-center text-muted" style="padding: 40px 0;">暂无错题</div>
          </div>
        </div>
      </div>

      <!-- Knowledge Point Mastery -->
      <div class="card">
        <div class="card-header">
          <h3>知识点掌握度</h3>
        </div>
        <div class="card-body" v-if="dashboard.knowledgeMastery && Object.keys(dashboard.knowledgeMastery).length">
          <div v-for="(pct, kp) in dashboard.knowledgeMastery" :key="kp" class="mb-2">
            <div class="flex justify-between mb-1">
              <span class="text-sm">{{ kp }}</span>
              <span class="text-sm font-semibold" :style="{ color: masteryColor(pct) }">{{ pct }}%</span>
            </div>
            <div style="height: 8px; background: var(--slate-100); border-radius: 4px; overflow: hidden;">
              <div :style="{
                height: '100%',
                width: pct + '%',
                background: masteryColor(pct),
                borderRadius: '4px',
                transition: 'width 0.3s ease'
              }"></div>
            </div>
          </div>
        </div>
        <div v-else class="card-body text-center text-muted" style="padding: 40px;">暂无知识点数据</div>
      </div>
    </template>
  </div>
</template>
