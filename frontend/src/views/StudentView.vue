<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth, api } from '../composables/api'
import ExamView from './ExamView.vue'
import QuestionView from './QuestionView.vue'
import WrongBook from './WrongBook.vue'
import KnowledgeMastery from './KnowledgeMastery.vue'
import Dashboard from './Dashboard.vue'
import TutorView from './TutorView.vue'
import LeaderboardView from './LeaderboardView.vue'

const { nickname, userRole, logout } = useAuth()
const router = useRouter()
const activeTab = ref('dashboard')

const navItems = [
  { id: 'dashboard', label: '学习看板', icon: '📊', section: '学习' },
  { id: 'exam', label: '在线考试', icon: '📝', section: '学习' },
  { id: 'question', label: '题库练习', icon: '📚', section: '学习' },
  { id: 'wrongbook', label: '错题本', icon: '📕', section: '巩固' },
  { id: 'knowledge', label: '知识点掌握', icon: '🎯', section: '巩固' },
  { id: 'tutor', label: 'AI导师', icon: '🤖', section: '辅助' },
  { id: 'leaderboard', label: '排行榜', icon: '🏆', section: '辅助' }
]

const doLogout = () => {
  logout()
  router.push('/')
}
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
        <div class="sidebar-section-title">学习</div>
        <button
          v-for="item in navItems.filter(i => i.section === '学习')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
        </button>

        <div class="sidebar-section-title">巩固</div>
        <button
          v-for="item in navItems.filter(i => i.section === '巩固')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
        </button>

        <div class="sidebar-section-title">辅助</div>
        <button
          v-for="item in navItems.filter(i => i.section === '辅助')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="sidebar-avatar">{{ nickname?.charAt(0)?.toUpperCase() || 'S' }}</div>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name">{{ nickname }}</div>
            <div class="sidebar-user-role">学生</div>
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
          <h1 class="page-title">{{ navItems.find(i => i.id === activeTab)?.label || '学习中心' }}</h1>
        </div>
        <div class="app-header-right">
          <span class="header-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
            学习中心
          </span>
        </div>
      </header>

      <div class="page-content">
        <Dashboard v-if="activeTab === 'dashboard'" />
        <ExamView v-else-if="activeTab === 'exam'" />
        <QuestionView v-else-if="activeTab === 'question'" />
        <WrongBook v-else-if="activeTab === 'wrongbook'" />
        <KnowledgeMastery v-else-if="activeTab === 'knowledge'" />
        <TutorView v-else-if="activeTab === 'tutor'" />
        <LeaderboardView v-else />
      </div>
    </div>
  </div>
</template>
