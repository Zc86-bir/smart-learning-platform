package com.smartlearn.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlearn.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT u.*,
               COUNT(DISTINCT e.id) as examCount,
               COALESCE(SUM(CASE WHEN e.status IN ('SUBMITTED','GRADED') THEN 1 ELSE 0 END), 0) as completedExams,
               COALESCE(MAX(e.submit_time), u.created_at) as lastActiveTime
        FROM users u
        LEFT JOIN exam_records e ON u.id = e.user_id
        WHERE u.role = 'STUDENT' AND u.deleted = 0
        GROUP BY u.id
        ORDER BY u.created_at DESC
        """)
    List<java.util.Map<String, Object>> findStudentsWithStats();

    @Select("SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND deleted = 0")
    long countStudents();
}
