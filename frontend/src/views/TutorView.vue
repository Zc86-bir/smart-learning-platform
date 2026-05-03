<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth, api } from '../composables/api'
import TutorChat from './TutorChat.vue'
import TeacherDashboard from './TeacherDashboard.vue'
import ClassManagement from './ClassManagement.vue'
import PaperManagement from './PaperManagement.vue'
import VideoManagement from './VideoManagement.vue'

const { nickname, userRole, logout } = useAuth()
const router = useRouter()
const activeTab = ref('dashboard')

// Stats
const totalStudents = ref(0)
const totalExams = ref(0)
const completedExams = ref(0)
const pendingVideos = ref(0)
const myPapers = ref(0)

const navItems = [
  { id: 'dashboard', label: '工作台', icon: '📊', section: '概览' },
  { id: 'class', label: '班级管理', icon: '👥', section: '教学' },
  { id: 'papers', label: '试卷管理', icon: '📋', section: '教学' },
  { id: 'videos', label: '视频管理', icon: '🎬', section: '资源' },
  { id: 'chat', label: 'AI答疑', icon: '🤖', section: '辅助' }
]

const doLogout = () => {
  logout()
  router.push('/')
}

const loadStats = async () => {
  try {
    const stats = await api('/admin/dashboard/stats')
    totalStudents.value = stats.totalStudents || 0
    totalExams.value = stats.totalExams || 0
    completedExams.value = stats.completedExams || 0
  } catch {}
  try {
    const videos = await api('/admin/videos?status=PENDING&page=1&size=1')
    pendingVideos.value = videos.total || 0
  } catch {}
  try {
    const papers = await api('/admin/papers?page=1&size=1')
    myPapers.value = papers.total || 0
  } catch {}
}

onMounted(() => loadStats())
</script>

<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="sidebar-brand-icon teacher">T</div>
        <span class="sidebar-brand-text">教师工作台</span>
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
          <span v-if="item.id === 'videos' && pendingVideos > 0" class="sidebar-badge">{{ pendingVideos }}</span>
        </button>

        <div class="sidebar-section-title">教学</div>
        <button
          v-for="item in navItems.filter(i => i.section === '教学')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
        </button>

        <div class="sidebar-section-title">资源</div>
        <button
          v-for="item in navItems.filter(i => i.section === '资源')"
          :key="item.id"
          :class="['sidebar-item', { active: activeTab === item.id }]"
          @click="activeTab = item.id"
        >
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span class="sidebar-item-label">{{ item.label }}</span>
          <span v-if="item.id === 'videos' && pendingVideos > 0" class="sidebar-badge">{{ pendingVideos }}</span>
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
          <div class="sidebar-avatar teacher">{{ nickname?.charAt(0)?.toUpperCase() || 'T' }}</div>
          <div class="sidebar-user-info">
            <div class="sidebar-user-name">{{ nickname }}</div>
            <div class="sidebar-user-role">教师</div>
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
          <h1 class="page-title">{{ navItems.find(i => i.id === activeTab)?.label || '教师工作台' }}</h1>
        </div>
        <div class="app-header-right">
          <span class="header-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
            </svg>
            教师端
          </span>
        </div>
      </header>

      <div class="page-content">
        <TeacherDashboard v-if="activeTab === 'dashboard'" :total-students="totalStudents" :total-exams="totalExams" :completed-exams="completedExams" />
        <ClassManagement v-else-if="activeTab === 'class'" />
        <PaperManagement v-else-if="activeTab === 'papers'" />
        <VideoManagement v-else-if="activeTab === 'videos'" @refresh="loadStats" />
        <TutorChat v-else />
      </div>
    </div>
  </div>
</template>
