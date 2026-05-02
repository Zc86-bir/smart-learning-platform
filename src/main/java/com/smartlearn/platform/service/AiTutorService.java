package com.smartlearn.platform.service;

import com.smartlearn.platform.dto.TutorMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

public interface AiTutorService {
    SseEmitter ask(Long userId, Long questionId, String message, String questionStem, String standardAnswer);
    void clearHistory(Long userId, Long questionId);
    List<TutorMessageDTO> getHistory(Long userId, Long questionId);
}
