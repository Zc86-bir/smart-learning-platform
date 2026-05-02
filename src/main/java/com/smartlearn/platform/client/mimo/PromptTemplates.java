package com.smartlearn.platform.client.mimo;

/**
 * AI prompt templates for question generation and grading.
 * Placeholders use {key} format for runtime substitution.
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    public static final String JSON_OUTPUT_INSTRUCTION =
        "Output in strictly valid JSON format only. Do not include any explanatory text, "
        + "markdown formatting, or additional content outside the JSON object.";

    // ========== Question Generation ==========

    public static String generateQuestionsPrompt(
        String category,
        String difficulty,
        int count,
        String knowledgePoint,
        String... types
    ) {
        var kp = knowledgePoint != null && !knowledgePoint.isBlank()
            ? knowledgePoint
            : "相关核心知识点";

        var typeList = types != null && types.length > 0
            ? String.join("、", types)
            : "单选题、多选题、判断题";

        var isStem = category.contains("数学") || category.contains("物理")
            || category.contains("化学") || category.contains("生物")
            || category.contains("高等数学") || category.contains("线性代数")
            || category.contains("概率论") || category.contains("科学")
            || category.contains("地理");

        var latexInstruction = isStem ? """

            LaTeX 格式要求（重要）：
            - 所有数学公式、化学方程式、物理符号必须使用 LaTeX 格式
            - 行内公式用 $...$ 包裹，如：$x^2 + y^2 = 1$、$\\frac{a}{b}$、$\\sqrt{x}$
            - 独立公式用 $$...$$ 包裹，居中显示，如：$$\\int_0^\\infty e^{-x} dx = 1$$
            - 化学方程式示例：$2H_2 + O_2 \\rightarrow 2H_2O$
            - 希腊字母：$\\alpha, \\beta, \\gamma, \\theta, \\pi, \\sigma, \\Delta$
            - 矩阵：$$\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}$$
            - 分数：$\\frac{分子}{分母}$，根号：$\\sqrt{x}$，求和：$\\sum_{i=1}^n$
            """ : "";

        return """
            你是一位资深教育专家，擅长根据知识点出题。请生成 **%d 道**高质量的 %s 类题目。

            分类：%s
            难度：%s
            知识点：%s
            题型要求：%s

            题型格式说明：
            1. 单选题 (SINGLE_CHOICE)：必须提供 4 个选项(A-D)，仅有 1 个正确答案，答案格式如 "A"
            2. 多选题 (MULTIPLE_CHOICE)：必须提供 4-6 个选项，有 2-4 个正确答案，答案格式如 "A,C"
            3. 判断题 (TRUE_FALSE)：无需选项，答案为 "true" 或 "false"
            4. 简答题 (SHORT_ANSWER)：无需选项，答案为核心要点

            请严格返回一个 **JSON 数组**（以 `[` 开头，以 `]` 结尾），必须恰好包含 **%d 个**题目对象。
            不要返回单个对象，不要包裹在 {"questions": [...]} 等包装对象中。
            数组格式：
            [
              {
                "type": "SINGLE_CHOICE|MULTIPLE_CHOICE|TRUE_FALSE|SHORT_ANSWER",
                "category": "%s",
                "stem": "清晰完整的题目描述",
                "options": {"A": "...", "B": "...", "C": "...", "D": "..."},
                "answer": "标准答案",
                "analysis": "详细解析，包含解题思路和关键知识点",
                "difficulty": "%s",
                "knowledgePoint": "具体知识点标签"
              }
            ]

            要求：
            - 题目原创，不得抄袭网络现有题目
            - 题干表述清晰、专业、无歧义
            - 干扰选项要有迷惑性，不能太明显
            - 解析要详细，包含解题步骤和关键概念
            - 各题型合理分布，但总数必须为 %d 道
            - 难度与要求一致
            %s
            只输出 JSON 数组，不要任何额外文字。
            """.formatted(
            count, category, category, difficulty, kp, typeList,
            count, category, difficulty, count,
            latexInstruction
        );
    }

    // ========== AI Grading ==========

    public static String gradingPrompt(
        String questionType,
        String questionStem,
        String standardAnswer,
        String studentAnswer,
        int fullScore
    ) {
        return """
            你是一位公正的阅卷老师。请对以下学生答案进行评分。

            题型：%s
            题目：%s
            标准答案：%s
            学生答案：%s
            满分：%d分

            请返回严格符合以下JSON Schema的对象：
            {
              "score": 0,        // 实际得分（整数，不超过满分）
              "fullScore": %d,   // 满分
              "correctness": "CORRECT|PARTIAL|WRONG",
              "evaluation": "对答案的评价，指出优点和不足",
              "suggestion": "改进建议"
            }

            评分原则：
            - 单选题/判断题：答对得满分，答错得0分
            - 多选题：完全正确得满分，部分正确得50%%，错误得0分
            - 简答题：根据要点覆盖度给分
            - 编程题：根据代码正确性、效率、可读性综合评分

            %s
            """.formatted(
            questionType, questionStem, standardAnswer, studentAnswer, fullScore,
            fullScore,
            JSON_OUTPUT_INSTRUCTION
        );
    }

    // ========== Bulk Grading (batch multiple questions) ==========

    public static String bulkGradingPrompt(String examContent, int totalScore) {
        return """
            你是一位公正的阅卷老师。请对以下整份试卷进行批量评分。

            试卷内容（包含题目、标准答案、学生作答）：
            %s

            试卷满分：%d分

            请返回严格符合以下JSON Schema的对象：
            {
              "totalScore": 0,      // 总分
              "details": [
                {
                  "questionId": 0,
                  "score": 0,
                  "fullScore": %d,
                  "correctness": "CORRECT|PARTIAL|WRONG",
                  "evaluation": "评价",
                  "suggestion": "改进建议"
                }
              ],
              "overallEvaluation": "整体评价",
              "suggestion": "综合改进建议"
            }

            %s
            """.formatted(examContent, totalScore, totalScore / 5, JSON_OUTPUT_INSTRUCTION);
    }

    // ========== AI Tutor (Socratic Method) ==========

    public static final String TUTOR_SYSTEM = """
        你是一位苏格拉底式AI导师。你的目标是引导学生独立思考，而不是直接给答案。

        严格遵循以下原则：
        1. **绝不直接给出答案**：即使学生要求，也要婉拒并引导思考
        2. **反问优先**：用"你是怎么想的？""你觉得这个条件意味着什么？"引导学生
        3. **拆解问题**：将复杂问题分解为更小的子问题，逐一引导
        4. **肯定正确思路**：当学生答对部分时给予肯定
        5. **指出错误方向**：温和地指出学生思路中的偏差
        6. **保持简洁**：每次回复不超过3句话

        请使用学生的母语回复。回答要有温度、有耐心，像一个好老师一样循循善诱。
        """;

    public static String tutorUserPrompt(String questionStem, String standardAnswer, String conversationContext) {
        return """
            学生正在请教以下题目：

            题目：%s
            参考答案：%s

            %s

            请用苏格拉底式教学方法引导学生思考。不要直接告诉学生答案。
            """.formatted(questionStem, standardAnswer, conversationContext);
    }
}
