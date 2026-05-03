package com.smartlearn.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.client.mimo.MiMoChat;
import com.smartlearn.platform.dto.QuestionDTO;
import com.smartlearn.platform.entity.Question;
import com.smartlearn.platform.enums.Difficulty;
import com.smartlearn.platform.enums.QuestionType;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.QuestionMapper;
import com.smartlearn.platform.service.AiChatService;
import com.smartlearn.platform.service.QuestionImportService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QuestionImportServiceImpl implements QuestionImportService {

    private final QuestionMapper questionMapper;
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    public QuestionImportServiceImpl(QuestionMapper questionMapper, AiChatService aiChatService, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<QuestionDTO> parseImport(InputStream stream, String extension) {
        return switch (extension.toLowerCase()) {
            case "xlsx", "xls" -> parseExcel(stream);
            case "docx" -> parseDocx(stream);
            case "pdf" -> parsePdf(stream);
            case "txt" -> parseTxt(stream);
            case "png", "jpg", "jpeg", "webp" -> parseImage(stream, extension);
            default -> throw new BizException("不支持的文件格式: " + extension + "，请上传 .xlsx/.docx/.pdf/.txt/.png/.jpg 文件");
        };
    }

    @Override
    public int saveImported(List<QuestionDTO> questions) {
        int saved = 0;
        for (var dto : questions) {
            try {
                var q = new Question();
                q.setType(dto.type());
                q.setCategory(dto.category());
                q.setStem(dto.stem());
                q.setAnswer(dto.answer());
                q.setAnalysis(dto.analysis());
                q.setDifficulty(dto.difficulty());
                q.setKnowledgePoint(dto.knowledgePoint());
                if (dto.options() != null && !dto.options().isEmpty()) {
                    q.setOptions(objectMapper.writeValueAsString(dto.options()));
                }
                questionMapper.insert(q);
                saved++;
            } catch (Exception e) {
                log.warn("[QuestionImport] Failed to save question: {}", dto, e);
            }
        }
        log.info("[QuestionImport] Imported {}/{} questions", saved, questions.size());
        return saved;
    }

    // ==================== Excel parser ====================

    private List<QuestionDTO> parseExcel(InputStream stream) {
        var result = new ArrayList<QuestionDTO>();
        try {
            // Read entire stream into byte[] — required for XLSX random-access format
            var bytes = stream.readAllBytes();
            if (bytes.length == 0) throw new BizException("Excel 文件为空");

            // Try XLSX first (more common), then fallback to XLS
            try (var workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
                extractQuestions(workbook, result);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[QuestionImport] Excel parse error", e);
            throw new BizException("Excel 解析失败: " + e.getMessage());
        }
        return result;
    }

    private void extractQuestions(Workbook workbook, List<QuestionDTO> result) {
        var sheet = workbook.getSheetAt(0);
        var headerRow = sheet.getRow(0);
        if (headerRow == null) throw new BizException("Excel 文件缺少表头行");

        // Map column names to indices (case-insensitive)
        var colMap = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            var cell = headerRow.getCell(i);
            if (cell != null) {
                var val = getCellValue(cell);
                if (val != null && !val.isBlank()) {
                    colMap.put(val.trim().toLowerCase(), i);
                }
            }
        }

        log.info("[QuestionImport] Excel columns detected: {}", colMap.keySet());

        var eval = workbook.getCreationHelper().createFormulaEvaluator();

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            var row = sheet.getRow(r);
            if (row == null) continue;

            var stem = getCell(row, colMap, "题干", eval);
            if (stem == null || stem.isBlank()) continue;

            var typeStr = getCell(row, colMap, "题型", eval);
            var type = parseQuestionType(typeStr);

            var category = getCell(row, colMap, "分类", eval);
            var knowledgePoint = getCell(row, colMap, "知识点", eval);
            var answer = getCell(row, colMap, "答案", eval);
            var analysis = getCell(row, colMap, "解析", eval);
            var diffStr = getCell(row, colMap, "难度", eval);
            var difficulty = parseDifficulty(diffStr);

            // Collect options
            var options = new LinkedHashMap<String, String>();
            for (var col : List.of("选项a", "a", "选项b", "b", "选项c", "c", "选项d", "d")) {
                var idx = colMap.get(col);
                if (idx != null) {
                    var val = getCell(row, idx, eval);
                    if (val != null && !val.isBlank()) {
                        options.put(col.toUpperCase().replaceAll("选项", ""), val);
                    }
                }
            }

            result.add(new QuestionDTO(
                null, type, category, stem,
                options.isEmpty() ? null : options,
                answer, analysis, difficulty, knowledgePoint
            ));
        }
    }

    // ==================== Word / PDF / TXT -> AI parser ====================

    private List<QuestionDTO> parseDocx(InputStream stream) {
        var text = new StringBuilder();
        try (var doc = new XWPFDocument(stream)) {
            doc.getParagraphs().forEach(p -> text.append(p.getText()).append("\n"));
            doc.getTables().forEach(table ->
                table.getRows().forEach(row ->
                    row.getTableCells().forEach(cell -> text.append(cell.getText()).append("\t"))));
        } catch (IOException e) {
            throw new BizException("Word 解析失败: " + e.getMessage());
        }
        return aiParseQuestions(text.toString());
    }

    private static final int MAX_PAGES_FOR_VISION = 10;

    private List<QuestionDTO> parsePdf(InputStream stream) {
        final byte[] bytes;
        try {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new BizException("PDF 读取失败: " + e.getMessage());
        }

        String text;
        try (var pdfDoc = Loader.loadPDF(bytes)) {
            var stripper = new org.apache.pdfbox.text.PDFTextStripper();
            text = stripper.getText(pdfDoc);
            if (text != null && !text.isBlank() && text.trim().length() >= 20) {
                // Text layer exists, use text-based AI parsing
                return aiParseQuestions(text);
            }

            // Scanned PDF — no text layer, render to images and use vision model
            log.info("[QuestionImport] PDF has no text layer, using vision OCR for {} pages",
                Math.min(pdfDoc.getNumberOfPages(), MAX_PAGES_FOR_VISION));
            return aiParseVision(pdfDoc, bytes);
        } catch (IOException e) {
            throw new BizException("PDF 解析失败: " + e.getMessage());
        }
    }

    private List<QuestionDTO> aiParseVision(org.apache.pdfbox.pdmodel.PDDocument pdfDoc, byte[] rawBytes) {
        int pageCount = Math.min(pdfDoc.getNumberOfPages(), MAX_PAGES_FOR_VISION);
        var imageParts = new java.util.ArrayList<MiMoChat.ContentPart>();

        try {
            var renderer = new PDFRenderer(pdfDoc);
            for (int i = 0; i < pageCount; i++) {
                var bi = renderer.renderImageWithDPI(i, 200, ImageType.RGB);
                var baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "PNG", baos);
                var base64 = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
                imageParts.add(MiMoChat.ContentPart.imageUrl("data:image/png;base64," + base64, "high"));
            }
        } catch (IOException e) {
            throw new BizException("PDF 图片渲染失败: " + e.getMessage());
        }

        return aiParseVision(imageParts);
    }

    private List<QuestionDTO> aiParseVision(List<MiMoChat.ContentPart> imageParts) {
        var systemPrompt = """
            你是一位教育数据解析专家。请查看提供的文档图片，从中提取所有题目。
            一张图片通常包含多道题目，请逐题完整提取。
            """;
        var userText = """
            请从这些文档图片中提取所有题目，按以下JSON数组格式返回：
            [
              {"type": "SINGLE_CHOICE", "category": "数学", "stem": "题干", "options": {"A": "选项A", "B": "选项B"}, "answer": "A", "analysis": "解析", "difficulty": "MEDIUM", "knowledgePoint": "知识点"},
              {"type": "TRUE_FALSE", "category": "历史", "stem": "题干", "answer": "对", "difficulty": "EASY"},
              ...更多题目...
            ]

            字段说明：
            - type: 题型（SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/SHORT_ANSWER）
            - category: 学科分类
            - stem: 题干原文
            - options: 选项（JSON对象）
            - answer: 正确答案
            - analysis: 解析（可选）
            - difficulty: 难度（EASY/MEDIUM/HARD）
            - knowledgePoint: 知识点（可选）

            注意：
            1. 必须返回JSON数组，包含图片中的所有题目
            2. 数学公式保留 LaTeX 格式
            3. 不要遗漏任何题目
            """;

        try {
            // Disable jsonMode for longer output, rely on prompt + stripMarkdown
            var rawJson = aiChatService.chatWithVision(
                systemPrompt, userText, imageParts,
                AiChatService.ChatOptions.builder()
                    .thinkingType("disabled")
                    .temperature(0.7)
                    .build()
            ).get();
            var cleaned = aiChatService.stripMarkdown(rawJson);
            return parseAiJson(cleaned);
        } catch (Exception e) {
            log.error("[QuestionImport] Vision parse failed", e);
            throw new BizException("AI 视觉识别文档失败: " + e.getMessage());
        }
    }

    private List<QuestionDTO> parseTxt(InputStream stream) {
        var text = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new BizException("文本解析失败: " + e.getMessage());
        }
        return aiParseQuestions(text.toString());
    }

    private List<QuestionDTO> parseImage(InputStream stream, String extension) {
        final byte[] bytes;
        try {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            throw new BizException("图片读取失败: " + e.getMessage());
        }

        var mimeType = switch (extension.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };

        var base64 = java.util.Base64.getEncoder().encodeToString(bytes);
        var part = MiMoChat.ContentPart.imageUrl("data:" + mimeType + ";base64," + base64, "high");
        return aiParseVision(List.of(part));
    }

    private List<QuestionDTO> aiParseQuestions(String rawText) {
        if (rawText == null || rawText.isBlank() || rawText.trim().length() < 20) {
            throw new BizException("文件内容过少，无法识别题目");
        }

        var systemPrompt = """
            你是一位教育数据解析专家。擅长从文档文本中提取所有题目。

            重要：一段文本通常包含多道题目（一般3-10道），请逐题完整提取，不要遗漏。
            请先数出有多少道题，然后按顺序编号提取。

            你必须且只能返回一个JSON数组。
            """;
        var userPrompt = """
            请从以下文档文本中提取所有题目。

            识别步骤：
            1. 先数清文本中有多少道题目
            2. 逐题按顺序提取，确保不遗漏

            每道题的JSON字段：
            - type: 题型（SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/SHORT_ANSWER）
            - category: 学科分类（如无法判断则填"未知"）
            - stem: 题干（完整原文，保留 LaTeX 公式）
            - options: 选项（JSON对象 {"A": "...", "B": "..."}）
            - answer: 正确答案
            - analysis: 解析/解题思路
            - difficulty: 难度（EASY/MEDIUM/HARD）
            - knowledgePoint: 知识点

            严格要求：
            1. 返回JSON数组 [ {...}, {...}, ... ]，包含所有题目
            2. 不要遗漏任何一道题
            3. 数学公式保留 LaTeX 格式

            文档文本：
            %s
            """.formatted(rawText.substring(0, Math.min(rawText.length(), 20000)));

        try {
            var rawJson = aiChatService.chat(
                systemPrompt, userPrompt,
                AiChatService.ChatOptions.builder()
                    .thinkingType("disabled")
                    .temperature(0.7)
                    .jsonMode(true)
                    .build()
            ).get();
            var cleaned = aiChatService.stripMarkdown(rawJson);
            return parseAiJson(cleaned);
        } catch (Exception e) {
            log.error("[QuestionImport] AI parse failed", e);
            throw new BizException("AI 解析文档失败: " + e.getMessage());
        }
    }

    // ==================== Helpers ====================

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                var s = cell.getStringCellValue();
                yield (s == null) ? null : s;
            }
            case NUMERIC -> {
                var d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getCellFormula();
                } catch (Exception e) {
                    yield null;
                }
            }
            case BLANK -> null;
            default -> null;
        };
    }

    private String getCell(Row row, Map<String, Integer> colMap, String name, FormulaEvaluator eval) {
        var idx = colMap.get(name);
        return idx != null ? getCell(row, idx, eval) : null;
    }

    private String getCell(Row row, int idx, FormulaEvaluator eval) {
        var cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.FORMULA && eval != null) {
            try {
                cell = cell.getCachedFormulaResultType() == CellType.STRING ? cell : cell;
            } catch (Exception ignored) {}
        }
        return getCellValue(cell);
    }

    private QuestionType parseQuestionType(String s) {
        if (s == null || s.isBlank()) return QuestionType.SINGLE_CHOICE;
        var upper = s.toUpperCase().trim();
        return switch (upper) {
            case "SINGLE_CHOICE", "单选题" -> QuestionType.SINGLE_CHOICE;
            case "MULTIPLE_CHOICE", "多选题" -> QuestionType.MULTIPLE_CHOICE;
            case "TRUE_FALSE", "判断题" -> QuestionType.TRUE_FALSE;
            case "SHORT_ANSWER", "简答题" -> QuestionType.SHORT_ANSWER;
            default -> QuestionType.SINGLE_CHOICE;
        };
    }

    private Difficulty parseDifficulty(String s) {
        if (s == null || s.isBlank()) return Difficulty.MEDIUM;
        var upper = s.toUpperCase().trim();
        return switch (upper) {
            case "EASY", "简单" -> Difficulty.EASY;
            case "MEDIUM", "中等" -> Difficulty.MEDIUM;
            case "HARD", "困难" -> Difficulty.HARD;
            default -> Difficulty.MEDIUM;
        };
    }

    private List<QuestionDTO> parseAiJson(String json) {
        if (json == null || json.isBlank()) {
            throw new BizException("AI 返回内容为空，请重试");
        }
        log.info("[QuestionImport] Raw AI response (first 1000 chars): {}", json.substring(0, Math.min(json.length(), 1000)));

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("[QuestionImport] Invalid JSON: {}", e.getMessage());
            throw new BizException("AI 返回的不是有效的 JSON 格式");
        }

        try {
            if (node.isArray()) {
                log.info("[QuestionImport] Parsed {} questions from top-level array", node.size());
                return convertToQuestionList(node);
            }
            if (node.isObject()) {
                // Try known wrapper keys
                for (var key : List.of("questions", "data", "items", "result", "response")) {
                    var arr = node.get(key);
                    if (arr != null && arr.isArray()) {
                        log.info("[QuestionImport] Parsed {} questions from key '{}'", arr.size(), key);
                        return convertToQuestionList(arr);
                    }
                }
                // Try as single question object
                if (hasQuestionField(node)) {
                    log.info("[QuestionImport] Parsed as single question object");
                    return List.of(convertSingleQuestion(node));
                }
            }
            log.warn("[QuestionImport] Unrecognized JSON: first 500 chars = {}", json.substring(0, Math.min(json.length(), 500)));
            throw new BizException("AI 返回格式无法识别，请检查文件内容是否为标准题目格式");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[QuestionImport] Conversion error: {}", e.getMessage());
            throw new BizException("解析 AI 返回失败: " + e.getMessage());
        }
    }

    private List<QuestionDTO> convertToQuestionList(JsonNode arrayNode) {
        var result = new ArrayList<QuestionDTO>();
        for (var item : arrayNode) {
            result.add(convertSingleQuestion(item));
        }
        return result;
    }

    private QuestionDTO convertSingleQuestion(JsonNode node) {
        // Normalize Chinese type values to enum constants
        var typeNode = node.get("type");
        if (typeNode != null && typeNode.isTextual()) {
            var typeVal = typeNode.asText();
            var normalized = normalizeType(typeVal);
            if (!normalized.equals(typeVal)) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("type", normalized);
            }
        }
        // Normalize Chinese difficulty values
        var diffNode = node.get("difficulty");
        if (diffNode != null && diffNode.isTextual()) {
            var diffVal = diffNode.asText();
            var normalized = normalizeDifficulty(diffVal);
            if (!normalized.equals(diffVal)) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("difficulty", normalized);
            }
        }
        try {
            return objectMapper.treeToValue(node, QuestionDTO.class);
        } catch (Exception e) {
            log.warn("[QuestionImport] Failed to convert node: {}", e.getMessage());
            return new QuestionDTO(null, null, null, null, null, null, null, null, null);
        }
    }

    private boolean hasQuestionField(JsonNode node) {
        return node.has("stem") || node.has("question") || node.has("content");
    }

    private String normalizeType(String s) {
        return switch (s) {
            case "单选题" -> "SINGLE_CHOICE";
            case "多选题" -> "MULTIPLE_CHOICE";
            case "判断题" -> "TRUE_FALSE";
            case "简答题" -> "SHORT_ANSWER";
            case "编程题" -> "CODING";
            default -> s;
        };
    }

    private String normalizeDifficulty(String s) {
        return switch (s) {
            case "简单" -> "EASY";
            case "中等" -> "MEDIUM";
            case "困难" -> "HARD";
            default -> s;
        };
    }
}
