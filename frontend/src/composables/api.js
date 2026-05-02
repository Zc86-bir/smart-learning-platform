import { ref } from 'vue'

export function useAuth() {
  const userId = ref(localStorage.getItem('userId') || '')
  const userRole = ref(localStorage.getItem('userRole') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')

  const login = (uid, role, name) => {
    userId.value = uid
    userRole.value = role
    nickname.value = name
    localStorage.setItem('userId', uid)
    localStorage.setItem('userRole', role)
    localStorage.setItem('nickname', name)
  }

  const logout = () => {
    userId.value = ''
    userRole.value = ''
    nickname.value = ''
    localStorage.removeItem('userId')
    localStorage.removeItem('userRole')
    localStorage.removeItem('nickname')
  }

  return { userId, userRole, nickname, login, logout }
}

export function headers() {
  return {
    'Content-Type': 'application/json',
    'X-User-Id': localStorage.getItem('userId'),
    'X-User-Role': localStorage.getItem('userRole')
  }
}

export async function api(path, opts = {}) {
  const isFormData = opts.body instanceof FormData
  const baseHeaders = isFormData ? {
    'X-User-Id': localStorage.getItem('userId'),
    'X-User-Role': localStorage.getItem('userRole')
  } : headers()
  opts.headers = { ...baseHeaders, ...(opts.headers || {}) }
  const res = await fetch('/api' + path, opts)
  const json = await res.json()
  if (json.code !== 200) throw new Error(json.message || '请求失败')
  return json.data
}

export const questionTypeLabel = (type) => {
  const map = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    SHORT_ANSWER: '简答题'
  }
  return map[type] || type
}
