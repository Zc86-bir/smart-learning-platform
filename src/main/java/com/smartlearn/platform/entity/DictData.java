package com.smartlearn.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dict_data")
public class DictData {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dictTypeId;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
    private String color;
    private Integer isDefault;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
