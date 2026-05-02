<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { api, questionTypeLabel } from '../composables/api'

const files = ref([])
const filePreviews = ref([])
const parsing = ref(false)
const saving = ref(false)
const parsed = ref([])
const selectedIndices = ref(new Set())
const expandedIndices = ref(new Set())
const error = ref('')
const success = ref('')

const ACCEPTED_DOC = '.xlsx,.xls,.docx,.pdf,.txt'
const ACCEPTED_IMG = '.png,.jpg,.jpeg,.webp'

const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const readImageAsDataUrl = (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => resolve(null)
    reader.readAsDataURL(file)
  })
}

const addFiles = async (newFiles) => {
  if (!newFiles?.length) return
  files.value = [...files.value, ...newFiles]
  error.value = ''
  parsed.value = []
  selectedIndices.value = new Set()
  expandedIndices.value = new Set()
  success.value = ''

  for (const f of newFiles) {
    if (/\.(png|jpg|jpeg|webp)$/i.test(f.name)) {
      const url = await readImageAsDataUrl(f)
      if (url) {
        filePreviews.value.push({ name: f.name, size: f.size, url })
      }
    }
  }
}

const onFileChange = async (e) => {
  const newFiles = Array.from(e.target.files || [])
  if (!newFiles.length) return

  files.value = newFiles
  filePreviews.value = []
  error.value = ''
  parsed.value = []
  selectedIndices.value = new Set()
  expandedIndices.value = new Set()
  success.value = ''

  for (const f of newFiles) {
    if (/\.(png|jpg|jpeg|webp)$/i.test(f.name)) {
      const url = await readImageAsDataUrl(f)
      if (url) {
        filePreviews.value.push({ name: f.name, size: f.size, url })
      }
    }
  }
}

const doParse = async () => {
  if (!files.value.length) return
  parsing.value = true
  error.value = ''
  parsed.value = []
  selectedIndices.value = new Set()
  expandedIndices.value = new Set()
  try {
    const fd = new FormData()
    for (const f of files.value) {
      fd.append('files', f)
    }
    parsed.value = await api('/admin/questions/import/parse', {
      method: 'POST',
      body: fd,
    })
    if (!parsed.value?.length) {
      error.value = '文件中未识别到题目，请检查格式'
    } else {
      // Auto-select all questions
      selectedIndices.value = new Set(parsed.value.map((_, i) => i))
    }
  } catch (e) {
    error.value = e.message || '解析失败'
  } finally {
    parsing.value = false
  }
}

const selectedQuestions = computed(() => {
  return parsed.value.filter((_, i) => selectedIndices.value.has(i))
})

const doSave = async () => {
  const questions = selectedQuestions.value
  if (!questions.length) {
    error.value = '请至少选择一道题目'
    return
  }
  saving.value = true
  success.value = ''
  error.value = ''
  try {
    const result = await api('/admin/questions/import/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(questions),
    })
    success.value = `成功导入 ${result.saved} 道题目（共 ${result.total} 道）`
    parsed.value = []
    selectedIndices.value = new Set()
    expandedIndices.value = new Set()
    files.value = []
    filePreviews.value = []
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

const clearFiles = () => {
  files.value = []
  filePreviews.value = []
}

const downloadTemplate = async () => {
  try {
    const res = await fetch('/api/admin/questions/template', {
      headers: { 'X-User-Id': localStorage.getItem('userId'), 'X-User-Role': localStorage.getItem('userRole') }
    })
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'question_import_template.xlsx'
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = '下载模板失败: ' + e.message
  }
}

let imageCounter = 0

const addImageFile = async (blob) => {
  const ext = blob.type === 'image/jpeg' ? 'jpg' : blob.type === 'image/webp' ? 'webp' : 'png'
  const name = `clipboard_${Date.now()}_${++imageCounter}.${ext}`
  const file = new File([blob], name, { type: blob.type })

  files.value = [...files.value, file]
  const url = await readImageAsDataUrl(blob)
  if (url) {
    filePreviews.value = [...filePreviews.value, { name, size: file.size, url }]
  }
  error.value = ''
  parsed.value = []
  selectedIndices.value = new Set()
  expandedIndices.value = new Set()
  success.value = ''
}

const blobUrlToFile = async (blobUrl) => {
  try {
    const res = await fetch(blobUrl)
    const blob = await res.blob()
    return new File([blob], 'pasted_image.' + (blob.type.split('/')[1] || 'png'), { type: blob.type })
  } catch {
    return null
  }
}

const dataUriToFile = (dataUri) => {
  const match = dataUri.match(/^data:(image\/\w+);base64,(.+)$/)
  if (!match) return null
  const byteChars = atob(match[2])
  const byteArr = new Uint8Array(byteChars.length)
  for (let i = 0; i < byteChars.length; i++) byteArr[i] = byteChars.charCodeAt(i)
  const ext = match[1].split('/')[1] || 'png'
  return new File([byteArr], 'pasted_image.' + ext, { type: match[1] })
}

const onPaste = async (e) => {
  const clipboardData = e.clipboardData
  if (!clipboardData) return

  const clipboardFiles = Array.from(clipboardData.files)
  const imageFiles = clipboardFiles.filter(f => f.type.startsWith('image/'))
  if (imageFiles.length) {
    e.preventDefault()
    for (const f of imageFiles) {
      addImageFile(f)
    }
    return
  }

  const items = Array.from(clipboardData.items)
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const blob = item.getAsFile()
      if (blob) {
        e.preventDefault()
        addImageFile(blob)
        return
      }
    }
  }

  const html = clipboardData.getData('text/html')
  if (html) {
    const parser = new DOMParser()
    const doc = parser.parseFromString(html, 'text/html')
    const imgs = doc.querySelectorAll('img')
    if (imgs.length) {
      e.preventDefault()
      for (const img of imgs) {
        const src = img.src || img.getAttribute('src')
        if (!src) continue

        if (src.startsWith('data:')) {
          const file = dataUriToFile(src)
          if (file) addImageFile(file)
        } else if (src.startsWith('blob:')) {
          const file = await blobUrlToFile(src)
          if (file) addImageFile(file)
        } else if (src.startsWith('http') || src.startsWith('//')) {
          try {
            const res = await fetch(src.startsWith('//') ? 'https:' + src : src)
            const blob = await res.blob()
            if (blob.type.startsWith('image/')) {
              const name = src.split('/').pop()?.split('?')[0] || 'remote_image.png'
              const file = new File([blob], name, { type: blob.type })
              addImageFile(file)
            }
          } catch {
            // Cross-origin or network error, skip
          }
        }
      }
      return
    }
  }

  const text = clipboardData.getData('text/plain')
  if (text && (text.startsWith('http') || text.startsWith('data:'))) {
    e.preventDefault()
    if (text.startsWith('data:')) {
      const file = dataUriToFile(text)
      if (file) addImageFile(file)
    } else {
      try {
        const res = await fetch(text)
        const blob = await res.blob()
        if (blob.type.startsWith('image/')) {
          const name = text.split('/').pop()?.split('?')[0] || 'url_image.png'
          const file = new File([blob], name, { type: blob.type })
          addImageFile(file)
        }
      } catch {
        // Not a reachable image URL
      }
    }
  }
}

onMounted(() => {
  document.addEventListener('paste', onPaste)
})

onUnmounted(() => {
  document.removeEventListener('paste', onPaste)
})

const difficultyLabel = (d) => {
  if (!d) return '中等'
  return d === 'EASY' ? '简单' : d === 'HARD' ? '困难' : '中等'
}

// Selection helpers
const toggleSelect = (i) => {
  const s = new Set(selectedIndices.value)
  if (s.has(i)) s.delete(i); else s.add(i)
  selectedIndices.value = s
}

const selectAll = () => {
  selectedIndices.value = new Set(parsed.value.map((_, i) => i))
}

const deselectAll = () => {
  selectedIndices.value = new Set()
}

const toggleExpand = (i) => {
  const s = new Set(expandedIndices.value)
  if (s.has(i)) s.delete(i); else s.add(i)
  expandedIndices.value = s
}

const removeQuestion = (i) => {
  parsed.value = parsed.value.filter((_, idx) => idx !== i)
  const s = new Set([...selectedIndices.value].filter(idx => idx < i).concat([...selectedIndices.value].filter(idx => idx > i).map(idx => idx - 1)))
  selectedIndices.value = s
}

const isSelected = (i) => selectedIndices.value.has(i)
const isExpanded = (i) => expandedIndices.value.has(i)

const selectCount = computed(() => selectedIndices.value.size)
const totalCount = computed(() => parsed.value.length)
</script>

<template>
  <div>
    <div class="page-header mb-4">
      <div>
        <h2>题目批量导入</h2>
        <p>支持 Excel、Word、PDF、TXT 格式的文件上传与解析，也支持图片题目识别（OCR），可直接粘贴截图。单张图片可识别多道题目</p>
      </div>
    </div>

    <!-- Upload Card -->
    <div class="card mb-4">
      <div class="card-header">
        <h3>1. 上传文件</h3>
      </div>
      <div class="card-body">
        <div class="flex items-center gap-4">
          <label class="btn btn-primary" style="cursor: pointer; padding: 10px 20px;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 6px; vertical-align: middle;">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            选择文件
            <input type="file" multiple :accept="ACCEPTED_DOC + ',' + ACCEPTED_IMG" @change="onFileChange" style="display: none;" />
          </label>
          <button class="btn btn-outline" style="padding: 10px 20px;" @click="downloadTemplate">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 6px; vertical-align: middle;">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            下载模板
          </button>
          <span v-if="files.length" class="text-sm">
            已选择 {{ files.length }} 个文件
          </span>
          <span v-else class="text-sm text-muted">
            支持 .xlsx / .docx / .pdf / .txt / .png / .jpg / .webp（可多选）
          </span>
        </div>

        <!-- Paste hint area -->
        <div class="mt-3" style="border: 2px dashed var(--slate-200); border-radius: 8px; padding: 16px; text-align: center; cursor: pointer; transition: border-color 0.2s;"
             @click="$event.target.querySelector('input')?.click()"
             @dragover.prevent
             @drop.prevent="addFiles(Array.from($event.dataTransfer.files))">
          <div class="text-sm text-muted">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: middle; margin-right: 4px;">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
            在此页面按 <kbd style="padding: 2px 6px; background: var(--slate-100); border-radius: 3px; font-size: 12px;">Ctrl+V</kbd> 粘贴图片，或拖拽文件到此处
          </div>
        </div>

        <!-- Image previews -->
        <div v-if="filePreviews.length" class="mt-4" style="display: flex; flex-wrap: wrap; gap: 12px;">
          <div v-for="(img, i) in filePreviews" :key="i" style="position: relative;">
            <img :src="img.url" :alt="img.name" style="width: 120px; height: 120px; object-fit: cover; border-radius: 8px; border: 1px solid var(--slate-200);" />
            <div class="text-xs text-muted mt-1" style="max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ img.name }}</div>
          </div>
        </div>

        <!-- File list for non-images -->
        <div v-else-if="files.length" class="mt-4">
          <div v-for="(f, i) in files" :key="i" class="flex items-center gap-2 mb-1">
            <span class="text-sm">📄 {{ f.name }}</span>
            <span class="text-xs text-muted">({{ formatSize(f.size) }})</span>
          </div>
        </div>

        <div v-if="files.length" class="mt-4 flex gap-2">
          <button class="btn btn-primary" :disabled="parsing" @click="doParse">
            <span v-if="parsing" class="spinner spinner-sm" style="width:14px;height:14px;border-width:2px;margin-right:6px;"></span>
            {{ parsing ? '解析中...' : '解析题目' }}
          </button>
          <button class="btn btn-outline" @click="clearFiles">清除</button>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-if="error" class="card mb-4" style="border-color: var(--danger); background: var(--danger-bg);">
      <div class="card-body">
        <div style="color: var(--danger); font-weight: 600;">{{ error }}</div>
      </div>
    </div>

    <!-- Success -->
    <div v-if="success" class="card mb-4" style="border-color: var(--success); background: var(--success-bg);">
      <div class="card-body">
        <div style="color: var(--success); font-weight: 600;">{{ success }}</div>
      </div>
    </div>

    <!-- Preview with multi-question support -->
    <div v-if="parsed.length" class="card mb-4">
      <div class="card-header">
        <h3>2. 预览题目（共 {{ totalCount }} 道，已选 {{ selectCount }} 道）</h3>
        <div class="flex gap-2">
          <button class="btn btn-sm btn-outline" @click="selectAll">全选</button>
          <button class="btn btn-sm btn-outline" @click="deselectAll">取消</button>
          <button class="btn btn-success" :disabled="!selectCount || saving" @click="doSave">
            <span v-if="saving" class="spinner spinner-sm" style="width:14px;height:14px;border-width:2px;margin-right:6px;"></span>
            {{ saving ? '保存中...' : '导入选中 (' + selectCount + ')' }}
          </button>
        </div>
      </div>
      <div class="card-body" style="padding: 0;">
        <!-- Question cards instead of table -->
        <div style="display: flex; flex-direction: column;">
          <div v-for="(q, i) in parsed" :key="i"
               style="border-bottom: 1px solid var(--slate-100); transition: background 0.15s;"
               :style="{ background: isSelected(i) ? 'var(--slate-50)' : '' }">
            <!-- Row header: checkbox + meta + actions -->
            <div class="flex items-center gap-3" style="padding: 12px 16px; cursor: pointer;" @click="toggleExpand(i)">
              <input type="checkbox" :checked="isSelected(i)" @change.stop="toggleSelect(i)"
                     style="width: 16px; height: 16px; cursor: pointer; flex-shrink: 0;" />
              <span class="text-sm font-mono" style="color: var(--slate-400); min-width: 24px;">#{{ i + 1 }}</span>
              <span v-if="q.type" :class="['badge', q.type === 'SINGLE_CHOICE' ? 'badge-blue' : q.type === 'MULTIPLE_CHOICE' ? 'badge-cyan' : q.type === 'TRUE_FALSE' ? 'badge-yellow' : 'badge-gray']">
                {{ questionTypeLabel(q.type) }}
              </span>
              <span v-else class="badge badge-gray">—</span>
              <span :class="['badge', q.difficulty === 'EASY' ? 'badge-green' : q.difficulty === 'HARD' ? 'badge-red' : 'badge-yellow']" style="font-size: 12px;">
                {{ difficultyLabel(q.difficulty) }}
              </span>
              <span v-if="q.category" class="text-sm text-muted" style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                {{ q.category }}
                <span v-if="q.knowledgePoint">· {{ q.knowledgePoint }}</span>
              </span>
              <span v-else class="text-sm text-muted" style="flex: 1;"></span>
              <button class="btn btn-sm btn-outline" @click.stop="removeQuestion(i)" style="padding: 2px 8px; font-size: 12px; color: var(--danger);">
                移除
              </button>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                   :style="{ transition: 'transform 0.2s', transform: isExpanded(i) ? 'rotate(90deg)' : '' }">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            </div>

            <!-- Expanded detail -->
            <div v-if="isExpanded(i)" style="padding: 0 16px 16px 56px;">
              <div class="mb-2">
                <div class="text-xs text-muted mb-1">题干</div>
                <div class="text-sm" style="white-space: pre-wrap; line-height: 1.6;">{{ q.stem || '—' }}</div>
              </div>
              <div v-if="q.options && Object.keys(q.options).length" class="mb-2">
                <div class="text-xs text-muted mb-1">选项</div>
                <div class="text-sm" style="line-height: 1.8;">
                  <div v-for="(val, key) in q.options" :key="key">
                    <strong>{{ key }}.</strong> {{ val }}
                  </div>
                </div>
              </div>
              <div class="mb-2">
                <div class="text-xs text-muted mb-1">答案</div>
                <div class="text-sm" style="color: var(--success); font-weight: 600;">{{ q.answer || '—' }}</div>
              </div>
              <div v-if="q.analysis">
                <div class="text-xs text-muted mb-1">解析</div>
                <div class="text-sm" style="color: var(--slate-600); line-height: 1.6;">{{ q.analysis }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Template Guide -->
    <div class="card">
      <div class="card-header">
        <h3>使用说明</h3>
      </div>
      <div class="card-body">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px;">
          <div>
            <h4 class="text-sm font-semibold mb-2">📊 Excel 结构化导入</h4>
            <p class="text-sm text-muted mb-2">首行为表头，支持列名：</p>
            <div class="text-sm" style="line-height: 1.8;">
              <code>题型</code> · <code>分类</code> · <code>题干</code> · <code>选项A/B/C/D</code> · <code>答案</code> · <code>解析</code> · <code>难度</code> · <code>知识点</code>
            </div>
          </div>
          <div>
            <h4 class="text-sm font-semibold mb-2">📄 文档 / 图片 AI 识别</h4>
            <p class="text-sm text-muted mb-2">以下格式将通过 AI 视觉模型智能识别：</p>
            <div class="text-sm" style="line-height: 1.8;">
              <code>.docx</code> · <code>.pdf</code> · <code>.txt</code> · <code>.png</code> · <code>.jpg</code> · <code>.webp</code>
              <br /><span class="text-muted">PDF 扫描件 / 题目照片均可识别，单张图片自动识别多道题目</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
