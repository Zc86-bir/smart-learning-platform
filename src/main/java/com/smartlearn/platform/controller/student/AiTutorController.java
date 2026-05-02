package com.smartlearn.platform.controller.student;

import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.request.TutorRequest;
import com.smartlearn.platform.service.AiTutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/student/ai-tutor")
@Tag(name = "AI一对一答疑", description = "苏格拉底式AI导师")
@com.smartlearn.platform.interceptor.RequireRole("STUDENT")
public class AiTutorController {

    private final AiTutorService aiTutorService;

    public AiTutorController(AiTutorService aiTutorService) {
        this.aiTutorService = aiTutorService;
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发起追问（SSE流式）")
    public SseEmitter ask(
        HttpServletRequest request,
        @Valid @RequestBody TutorRequest req
    ) {
        var userId = (Long) request.getAttribute("userId");
        var qStem = req.questionStem();
        var qAnswer = req.standardAnswer();

        if (qStem == null || qStem.isBlank()) {
            throw new BizException("请提供题目题干");
        }
        if (qAnswer == null || qAnswer.isBlank()) {
            throw new BizException("请提供参考答案");
        }

        return aiTutorService.ask(userId, req.questionId(), req.message(), qStem, qAnswer);
    }

    @DeleteMapping("/clear/{questionId}")
    @Operation(summary = "清空某题目的对话历史")
    public org.springframework.http.ResponseEntity<Void> clear(
        HttpServletRequest request,
        @PathVariable Long questionId
    ) {
        var userId = (Long) request.getAttribute("userId");
        aiTutorService.clearHistory(userId, questionId);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/history/{questionId}")
    @Operation(summary = "获取某题目的对话历史")
    public com.smartlearn.platform.dto.ApiResponse<java.util.List<com.smartlearn.platform.dto.TutorMessageDTO>> getHistory(
        HttpServletRequest request,
        @PathVariable Long questionId
    ) {
        var userId = (Long) request.getAttribute("userId");
        return com.smartlearn.platform.dto.ApiResponse.ok(aiTutorService.getHistory(userId, questionId));
    }
}
