-- V2: Seed data - roles, permissions, users, dictionaries, sample questions

-- ==========================================
-- RBAC Roles
-- ==========================================
INSERT INTO sys_role (id, name, code, description, sort, status) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1, 1),
(2, '系统管理员', 'ADMIN', '管理题库、试卷、用户等', 2, 1),
(3, '教师', 'TEACHER', '出题、组卷、判卷', 3, 1),
(4, '学生', 'STUDENT', '学习、考试、查看错题', 4, 1)
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ==========================================
-- RBAC Permissions
-- ==========================================
INSERT INTO sys_permission (id, parent_id, name, code, type, path, api_path, method, sort, status) VALUES
(1, 0, '仪表盘', 'dashboard', 'menu', '/dashboard', NULL, NULL, 1, 1),
(2, 0, '题库管理', 'question', 'menu', '/questions', NULL, NULL, 2, 1),
(3, 0, '试卷管理', 'paper', 'menu', '/papers', NULL, NULL, 3, 1),
(4, 0, '考试管理', 'exam', 'menu', '/exams', NULL, NULL, 4, 1),
(5, 0, '视频管理', 'video', 'menu', '/videos', NULL, NULL, 5, 1),
(6, 0, '用户管理', 'user', 'menu', '/users', NULL, NULL, 6, 1),
(7, 0, '系统设置', 'system', 'menu', '/system', NULL, NULL, 7, 1),
(8, 0, '错题本', 'wrong-question', 'menu', '/wrong-questions', NULL, NULL, 8, 1),
(9, 0, 'AI助教', 'ai-tutor', 'menu', '/ai-tutor', NULL, NULL, 9, 1),
(10, 2, '查看题目', 'question:view', 'button', NULL, '/api/admin/questions', 'GET', 1, 1),
(11, 2, '新增题目', 'question:create', 'button', NULL, '/api/admin/questions', 'POST', 2, 1),
(12, 2, '编辑题目', 'question:update', 'button', NULL, '/api/admin/questions/*', 'PUT', 3, 1),
(13, 2, '删除题目', 'question:delete', 'button', NULL, '/api/admin/questions/*', 'DELETE', 4, 1),
(14, 2, 'AI出题', 'question:ai-generate', 'button', NULL, '/api/admin/questions/generate', 'POST', 5, 1),
(15, 2, '批量导入', 'question:import', 'button', NULL, '/api/admin/questions/import', 'POST', 6, 1),
(16, 3, '查看试卷', 'paper:view', 'button', NULL, '/api/admin/papers', 'GET', 1, 1),
(17, 3, '创建试卷', 'paper:create', 'button', NULL, '/api/admin/papers', 'POST', 2, 1),
(18, 3, '编辑试卷', 'paper:update', 'button', NULL, '/api/admin/papers/*', 'PUT', 3, 1),
(19, 3, '删除试卷', 'paper:delete', 'button', NULL, '/api/admin/papers/*', 'DELETE', 4, 1),
(20, 5, '查看视频', 'video:view', 'button', NULL, '/api/admin/videos', 'GET', 1, 1),
(21, 5, '审核视频', 'video:review', 'button', NULL, '/api/admin/videos/*/review', 'PUT', 2, 1),
(22, 5, '删除视频', 'video:delete', 'button', NULL, '/api/admin/videos/*', 'DELETE', 3, 1),
(23, 6, '查看用户', 'user:view', 'button', NULL, '/api/admin/users', 'GET', 1, 1),
(24, 6, '编辑用户', 'user:update', 'button', NULL, '/api/admin/users/*', 'PUT', 2, 1),
(25, 6, '禁用用户', 'user:disable', 'button', NULL, '/api/admin/users/*/status', 'PUT', 3, 1),
(26, 6, '角色分配', 'user:role-assign', 'button', NULL, '/api/admin/users/*/roles', 'POST', 4, 1),
(27, 7, '轮播图管理', 'banner:manage', 'button', NULL, '/api/admin/banners', 'GET', 1, 1),
(28, 7, '分类管理', 'category:manage', 'button', NULL, '/api/admin/categories', 'GET', 2, 1),
(29, 7, 'AI用量统计', 'ai-usage:view', 'button', NULL, '/api/admin/ai-usage', 'GET', 3, 1),
(30, 4, '参加考试', 'exam:take', 'button', NULL, '/api/student/exams', 'GET', 1, 1),
(31, 4, '提交答卷', 'exam:submit', 'button', NULL, '/api/student/exams/submit', 'POST', 2, 1),
(32, 4, '查看成绩', 'exam:score-view', 'button', NULL, '/api/student/exams/*/report', 'GET', 3, 1),
(33, 8, '查看错题', 'wrong-question:view', 'button', NULL, '/api/student/wrong-questions', 'GET', 1, 1),
(34, 8, '错题练习', 'wrong-question:practice', 'button', NULL, '/api/student/wrong-questions/practice', 'GET', 2, 1),
(35, 9, 'AI答疑', 'ai-tutor:ask', 'button', NULL, '/api/student/ai-tutor', 'POST', 1, 1),
(36, 4, '题库练习', 'practice:take', 'button', NULL, '/api/student/practice', 'POST', 4, 1),
(37, 4, '查看练习报告', 'practice:report', 'button', NULL, '/api/student/practice/*/report', 'GET', 5, 1),
(38, 7, '数据字典', 'dict:manage', 'button', NULL, '/api/admin/dict', 'GET', 4, 1)
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ==========================================
-- Users
-- ==========================================
INSERT INTO users (id, username, password, nickname, email, role) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@smartlearn.com', 'ADMIN'),
(2, 'student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'student1@smartlearn.com', 'STUDENT'),
(3, 'student2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'student2@smartlearn.com', 'STUDENT'),
(4, 'student3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'student3@smartlearn.com', 'STUDENT'),
(5, 'student4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', 'student4@smartlearn.com', 'STUDENT')
    ON DUPLICATE KEY UPDATE username=VALUES(username);

-- ==========================================
-- User-Role Bindings
-- ==========================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (1, 2),
(2, 4), (3, 4), (4, 4), (5, 4)
    ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- ==========================================
-- Role-Permission Bindings
-- ==========================================
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0
    ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7),
(2, 10), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15),
(2, 16), (2, 17), (2, 18), (2, 19),
(2, 20), (2, 21), (2, 22),
(2, 23), (2, 24), (2, 25),
(2, 27), (2, 28), (2, 29), (2, 38)
    ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 8), (3, 9),
(3, 10), (3, 11), (3, 12), (3, 14), (3, 15),
(3, 16), (3, 17), (3, 18),
(3, 20),
(3, 33), (3, 34), (3, 35)
    ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(4, 1), (4, 4), (4, 5), (4, 8), (4, 9),
(4, 30), (4, 31), (4, 32),
(4, 33), (4, 34),
(4, 35), (4, 36), (4, 37)
    ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- ==========================================
-- Categories
-- ==========================================
INSERT INTO categories (name, sort_order) VALUES
('Java编程', 1),
('数据结构', 2),
('操作系统', 3),
('计算机网络', 4),
('数据库', 5)
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ==========================================
-- Dictionary Types
-- ==========================================
INSERT INTO sys_dict_type (id, name, code, status, remark) VALUES
(1, '视频状态', 'video_status', 1, '视频审核状态'),
(2, '题目难度', 'difficulty', 1, '题目难度等级'),
(3, '题目类型', 'question_type', 1, '题目类型枚举'),
(4, '用户状态', 'user_status', 1, '用户账号状态'),
(5, '考试状态', 'exam_status', 1, '考试记录状态'),
(6, '审核状态', 'review_status', 1, '通用审核状态')
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ==========================================
-- Dictionary Data
-- ==========================================
INSERT INTO sys_dict_data (dict_type_id, label, value, sort, status, color, remark) VALUES
(1, '待审核', 'PENDING', 1, 1, 'orange', '等待管理员审核'),
(1, '已通过', 'APPROVED', 2, 1, 'green', '审核通过可播放'),
(1, '已拒绝', 'REJECTED', 3, 1, 'red', '审核被拒绝'),
(2, '简单', 'EASY', 1, 1, 'green', '基础难度'),
(2, '中等', 'MEDIUM', 2, 1, 'orange', '中等难度'),
(2, '困难', 'HARD', 3, 1, 'red', '高难度'),
(3, '单选题', 'SINGLE_CHOICE', 1, 1, null, null),
(3, '多选题', 'MULTIPLE_CHOICE', 2, 1, null, null),
(3, '判断题', 'TRUE_FALSE', 3, 1, null, null),
(3, '简答题', 'SHORT_ANSWER', 4, 1, null, null),
(3, '编程题', 'CODING', 5, 1, null, null),
(4, '启用', '1', 1, 1, 'green', null),
(4, '禁用', '0', 2, 1, 'red', null),
(5, '考试中', 'IN_PROGRESS', 1, 1, 'blue', null),
(5, '已提交', 'SUBMITTED', 2, 1, 'orange', null),
(5, '已批阅', 'GRADED', 3, 1, 'green', null),
(5, '强制提交', 'FORCE_SUBMITTED', 4, 1, 'red', null),
(6, '待审核', 'PENDING', 1, 1, 'orange', null),
(6, '通过', 'APPROVED', 2, 1, 'green', null),
(6, '拒绝', 'REJECTED', 3, 1, 'red', null)
    ON DUPLICATE KEY UPDATE label=VALUES(label);

-- ==========================================
-- Sample Questions
-- ==========================================
INSERT INTO questions (type, category, stem, options, answer, analysis, difficulty, knowledge_point) VALUES
('SINGLE_CHOICE', 'Java编程',
 'Java中HashMap的底层数据结构是什么？',
 '{"A":"数组","B":"链表","C":"数组+链表","D":"数组+链表+红黑树"}',
 'D',
 'JDK 8中HashMap采用数组+链表+红黑树结构，链表长度超过8且数组长度>=64时转换为红黑树。',
 'MEDIUM', 'HashMap底层结构'),

('SINGLE_CHOICE', 'Java编程',
 '以下哪个关键字用于定义不可变变量？',
 '{"A":"static","B":"final","C":"const","D":"volatile"}',
 'B',
 'final修饰的变量只能赋值一次。Java中没有const关键字。',
 'EASY', 'final关键字'),

('MULTIPLE_CHOICE', 'Java编程',
 '以下哪些是Java集合框架的接口？',
 '{"A":"List","B":"ArrayList","C":"Set","D":"HashMap","E":"Map"}',
 'A,C,E',
 'List、Set、Map是接口，ArrayList和HashMap是具体实现。',
 'MEDIUM', '集合框架接口'),

('TRUE_FALSE', '数据结构',
 '二叉树的前序遍历第一个访问的是根节点。',
 NULL,
 'true',
 '前序遍历顺序为：根→左→右。',
 'EASY', '二叉树遍历'),

('SINGLE_CHOICE', '数据结构',
 '时间复杂度为O(n log n)的排序算法是？',
 '{"A":"冒泡排序","B":"快速排序","C":"插入排序","D":"选择排序"}',
 'B',
 '快速排序平均时间复杂度为O(n log n)。',
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
 'HTTP本身是无状态协议，每次请求都是独立的。',
 'EASY', 'HTTP协议'),

('SINGLE_CHOICE', '数据库',
 'SQL中用于删除表中所有数据但保留表结构的语句是？',
 '{"A":"DROP TABLE","B":"DELETE FROM table","C":"TRUNCATE TABLE","D":"REMOVE ALL"}',
 'C',
 'TRUNCATE TABLE删除所有数据但保留表结构，比DELETE更高效。',
 'MEDIUM', 'SQL语句'),

('SHORT_ANSWER', '数据库',
 '解释事务的ACID特性。',
 NULL,
 'ACID指：原子性(Atomicity)-事务要么全部成功要么全部失败；一致性(Consistency)-事务前后数据保持一致；隔离性(Isolation)-事务之间互不干扰；持久性(Durability)-事务提交后永久保存。',
 'ACID四大特性：原子性、一致性、隔离性、持久性。',
 'HARD', '事务特性'),

('SINGLE_CHOICE', '计算机网络',
 'TCP三次握手的第二次握手发送的标志位是？',
 '{"A":"SYN","B":"ACK","C":"SYN+ACK","D":"FIN"}',
 'C',
 '第二次握手服务端返回SYN+ACK。',
 'MEDIUM', 'TCP握手');

-- ==========================================
-- Sample Papers
-- ==========================================
INSERT INTO papers (title, description, total_score, duration_minutes, question_ids, created_by) VALUES
('Java基础测试', '考察Java编程基础、集合、异常等知识点', 100, 60, '[1,2,3]', 1),
('计算机综合测试', '涵盖数据结构、操作系统、网络、数据库', 100, 45, '[4,5,6,7,8,9,10]', 1);
