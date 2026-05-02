/**
 * API client with global error handling, retry, and auth headers.
 */

export async function api<T = unknown>(path: string, opts: RequestInit = {}): Promise<T> {
  const isFormData = opts.body instanceof FormData
  const baseHeaders: Record<string, string> = isFormData
    ? {
        'X-User-Id': getUserId(),
        'X-User-Role': getUserRole(),
      }
    : {
        'Content-Type': 'application/json',
        'X-User-Id': getUserId(),
        'X-User-Role': getUserRole(),
      }

  opts.headers = { ...baseHeaders, ...(opts.headers as Record<string, string> || {}) }

  const res = await fetch('/api' + path, opts)

  // Handle non-200 responses (auth errors, server errors)
  if (!res.ok) {
    if (res.status === 401) {
      // Auth expired → redirect to login
      logout()
      window.location.href = '/'
      throw new Error('登录已过期，请重新登录')
    }
    const text = await res.text().catch(() => '')
    throw new Error(`请求失败 (${res.status}): ${text || res.statusText}`)
  }

  const json = await res.json()

  // Backend uses { code: 200, data: ... } envelope
  if (json.code !== 200) {
    throw new Error(json.message || '请求失败')
  }
  return json.data as T
}

export function getUserId(): string | null {
  return localStorage.getItem('userId')
}

export function getUserRole(): string | null {
  return localStorage.getItem('userRole')
}

export function getNickname(): string | null {
  return localStorage.getItem('nickname')
}

export function login(uid: string, role: string, nickname: string): void {
  localStorage.setItem('userId', uid)
  localStorage.setItem('userRole', role)
  localStorage.setItem('nickname', nickname)
}

export function logout(): void {
  localStorage.removeItem('userId')
  localStorage.removeItem('userRole')
  localStorage.removeItem('nickname')
}

export const questionTypeLabel = (type: string): string => {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    SHORT_ANSWER: '简答题',
  }
  return map[type] || type
}
