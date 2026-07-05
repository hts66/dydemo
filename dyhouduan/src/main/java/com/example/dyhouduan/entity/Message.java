package com.example.dyhouduan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long senderId;

    private Long receiverId;

    private String content;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String senderUsername;

    @TableField(exist = false)
    private String senderAvatar;

    @TableField(exist = false)
    private String receiverUsername;

    @TableField(exist = false)
    private String receiverAvatar;
}