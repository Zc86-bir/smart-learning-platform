-- 添加练习功能相关表
USE smart_learn;

CREATE TABLE IF NOT EXISTS practice_sessions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    category            VARCHAR(64)  DEFAULT NULL COMMENT '分类',
    difficulty          VARCHAR(16)  DEFAULT NULL COMMENT 'EASY/MEDIUM/HARD',
    question_count      INT          NOT NULL DEFAULT 0,
    question_ids        JSON         NOT NULL COMMENT '题目ID列表',
    answers             JSON         NOT NULL COMMENT '作答记录',
    correct_count       INT          NOT NULL DEFAULT 0,
    accuracy            INT          DEFAULT NULL COMMENT '正确率 0-100',
    status              VARCHAR(16)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/FINISHED',
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

-- 添加练习权限
INSERT INTO sys_permission (id, parent_id, name, code, type, path, api_path, method, sort, status) VALUES
(36, 4, '题库练习', 'practice:take', 'button', NULL, '/api/student/practice', 'POST', 4, 1),
(37, 4, '查看练习报告', 'practice:report', 'button', NULL, '/api/student/practice/*/report', 'GET', 5, 1)
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- STUDENT 角色添加练习权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(4, 36), (4, 37);
