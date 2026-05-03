import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/student',
    name: 'student',
    component: () => import('../views/StudentView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/student/tutor',
    name: 'tutor',
    component: () => import('../views/TutorView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'SUPER_ADMIN'] }
  },
  {
    path: '/tutor',
    name: 'tutor',
    component: () => import('../views/TutorView.vue'),
    meta: { requiresAuth: true, roles: ['TEACHER', 'ADMIN', 'SUPER_ADMIN'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !localStorage.getItem('userId')) {
    return { name: 'login' }
  }
  if (to.meta.role && localStorage.getItem('userRole') !== to.meta.role) {
    return { name: 'login' }
  }
  if (to.meta.roles && !to.meta.roles.includes(localStorage.getItem('userRole'))) {
    return { name: 'login' }
  }
})

export default router
