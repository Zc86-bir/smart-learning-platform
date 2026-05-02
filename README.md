# 智能学习平台 (Smart Learn Platform)

基于 Spring Boot + MiMo AI 的智能出题与判卷后端系统。

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 21 (Virtual Threads) |
| Spring Boot | 3.5.0 |
| MyBatis-Plus | 3.5.12 |
| MySQL | 8.4 |
| Redis | 7.x |
| MiMo AI | MiMo-V2.5-Pro |

## 快速启动

### 1. 启动基础设施

```bash
docker compose up -d
```

### 2. 设置 API Key

编辑 `.env` 文件，确保 `MIMO_API_KEY` 已配置：

```
MIMO_API_KEY=your-api-key-here
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

访问 Swagger UI: http://localhost:8080/swagger-ui.html

## 项目结构

```
src/main/java/com/smartlearn/platform/
├── client/mimo/          # MiMo AI 客户端
│   ├── MiMoAiClient      # HttpClient + 虚拟线程 + 429退避
│   ├── MiMoChat          # API 请求/响应 Records
│   └── PromptTemplates   # AI Prompt 模板
├── config/               # 配置类
├── controller/
│   ├── admin/            # 管理端 (/api/admin/*)
│   │   ├── QuestionController   # 题库管理 + AI出题
│   │   ├── VideoController      # 视频上传/审核
│   │   ├── BannerController     # 轮播图CRUD
│   │   └── CategoryController   # 分类管理
│   └── student/          # 学生端 (/api/student/*)
│       ├── ExamController       # 考试/提交/排行榜/防作弊
│       └── StudentController    # 公共查询
├── entity/               # 数据库实体
├── enums/                # 枚举 (QuestionType, Difficulty, etc.)
├── exception/            # 全局异常处理
├── interceptor/          # Header拦截器 (X-User-Id, X-User-Role)
├── mapper/               # MyBatis-Plus Mapper
├── service/              # 业务接口 + 实现
└── dto/                  # 统一返回 ApiResponse
```

## API 规范

### 认证方式

项目支持两种认证方式：

#### 1. JWT Token 认证（推荐）

登录成功后使用 Bearer Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**登录接口：**

```bash
curl -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

**注册接口：**

```bash
curl -X POST http://localhost:9090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "123456",
    "nickname": "新用户"
  }'
```

#### 2. OAuth2 单点登录

支持 GitHub、微信等第三方登录。访问 `/oauth2/authorization/github` 开始 GitHub 登录流程。

#### 3. Header 注入（向后兼容）

旧版开发环境仍支持 Header 注入：

```
X-User-Id: 1
X-User-Role: ADMIN    # or STUDENT
```

### 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误返回

```json
{
  "code": 400,
  "message": "参数校验失败: 分类不能为空",
  "data": null
}
```

## 核心功能演示

### 1. AI 智能出题

```bash
curl -X POST http://localhost:8080/api/admin/questions/generate \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: ADMIN" \
  -d '{
    "category": "Java编程",
    "difficulty": "MEDIUM",
    "count": 3,
    "knowledgePoint": "集合框架"
  }'
```

### 2. 开始考试 & 提交答卷

```bash
# 开始考试
curl -X POST http://localhost:8080/api/student/exams/start/1 \
  -H "X-User-Id: 2" \
  -H "X-User-Role: STUDENT"

# 提交答卷 (假设 examRecordId=1)
curl -X POST http://localhost:8080/api/student/exams/submit \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -H "X-User-Role: STUDENT" \
  -d '{
    "examRecordId": 1,
    "answers": "{\"1\":\"A\",\"2\":\"A,C\",\"3\":\"true\"}",
    "cutScreenCount": 0,
    "clipboardCount": 0
  }'
```

### 3. 获取 AI 判卷报告

```bash
curl http://localhost:8080/api/student/exams/1/report \
  -H "X-User-Id: 2" \
  -H "X-User-Role: STUDENT"
```

## MiMo AI 集成

| 场景 | Thinking | Temperature | response_format |
|------|----------|-------------|-----------------|
| 智能出题 | disabled | 0.7 | json_object |
| 智能判卷 | enabled | 0.2 | json_object |

### 429 退避策略

| 尝试 | 延迟 |
|------|------|
| 1 | 1s |
| 2 | 2s |
| 3 | 4s |
| 4 | 8s |

## 防作弊系统

### 检测维度

| 维度 | 阈值 | 触发行为 |
|------|------|----------|
| 切屏次数 | ≥2 次 | 警告标记 |
| 切屏次数 | ≥5 次 | **强制提交** |
| 剪贴板异常 | ≥3 次 | 标记可疑 |
| 心跳超时 | >120s 无心跳 | 记录异常 |
| 答题速度 | 客观题平均 <5s/题 | 标记 FAST_ANSWER |
| 多设备切换 | IP 变化 | **强制提交** |

### 风险等级

- **LOW**: 切屏 <2 且无其他异常 → 正常评分
- **MEDIUM**: 切屏 2-4 次或剪贴板 ≥3 次 → 标记可疑，正常评分
- **HIGH**: 切屏 ≥5 次或严重违规 → 强制提交，成绩单标记

### 前端集成要点

```js
// 1. 切屏监听
document.addEventListener('visibilitychange', () => {
  if (document.hidden) cutScreenCount++;
});

// 2. 心跳保活（每30秒）
setInterval(() => fetch(`/api/student/exams/heartbeat/${examId}`), 30000);

// 3. 提交时携带防作弊数据
fetch('/api/student/exams/submit', {
  body: JSON.stringify({ examRecordId, answers, cutScreenCount, clipboardCount })
});
```

## Docker 部署

```bash
docker compose up -d --build
```

## 许可证

MIT
