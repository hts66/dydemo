package com.example.dyhouduan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long workId;

    private String content;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String avatar;
}
