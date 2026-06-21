package com.example.dyhouduan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("likes")
public class Like {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long workId;

    private LocalDateTime createdAt;
}
