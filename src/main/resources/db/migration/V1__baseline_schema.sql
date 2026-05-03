-- V1: Baseline schema - all tables for smart_learn_platform
-- This replaces the legacy schema.sql for versioned migrations

-- ==========================================
-- Users
-- ==========================================
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL,
    nickname        VARCHAR(64)  NOT NULL,
    email           VARCHAR(128) DEFAULT NULL,
    avatar          VARCHAR(256) DEFAULT NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    role            VARCHAR(32)  NOT NULL DEFAULT 'STUDENT' COMMENT 'ADMIN / STUDENT',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- RBAC: Roles
-- ==========================================
CREATE TABLE sys_role (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL COMMENT '角色名称',
    code            VARCHAR(64)  NOT NULL UNIQUE COMMENT '角色编码',
    description     VARCHAR(256) DEFAULT NULL,
    sort            INT          NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- RBAC: Permissions
-- ==========================================
CREATE TABLE sys_permission (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id       BIGINT       DEFAULT 0 COMMENT '父级ID, 0为顶级',
    name            VARCHAR(64)  NOT NULL COMMENT '权限名称',
    code            VARCHAR(128) NOT NULL UNIQUE COMMENT '权限编码',
    type            VARCHAR(16)  NOT NULL DEFAULT 'menu' COMMENT 'menu/button/api',
    path            VARCHAR(256) DEFAULT NULL COMMENT '前端路由',
    api_path        VARCHAR(256) DEFAULT NULL COMMENT 'API路径',
    method          VARCHAR(8)   DEFAULT NULL COMMENT 'GET/POST/PUT/DELETE',
    sort            INT          NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- RBAC: User-Role
-- ==========================================
CREATE TABLE sys_user_role (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    role_id         BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- RBAC: Role-Permission
-- ==========================================
CREATE TABLE sys_role_permission (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id         BIGINT       NOT NULL,
    permission_id   BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- OAuth2 User Bindings
-- ==========================================
CREATE TABLE oauth2_user_binding (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    provider            VARCHAR(32)  NOT NULL COMMENT 'github/wechat/google',
    provider_user_id    VARCHAR(128) NOT NULL COMMENT '第三方平台的用户ID',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_provider_user (provider, provider_user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Questions
-- ==========================================
CREATE TABLE questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(32)  NOT NULL COMMENT 'SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/SHORT_ANSWER/CODING',
    category        VARCHAR(64)  NOT NULL COMMENT '分类标签',
    stem            TEXT         NOT NULL COMMENT '题干',
    options         JSON         DEFAULT NULL COMMENT '选项',
    answer          TEXT         NOT NULL COMMENT '标准答案',
    analysis        TEXT         DEFAULT NULL COMMENT '解析',
    difficulty      VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'EASY/MEDIUM/HARD',
    knowledge_point VARCHAR(256) DEFAULT NULL COMMENT '知识点',
    created_by      BIGINT       DEFAULT NULL COMMENT '创建者ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_category (category),
    INDEX idx_difficulty (difficulty),
    INDEX idx_type (type),
    FULLTEXT idx_stem (stem) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Papers
-- ==========================================
CREATE TABLE papers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    description     TEXT         DEFAULT NULL,
    total_score     INT          NOT NULL DEFAULT 100,
    duration_minutes INT         NOT NULL DEFAULT 60,
    question_ids    JSON         NOT NULL COMMENT '题目ID列表',
    created_by      BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Exam Records
-- ==========================================
CREATE TABLE exam_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    paper_id            BIGINT       NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS',
    score               INT          DEFAULT NULL,
    total_score         INT          NOT NULL,
    start_time          DATETIME     NOT NULL,
    submit_time         DATETIME     DEFAULT NULL,
    duration_seconds    INT          DEFAULT NULL COMMENT '实际用时(秒)',
    cut_screen_count    INT          NOT NULL DEFAULT 0 COMMENT '切屏次数',
    clipboard_count     INT          NOT NULL DEFAULT 0 COMMENT '剪贴板异常次数',
    last_heartbeat      DATETIME     DEFAULT NULL COMMENT '最后心跳时间',
    ip_address          VARCHAR(45)  DEFAULT NULL COMMENT '考试IP',
    suspicious_flags    JSON         DEFAULT NULL COMMENT '可疑行为标记',
    answers             JSON         NOT NULL COMMENT '学生作答',
    ai_report           JSON         DEFAULT NULL COMMENT 'AI判卷报告',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_paper_id (paper_id),
    INDEX idx_status (status),
    INDEX idx_score (score DESC),
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_paper (user_id, paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Videos
-- ==========================================
CREATE TABLE videos (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(128) NOT NULL,
    description         TEXT         DEFAULT NULL,
    cover_url           VARCHAR(256) DEFAULT NULL,
    video_url           VARCHAR(256) NOT NULL,
    duration_seconds    INT          NOT NULL DEFAULT 0,
    duration_display    VARCHAR(8)   DEFAULT NULL COMMENT 'mm:ss',
    is_official         TINYINT      NOT NULL DEFAULT 0 COMMENT '1=官方 0=用户',
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    category            VARCHAR(64)  DEFAULT NULL,
    uploader_id         BIGINT       DEFAULT NULL,
    reviewer_id         BIGINT       DEFAULT NULL,
    reviewed_at         DATETIME     DEFAULT NULL,
    review_comment      TEXT         DEFAULT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_is_official (is_official)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Banners
-- ==========================================
CREATE TABLE banners (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    image_url       VARCHAR(256) NOT NULL,
    link_url        VARCHAR(256) DEFAULT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    is_active       TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Categories
-- ==========================================
CREATE TABLE categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL UNIQUE,
    parent_id       BIGINT       DEFAULT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Wrong Questions
-- ==========================================
CREATE TABLE wrong_questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    question_id     BIGINT       NOT NULL,
    exam_record_id  BIGINT       DEFAULT NULL COMMENT '关联考试记录',
    student_answer  TEXT         DEFAULT NULL,
    correct_answer  TEXT         NOT NULL,
    difficulty      VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    category        VARCHAR(64)  DEFAULT NULL,
    knowledge_point VARCHAR(256) DEFAULT NULL,
    wrong_count     INT          NOT NULL DEFAULT 1,
    mastered        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=否 1=是',
    last_wrong_time DATETIME     DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_user_mastered (user_id, mastered),
    INDEX idx_knowledge_point (knowledge_point),
    INDEX idx_user_category (user_id, category),
    INDEX idx_user_knowledge (user_id, knowledge_point),
    UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- AI Usage Logs
-- ==========================================
CREATE TABLE ai_usage_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       DEFAULT NULL,
    model           VARCHAR(64)  NOT NULL,
    purpose         VARCHAR(32)  NOT NULL COMMENT 'QUESTION/GRADING/IMPORT/TUTOR',
    prompt_tokens   INT          NOT NULL DEFAULT 0,
    completion_tokens INT        NOT NULL DEFAULT 0,
    total_tokens    INT          NOT NULL DEFAULT 0,
    duration_ms     INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS',
    error_message   TEXT         DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purpose (purpose),
    INDEX idx_created (created_at),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Practice Sessions
-- ==========================================
CREATE TABLE practice_sessions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    category            VARCHAR(64)  DEFAULT NULL,
    difficulty          VARCHAR(16)  DEFAULT NULL,
    question_count      INT          NOT NULL DEFAULT 0,
    question_ids        JSON         NOT NULL,
    answers             JSON         NOT NULL,
    correct_count       INT          NOT NULL DEFAULT 0,
    accuracy            INT          DEFAULT NULL COMMENT '正确率 0-100',
    status              VARCHAR(16)  NOT NULL DEFAULT 'IN_PROGRESS',
    start_time          DATETIME     NOT NULL,
    finish_time         DATETIME     DEFAULT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Operation Audit Logs
-- ==========================================
CREATE TABLE sys_operation_log (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       DEFAULT NULL,
    username            VARCHAR(64)  DEFAULT NULL,
    module              VARCHAR(64)  NOT NULL,
    operation           VARCHAR(64)  NOT NULL,
    method              VARCHAR(128) NOT NULL,
    request_method      VARCHAR(8)   NOT NULL,
    request_url         VARCHAR(256) NOT NULL,
    request_params      TEXT         DEFAULT NULL,
    response_status     INT          NOT NULL DEFAULT 200,
    error_message       TEXT         DEFAULT NULL,
    ip_address          VARCHAR(45)  DEFAULT NULL,
    user_agent          VARCHAR(256) DEFAULT NULL,
    duration_ms         INT          NOT NULL DEFAULT 0,
    trace_id            VARCHAR(32)  DEFAULT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_operation (operation),
    INDEX idx_created (created_at),
    INDEX idx_user_module (user_id, module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- System Dictionary
-- ==========================================
CREATE TABLE sys_dict_type (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL COMMENT '字典名称',
    code            VARCHAR(64)  NOT NULL UNIQUE COMMENT '字典编码',
    status          TINYINT      NOT NULL DEFAULT 1,
    remark          VARCHAR(256) DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_dict_data (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type_id    BIGINT       NOT NULL,
    label           VARCHAR(64)  NOT NULL,
    value           VARCHAR(64)  NOT NULL,
    sort            INT          NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1,
    color           VARCHAR(16)  DEFAULT NULL,
    is_default      TINYINT      NOT NULL DEFAULT 0,
    remark          VARCHAR(256) DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_dict_type (dict_type_id),
    INDEX idx_value (value),
    UNIQUE KEY uk_dict_type_value (dict_type_id, value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Flyway metadata table
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL PRIMARY KEY,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
