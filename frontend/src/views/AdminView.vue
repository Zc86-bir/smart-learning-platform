<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth, api } from '../composables/api'
import GenQuestions from './GenQuestions.vue'
import AdminQuestionList from './AdminQuestionList.vue'
import SmartPaper from './SmartPaper.vue'
import QuestionImport from './QuestionImport.vue'
import AdminDashboard from './AdminDashboard.vue'
import StudentMgmt from './StudentMgmt.vue'

const { nickname, userRole, logout } = useAuth()
const router = useRouter()
const activeTab = ref('dashboard')
const questionCount = ref(0)
const paperCount = ref(0)

const navItems = [
  { id: 'dashboard', label: '数据看板', icon: '📊', section: '概览' },
  { id: 'gen', label: 'AI出题', icon: '📝', section: '出题' },
  { id: 'paper', label: '智能组卷', icon: '📋', section: '出题' },
  { id: 'import', label: '批量导入', icon: '📥', section: '管理' },
  { id: 'qlist', label: '题库管理', icon: '📚', section: '管理' },
  { id: 'students', label: '学生管理', icon: '👥', section: '管理' }
]

const doLogout = () => {
  logout()
  router.push('/')
}

const loadStats = async () => {
  try {
    const stats = await api('/admin/questions/stats')
    questionCount.value = Object.values(stats).reduce((s, v) => s + v, 0) || 0
  } catch {}
  try {
    const papers = await api('/admin/papers')
    paperCount.value = papers.total || 0
  } catch {}
}

onMounted(() => loadStats())
</script>

<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="sidebar-brand-icon">S</div>
        <span class="sidebar-brand-text">智能学习平台</span>
      </div>

      <nav class="sidebar-nav">
        <div class="sidebar-section-title">概览</div>
        <button
          v-for="item in navItems.filter(i => i.section === '概览')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
        </button>

        <div class="sidebar-section-title">出题</div>
        <button
          v-for="item in navItems.filter(i => i.section === '出题')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
        </button>

        <div class="sidebar-section-title">管理</div>
        <button
          v-for="item in navItems.filter(i => i.section === '管理')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
          <span v-if="item.id === 'qlist'" class="sidebar-item-badge">{{ questionCount }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="sidebar-avatar">{{ nickname?.charAt(0)?.toUpperCase() || 'A' }}</div>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name">{{ nickname }}</div>
            <div class="sidebar-user-role">{{ userRole === 'ADMIN' ? '管理员' : '学生' }}</div>
          </div>
          <button class="btn btn-ghost btn-icon btn-sm" @click="doLogout" title="退出登录">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="main-content">
      <header class="app-header">
        <div class="app-header-left">
          <h1 class="page-title">{{ navItems.find(i => i.id === activeTab)?.label || '管理后台' }}</h1>
        </div>
        <div class="app-header-right">
          <span class="header-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/><path d="M12 1v6m0 6v10m11-7h-6m-6 0H1m20.07-4.93l-4.24 4.24M6.17 17.83l-4.24-4.24m0-7.16l4.24 4.24m11.66 7.16l4.24-4.24"/>
            </svg>
            管理后台
          </span>
        </div>
      </header>

      <div class="page-content">
        <!-- Stats row (shown for all tabs except qlist and dashboard) -->
        <div class="stats-grid" v-if="activeTab !== 'qlist' && activeTab !== 'dashboard'">
          <div class="stat-card">
            <div class="stat-card-header">
              <div>
                <div class="stat-card-label">题库总量</div>
                <div class="stat-card-value">{{ questionCount }}</div>
              </div>
              <div class="stat-card-icon blue">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                </svg>
              </div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-card-header">
              <div>
                <div class="stat-card-label">试卷数量</div>
                <div class="stat-card-value">{{ paperCount }}</div>
              </div>
              <div class="stat-card-icon green">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
              </div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-card-header">
              <div>
                <div class="stat-card-label">可用模型</div>
                <div class="stat-card-value">3</div>
              </div>
              <div class="stat-card-icon cyan">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z"/><path d="M16 14H8a4 4 0 0 0-4 4v2h16v-2a4 4 0 0 0-4-4z"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <AdminDashboard v-if="activeTab === 'dashboard'" />
        <GenQuestions v-else-if="activeTab === 'gen'" />
        <SmartPaper v-else-if="activeTab === 'paper'" />
        <QuestionImport v-else-if="activeTab === 'import'" />
        <AdminQuestionList v-else-if="activeTab === 'qlist'" />
        <StudentMgmt v-else-if="activeTab === 'students'" />
      </div>
    </div>
  </div>
</template>
