package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.request.GenerateQuestionsRequest;

import java.util.List;
import java.util.Map;

public interface QuestionService {
    List<QuestionDTO> generateQuestions(GenerateQuestionsRequest request);
    QuestionDTO saveQuestion(Question question);
    QuestionDTO getQuestionById(Long id);
    Page<QuestionDTO> listQuestions(String category, String difficulty, String keyword, int page, int size);
    void incrementHotScore(Long questionId);
    List<QuestionDTO> getHotQuestions(int limit);
    QuestionDTO updateQuestion(Long id, Question question);
    void deleteQuestion(Long id);
    int batchDeleteQuestions(List<Long> ids);
    Map<String, Long> getCategoryStats();
}
