<script setup>
import { ref, nextTick } from 'vue'
import { api } from '../composables/api'

const questions = ref([])
const selectedQId = ref('')
const selectedQStem = ref('')
const selectedQAnswer = ref('')
const inputMessage = ref('')
const messages = ref([])
const streaming = ref(false)
const loading = ref(false)

const loadQuestions = async () => {
  try {
    const page = await api('/student/questions?page=1&size=50')
    questions.value = page.records || []
  } catch (e) {
    questions.value = []
  }
}

const loadHistory = async (questionId) => {
  loading.value = true
  try {
    const history = await api(`/student/ai-tutor/history/${questionId}`)
    messages.value = Array.isArray(history) ? history : []
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
}

const onQuestionChange = () => {
  const q = questions.value.find(q => q.id == selectedQId.value)
  if (q) {
    selectedQStem.value = q.stem
    selectedQAnswer.value = q.answer || ''
    loadHistory(q.id)
  }
}

const send = async () => {
  const text = inputMessage.value.trim()
  if (!text) return
  if (!selectedQId.value) {
    alert('请先选择要提问的题目')
    return
  }

  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  streaming.value = true

  messages.value.push({ role: 'assistant', content: '' })
  await nextTick()

  try {
    const response = await fetch('/api/student/ai-tutor/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': localStorage.getItem('userId'),
        'X-User-Role': localStorage.getItem('userRole')
      },
      body: JSON.stringify({
        questionId: selectedQId.value,
        message: text,
        questionStem: selectedQStem.value,
        standardAnswer: selectedQAnswer.value
      })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullAnswer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            streaming.value = false
            break
          }
          try {
            const parsed = JSON.parse(data)
            const chunk = parsed.content || parsed.choices?.[0]?.delta?.content || ''
            if (chunk) {
              fullAnswer += chunk
              const lastMsg = messages.value[messages.value.length - 1]
              if (lastMsg.role === 'assistant') {
                lastMsg.content = fullAnswer
              }
              await nextTick()
            }
          } catch {}
        }
      }
    }

    if (!fullAnswer) {
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg.role === 'assistant' && !lastMsg.content) {
        lastMsg.content = '(回复结束)'
      }
    }

    streaming.value = false
  } catch (e) {
    streaming.value = false
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg.role === 'assistant') {
      lastMsg.content = '请求失败: ' + e.message
    }
  }
}

const clearHistory = async () => {
  if (selectedQId.value) {
    try {
      await fetch(`/api/student/ai-tutor/clear/${selectedQId.value}`, {
        method: 'DELETE',
        headers: {
          'X-User-Id': localStorage.getItem('userId'),
          'X-User-Role': localStorage.getItem('userRole')
        }
      })
      messages.value = []
    } catch (e) {
      messages.value = []
    }
  }
}

const askAboutCurrent = () => {
  if (!selectedQStem.value) return
  const truncated = selectedQStem.value.length > 200
    ? selectedQStem.value.substring(0, 200) + '...'
    : selectedQStem.value
  inputMessage.value = `请帮我分析这道题：${truncated}`
}

// Check for auto-question from localStorage (from "基于当前题目提问" button)
const checkAutoQuestion = () => {
  const autoQ = localStorage.getItem('tutorAutoQuestion')
  const autoA = localStorage.getItem('tutorAutoAnswer')
  const autoId = localStorage.getItem('tutorAutoQuestionId')
  if (autoQ && autoId) {
    selectedQId.value = autoId
    selectedQStem.value = autoQ
    selectedQAnswer.value = autoA || ''
    inputMessage.value = `请帮我分析这道题：${autoQ.length > 100 ? autoQ.substring(0, 100) + '...' : autoQ}`
    loadHistory(autoId)
    // Clean up
    localStorage.removeItem('tutorAutoQuestion')
    localStorage.removeItem('tutorAutoAnswer')
    localStorage.removeItem('tutorAutoQuestionId')
  }
}

const goBack = () => {
  window.history.back()
}

loadQuestions()
checkAutoQuestion()
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <button class="btn btn-ghost" @click="goBack" style="padding-left: 0;">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          返回
        </button>
        <h2>AI一对一答疑</h2>
        <p>苏格拉底式引导：AI会先反问，不直接给答案</p>
      </div>
      <button class="btn btn-outline" @click="clearHistory" v-if="messages.length">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
        </svg>
        清空记录
      </button>
    </div>

    <div class="card mb-4">
      <div class="card-body">
        <div class="form-group" style="margin-bottom: 0;">
          <label>选择题目</label>
          <select class="form-select" v-model="selectedQId" @change="onQuestionChange">
            <option value="">-- 选择要提问的题目 --</option>
            <option v-for="q in questions" :key="q.id" :value="q.id">{{ q.stem.substring(0, 50) }}...</option>
          </select>
        </div>
        <div v-if="selectedQId" class="mt-2">
          <button class="btn btn-sm btn-outline" @click="askAboutCurrent" :disabled="!selectedQStem">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            基于当前题目提问
          </button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-body">
        <div v-if="loading" class="text-center" style="padding: 40px 0; color: var(--slate-400);">
          <div class="spinner" style="margin: 0 auto 12px; width: 32px; height: 32px; border-width: 3px;"></div>
          <p>加载对话记录...</p>
        </div>
        <div class="tutor-messages" v-else>
          <div v-if="!messages.length" class="text-center" style="padding: 60px 0; color: var(--slate-400);">
            <div style="font-size: 48px; margin-bottom: 12px;"></div>
            <p>选择题目后，输入你的问题开始追问</p>
          </div>
          <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role === 'user' ? 'user' : 'assistant']">
            <div class="msg-bubble" :class="{ streaming: msg.role === 'assistant' && streaming && i === messages.length - 1 }">
              {{ msg.content }}
            </div>
          </div>
        </div>

        <div class="tutor-input-area">
          <input
            class="tutor-input"
            v-model="inputMessage"
            placeholder="输入你的问题..."
            @keyup.enter="send"
            :disabled="streaming"
          />
          <button class="btn btn-primary" @click="send" :disabled="streaming || !inputMessage.trim()">
            <svg v-if="!streaming" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
            {{ streaming ? '思考中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
