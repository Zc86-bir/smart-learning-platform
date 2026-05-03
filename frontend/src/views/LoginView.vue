<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/api'

const router = useRouter()
const { login } = useAuth()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const doLogin = async () => {
  if (!username.value.trim()) {
    error.value = '请输入用户名'
    return
  }
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  error.value = ''
  loading.value = true
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value.trim(),
        password: password.value
      })
    })
    const json = await res.json()
    if (json.code !== 200) throw new Error(json.message || '登录失败')
    const user = json.data
    login(user.id.toString(), user.role, user.nickname)
    if (user.role === 'ADMIN' || user.role === 'SUPER_ADMIN') {
      router.push('/admin')
    } else if (user.role === 'TEACHER') {
      router.push('/tutor')
    } else {
      router.push('/student')
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-screen">
    <div class="login-card">
      <div class="logo">
        <div class="logo-icon">S</div>
        <div class="logo-text">智能学习平台</div>
      </div>
      <p class="subtitle">欢迎回来，请登录您的账号</p>
      <div class="form-group">
        <label>用户名</label>
        <input class="form-input" v-model="username" placeholder="请输入用户名" @keyup.enter="doLogin" autofocus />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input class="form-input" type="password" v-model="password" placeholder="请输入密码" @keyup.enter="doLogin" />
      </div>
      <p v-if="error" class="text-sm" style="color: var(--danger); margin-bottom: 12px; text-align: center;">{{ error }}</p>
      <button class="btn btn-primary btn-block btn-lg" @click="doLogin" :disabled="loading">
        {{ loading ? '登录中...' : '登 录' }}
      </button>
      <div class="mt-6 text-sm text-center" style="color: var(--slate-400);">
        测试账号：admin / student1 / student2，密码：test1234
      </div>
    </div>
  </div>
</template>
