package com.example.dyhouduan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("works")
public class Work {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer type;

    private String url;

    private String thumbnail;

    private String title;

    private String description;

    private Integer likesCount;

    private Integer commentsCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
