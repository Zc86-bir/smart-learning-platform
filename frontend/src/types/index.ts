/**
 * Shared types for frontend ↔ backend API responses.
 */

export interface ApiResponse<T> {
  code: number
  data: T
  message: string
}

export interface Question {
  id: number
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'SHORT_ANSWER' | 'CODING'
  category: string
  stem: string
  options?: Record<string, string>
  answer: string
  analysis?: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  knowledgePoint?: string
}

export interface ExamRecord {
  id: number
  userId: number
  paperId: number
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED' | 'FORCE_SUBMITTED'
  score?: number
  totalScore: number
  startTime: string
  submitTime?: string
  durationSeconds?: number
  cutScreenCount: number
  clipboardCount: number
  suspiciousFlags: string[]
  aiReport?: GradingReport
}

export interface GradingReport {
  totalScore: number
  details: GradingDetail[]
  summary: string
  advice: string
}

export interface GradingDetail {
  questionId: number
  score: number
  fullScore: number
  correctness: 'CORRECT' | 'WRONG' | 'PARTIAL'
  comment: string
  suggestion: string
}

export interface Paper {
  id: number
  title: string
  description: string
  totalScore: number
  durationMinutes: number
  category: string
  createdAt: string
  questions: QuestionWithScore[]
}

export interface QuestionWithScore {
  id: number
  type: string
  stem: string
  options?: Record<string, string>
  answer: string
  analysis?: string
  difficulty: string
  score: number
}

export interface DashboardData {
  totalExams: number
  avgScore: number
  avgAccuracy: number
  totalQuestions: number
  scoreTrend: { date: string; score: number; totalScore: number }[]
  wrongCategoryDist: Record<string, number>
  knowledgeMastery: Record<string, number>
}

export interface WrongQuestion {
  id: number
  questionId: number
  studentAnswer: string
  correctAnswer: string
  difficulty: string
  category: string
  knowledgePoint?: string
  wrongCount: number
  mastered: boolean
  lastWrongTime: string
}

export interface TutorMessage {
  role: string
  content: string
}

export interface LeaderboardEntry {
  userId: number
  nickname: string
  score: number
  durationSeconds: number
  durationDisplay: string
}

export interface FilePreview {
  name: string
  size: number
  url: string
}

export type UserRole = 'ADMIN' | 'STUDENT'
