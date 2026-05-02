-- Smart Learn Platform Schema
-- MySQL 8.4

CREATE DATABASE IF NOT EXISTS smart_learn
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_learn;

-- ==========================================
-- Users (简化版, Header拦截器注入身份)
-- ==========================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL,
    nickname        VARCHAR(64)  NOT NULL,
    email           VARCHAR(128) DEFAULT NULL,
    avatar          VARCHAR(256) DEFAULT NULL,
    role            VARCHAR(32)  NOT NULL DEFAULT 'STUDENT' COMMENT 'ADMIN / STUDENT',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- OAuth2 User Bindings (单点登录绑定)
-- ==========================================
CREATE TABLE IF NOT EXISTS oauth2_user_binding (
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
-- Questions (智能题库)
-- ==========================================
CREATE TABLE IF NOT EXISTS questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(32)  NOT NULL COMMENT 'SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE/SHORT_ANSWER/CODING',
    category        VARCHAR(64)  NOT NULL COMMENT '分类标签',
    stem            TEXT         NOT NULL COMMENT '题干',
    options         JSON         DEFAULT NULL COMMENT '选项 {"A":"...","B":"..."}',
    answer          TEXT         NOT NULL COMMENT '标准答案',
    analysis        TEXT         DEFAULT NULL COMMENT '解析',
    difficulty      VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT 'EASY/MEDIUM/HARD',
    knowledge_point VARCHAR(256) DEFAULT NULL COMMENT '知识点',
    created_by      BIGINT       DEFAULT NULL COMMENT '创建者ID (AI生成的为NULL)',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_category (category),
    INDEX idx_difficulty (difficulty),
    INDEX idx_type (type),
    FULLTEXT idx_stem (stem) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Papers (试卷)
-- ==========================================
CREATE TABLE IF NOT EXISTS papers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    description     TEXT         DEFAULT NULL,
    total_score     INT          NOT NULL DEFAULT 100,
    duration_minutes INT         NOT NULL DEFAULT 60,
    question_ids    JSON         NOT NULL COMMENT '题目ID列表 [1,2,3,...]',
    created_by      BIGINT       NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Exam Records (考试记录)
-- ==========================================
CREATE TABLE IF NOT EXISTS exam_records (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    paper_id            BIGINT       NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/SUBMITTED/GRADED/FORCE_SUBMITTED',
    score               INT          DEFAULT NULL,
    total_score         INT          NOT NULL,
    start_time          DATETIME     NOT NULL,
    submit_time         DATETIME     DEFAULT NULL,
    duration_seconds    INT          DEFAULT NULL COMMENT '实际用时(秒)',
    cut_screen_count    INT          NOT NULL DEFAULT 0 COMMENT '切屏次数',
    clipboard_count     INT          NOT NULL DEFAULT 0 COMMENT '剪贴板异常次数',
    last_heartbeat      DATETIME     DEFAULT NULL COMMENT '最后心跳时间',
    ip_address          VARCHAR(45)  DEFAULT NULL COMMENT '考试IP地址',
    suspicious_flags    JSON         DEFAULT NULL COMMENT '可疑行为标记 ["FAST_ANSWER","MULTI_IP","CUT_SCREEN"]',
    answers             JSON         NOT NULL COMMENT '学生作答 {"1":"A","2":"B,C",...}',
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
-- Videos (视频)
-- ==========================================
CREATE TABLE IF NOT EXISTS videos (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(128) NOT NULL,
    description         TEXT         DEFAULT NULL,
    cover_url           VARCHAR(256) DEFAULT NULL,
    video_url           VARCHAR(256) NOT NULL,
    duration_seconds    INT          NOT NULL DEFAULT 0,
    duration_display    VARCHAR(8)   DEFAULT NULL COMMENT 'mm:ss 格式',
    is_official         TINYINT      NOT NULL DEFAULT 0 COMMENT '1=官方 0=用户',
    status              VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
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
-- Banners (轮播图)
-- ==========================================
CREATE TABLE IF NOT EXISTS banners (
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
-- Categories (分类管理)
-- ==========================================
CREATE TABLE IF NOT EXISTS categories (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL UNIQUE,
    parent_id       BIGINT       DEFAULT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Wrong Questions (错题本)
-- ==========================================
CREATE TABLE IF NOT EXISTS wrong_questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    question_id     BIGINT       NOT NULL,
    exam_record_id  BIGINT       DEFAULT NULL COMMENT '关联的考试记录',
    student_answer  TEXT         DEFAULT NULL COMMENT '学生的错误答案',
    correct_answer  TEXT         NOT NULL COMMENT '标准答案',
    difficulty      VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT '题目难度',
    category        VARCHAR(64)  DEFAULT NULL COMMENT '分类',
    knowledge_point VARCHAR(256) DEFAULT NULL COMMENT '知识点',
    wrong_count     INT          NOT NULL DEFAULT 1 COMMENT '累计错误次数',
    mastered        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已掌握 0=否 1=是',
    last_wrong_time DATETIME     DEFAULT NULL COMMENT '最近一次错误时间',
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
-- AI Usage Logs (AI调用统计)
-- ==========================================
CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       DEFAULT NULL COMMENT '触发用户ID',
    model           VARCHAR(64)  NOT NULL COMMENT 'AI模型',
    purpose         VARCHAR(32)  NOT NULL COMMENT '用途: QUESTION/GRADING/IMPORT/TUTOR',
    prompt_tokens   INT          NOT NULL DEFAULT 0,
    completion_tokens INT        NOT NULL DEFAULT 0,
    total_tokens    INT          NOT NULL DEFAULT 0,
    duration_ms     INT          NOT NULL DEFAULT 0 COMMENT '请求耗时(毫秒)',
    status          VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    error_message   TEXT         DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_purpose (purpose),
    INDEX idx_created (created_at),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================
-- Seed data
-- ==========================================
INSERT INTO categories (name, sort_order) VALUES
('Java编程', 1),
('数据结构', 2),
('操作系统', 3),
('计算机网络', 4),
('数据库', 5)
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ==========================================
-- Seed: Users
-- ==========================================
INSERT INTO users (id, username, password, nickname, email, role) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@smartlearn.com', 'ADMIN'),
(2, 'student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'student1@smartlearn.com', 'STUDENT'),
(3, 'student2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'student2@smartlearn.com', 'STUDENT'),
(4, 'student3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'student3@smartlearn.com', 'STUDENT'),
(5, 'student4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', 'student4@smartlearn.com', 'STUDENT')
    ON DUPLICATE KEY UPDATE username=VALUES(username);

-- ==========================================
-- Seed: Questions
-- ==========================================
INSERT INTO questions (type, category, stem, options, answer, analysis, difficulty, knowledge_point) VALUES
('SINGLE_CHOICE', 'Java编程',
 'Java中HashMap的底层数据结构是什么？',
 '{"A":"数组","B":"链表","C":"数组+链表","D":"数组+链表+红黑树"}',
 'D',
 'JDK 8中HashMap采用数组+链表+红黑树结构，链表长度超过8且数组长度>=64时转换为红黑树，优化查询性能。',
 'MEDIUM', 'HashMap底层结构'),

('SINGLE_CHOICE', 'Java编程',
 '以下哪个关键字用于定义不可变变量？',
 '{"A":"static","B":"final","C":"const","D":"volatile"}',
 'B',
 'final修饰的变量只能赋值一次，赋值后不可修改，用于定义不可变变量。Java中没有const关键字。',
 'EASY', 'final关键字'),

('MULTIPLE_CHOICE', 'Java编程',
 '以下哪些是Java集合框架的接口？',
 '{"A":"List","B":"ArrayList","C":"Set","D":"HashMap","E":"Map"}',
 'A,C,E',
 'List、Set、Map是集合框架的核心接口，ArrayList和HashMap是接口的具体实现类。',
 'MEDIUM', '集合框架接口'),

('TRUE_FALSE', '数据结构',
 '二叉树的前序遍历第一个访问的是根节点。',
 NULL,
 'true',
 '前序遍历顺序为：根→左→右，第一个访问的确实是根节点。',
 'EASY', '二叉树遍历'),

('SINGLE_CHOICE', '数据结构',
 '时间复杂度为O(n log n)的排序算法是？',
 '{"A":"冒泡排序","B":"快速排序","C":"插入排序","D":"选择排序"}',
 'B',
 '快速排序平均时间复杂度为O(n log n)。冒泡、插入、选择排序均为O(n²)。',
 'MEDIUM', '排序算法复杂度'),

('SHORT_ANSWER', '操作系统',
 '什么是死锁？产生死锁的四个必要条件是什么？',
 NULL,
 '死锁是指两个或多个进程因争夺资源而造成的互相等待现象。四个必要条件：1.互斥条件 2.请求与保持 3.不剥夺条件 4.循环等待条件。',
 '死锁是多个进程互相等待对方持有资源的情况。必须同时满足四个条件：互斥、请求保持、不可剥夺、循环等待。',
 'HARD', '死锁'),

('TRUE_FALSE', '计算机网络',
 'HTTP协议是无状态的。',
 NULL,
 'true',
 'HTTP本身是无状态协议，每次请求都是独立的。通过Cookie/Session机制可以实现状态保持。',
 'EASY', 'HTTP协议'),

('SINGLE_CHOICE', '数据库',
 'SQL中用于删除表中所有数据但保留表结构的语句是？',
 '{"A":"DROP TABLE","B":"DELETE FROM table","C":"TRUNCATE TABLE","D":"REMOVE ALL"}',
 'C',
 'TRUNCATE TABLE删除所有数据但保留表结构，比DELETE更高效。DROP TABLE会删除整个表。',
 'MEDIUM', 'SQL语句'),

('SHORT_ANSWER', '数据库',
 '解释事务的ACID特性。',
 NULL,
 'ACID指：原子性(Atomicity)-事务要么全部成功要么全部失败；一致性(Consistency)-事务前后数据保持一致；隔离性(Isolation)-事务之间互不干扰；持久性(Durability)-事务提交后永久保存。',
 'ACID四大特性：原子性、一致性、隔离性、持久性。这是关系型数据库事务的核心保障。',
 'HARD', '事务特性'),

('SINGLE_CHOICE', '计算机网络',
 'TCP三次握手的第二次握手发送的标志位是？',
 '{"A":"SYN","B":"ACK","C":"SYN+ACK","D":"FIN"}',
 'C',
 '第二次握手服务端返回SYN+ACK，确认客户端的SYN请求并同时发送自己的SYN。',
 'MEDIUM', 'TCP握手');

-- ==========================================
-- Seed: Paper (Java基础测试卷)
-- ==========================================
INSERT INTO papers (title, description, total_score, duration_minutes, question_ids, created_by) VALUES
('Java基础测试', '考察Java编程基础、集合、异常等知识点', 100, 60, '[1,2,3]', 1),
('计算机综合测试', '涵盖数据结构、操作系统、网络、数据库', 100, 45, '[4,5,6,7,8,9,10]', 1);

