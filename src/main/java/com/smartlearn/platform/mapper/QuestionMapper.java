package com.smartlearn.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlearn.platform.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT category, COUNT(*) as cnt FROM questions WHERE deleted = 0 GROUP BY category")
    @Results({@Result(property = "cnt", column = "cnt")})
    List<Map<String, Object>> countByCategory();
}
