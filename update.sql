USE dy;

-- 添加播放量字段
ALTER TABLE works ADD COLUMN views INT DEFAULT 0;

-- 创建观看历史表
CREATE TABLE watch_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    watch_duration INT DEFAULT 0 COMMENT '观看时长（秒）',
    is_complete BOOLEAN DEFAULT FALSE COMMENT '是否完播',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_work_id (work_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (work_id) REFERENCES works(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观看历史表';
