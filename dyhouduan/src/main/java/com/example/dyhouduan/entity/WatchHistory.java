package com.example.dyhouduan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("watch_history")
public class WatchHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long workId;

    private Integer watchDuration;

    private Boolean isComplete;

    private LocalDateTime createdAt;
}
