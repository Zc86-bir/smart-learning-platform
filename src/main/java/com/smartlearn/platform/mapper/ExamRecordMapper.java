package com.smartlearn.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlearn.platform.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Select("SELECT COUNT(*) FROM exam_records")
    long countTotalExams();

    @Select("SELECT COUNT(*) FROM exam_records WHERE status IN ('SUBMITTED','GRADED')")
    long countCompletedExams();

    @Select("""
        SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as date, COUNT(*) as cnt
        FROM exam_records WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
        GROUP BY date ORDER BY date ASC
        """)
    List<Map<String, Object>> examTrendLast30Days();

    @Select("""
        SELECT p.title as title, COUNT(e.id) as examCount,
               COALESCE(AVG(e.score * 100.0 / e.total_score), 0) as avgScoreRate,
               COALESCE(SUM(CASE WHEN e.score * 100.0 / e.total_score >= 60 THEN 1 ELSE 0 END), 0) as passCount,
               COUNT(e.id) as totalCount
        FROM papers p
        LEFT JOIN exam_records e ON p.id = e.paper_id AND e.status IN ('SUBMITTED','GRADED')
        WHERE p.deleted = 0
        GROUP BY p.id, p.title
        ORDER BY examCount DESC
        LIMIT 10
        """)
    List<Map<String, Object>> topPapers();

    @Select("""
        SELECT DATE_FORMAT(start_time, '%Y-%m') as month,
               COUNT(*) as total,
               COALESCE(AVG(score * 100.0 / total_score), 0) as avgRate
        FROM exam_records WHERE status IN ('SUBMITTED','GRADED')
        GROUP BY month ORDER BY month DESC LIMIT 6
        """)
    List<Map<String, Object>> monthlyExamStats();
}
