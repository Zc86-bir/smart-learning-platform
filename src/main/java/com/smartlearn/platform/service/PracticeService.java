package com.smartlearn.platform.service;

import com.smartlearn.platform.dto.QuestionDTO;

import java.util.List;
import java.util.Map;

public interface PracticeService {
    PracticeResult startPractice(Long userId, String category, String difficulty, int count);
    PracticeAnswerResult submitAnswer(Long userId, Long sessionId, Long questionId, String answer);
    PracticeReport finishPractice(Long userId, Long sessionId);
    PracticeReport getSessionReport(Long userId, Long sessionId);

    record PracticeResult(Long sessionId, List<QuestionDTO> questions) {}

    record PracticeAnswerResult(
        Long questionId,
        boolean correct,
        String correctAnswer,
        String analysis
    ) {}

    record PracticeReport(
        Long sessionId,
        String category,
        int questionCount,
        int correctCount,
        int accuracy,
        List<QuestionDetail> questions
    ) {
        record QuestionDetail(
            Long id,
            String stem,
            String type,
            String difficulty,
            String userAnswer,
            String correctAnswer,
            String analysis,
            boolean correct
        ) {}
    }
}
