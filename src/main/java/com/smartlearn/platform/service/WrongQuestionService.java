package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.WrongQuestionDTO;

public interface WrongQuestionService {
    /**
     * Auto-add a wrong question after exam grading.
     */
    void addWrongQuestion(Long userId, Long questionId, String studentAnswer,
                          String correctAnswer, Long examRecordId);

    /**
     * Get user's wrong question list (paginated).
     */
    Page<WrongQuestionDTO> getWrongQuestions(Long userId, String category,
                                             String difficulty, Boolean mastered,
                                             int page, int size);

    /**
     * Mark a wrong question as mastered.
     */
    void markMastered(Long userId, Long wrongId);

    /**
     * Re-attempt a wrong question (remove if answered correctly, or keep).
     * For now just resets mastered status.
     */
    void resetMastered(Long userId, Long wrongId);

    /**
     * Get wrong question count by category for the user.
     */
    java.util.Map<String, Integer> getWrongCategoryStats(Long userId);
}
