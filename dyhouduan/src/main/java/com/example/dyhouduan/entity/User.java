package com.example.dyhouduan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;

    private String password;

    private String username;

    @TableField("gender")
    private Integer gender;

    private String bio;

    private String avatar;

    private String background;

    private Integer followersCount;

    private Integer followingCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
