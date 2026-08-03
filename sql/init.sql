-- 短视频平台初始化数据库脚本
-- 合并自 biao.sql、update.sql、V1__add_background_column.sql
-- 并补充代码实际使用的 messages 表与关注/粉丝统计字段
-- 适用于全新 MySQL 数据库；执行前请确认数据已备份

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS dy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dy;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱，用于登录',
    password VARCHAR(255) NOT NULL COMMENT '密码，存储加密后的值',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    background VARCHAR(255) DEFAULT '#000000' COMMENT '个人主页背景颜色',
    followers_count INT NOT NULL DEFAULT 0 COMMENT '粉丝数',
    following_count INT NOT NULL DEFAULT 0 COMMENT '关注数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 作品表
CREATE TABLE IF NOT EXISTS works (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '类型：1-图片，2-视频',
    url VARCHAR(500) NOT NULL COMMENT '文件路径/URL',
    thumbnail VARCHAR(500) DEFAULT NULL COMMENT '封面图（视频用）',
    title VARCHAR(100) DEFAULT NULL COMMENT '作品标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '作品描述',
    likes_count INT DEFAULT 0 COMMENT '点赞数',
    comments_count INT DEFAULT 0 COMMENT '评论数',
    views INT DEFAULT 0 COMMENT '播放量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品表';

-- 3. 点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_user_work (user_id, work_id),
    INDEX idx_user_id (user_id),
    INDEX idx_work_id (work_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (work_id) REFERENCES works(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 4. 关注表
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    following_id BIGINT NOT NULL COMMENT '被关注者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    INDEX idx_follower_id (follower_id),
    INDEX idx_following_id (following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    CHECK (follower_id != following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

-- 5. 评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_work_id (work_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (work_id) REFERENCES works(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 6. 观看历史表
CREATE TABLE IF NOT EXISTS watch_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    work_id BIGINT NOT NULL COMMENT '作品ID',
    watch_duration INT DEFAULT 0 COMMENT '观看时长（秒）',
    is_complete BOOLEAN DEFAULT FALSE COMMENT '是否完整播放',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_work_id (work_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (work_id) REFERENCES works(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观看历史表';

-- 7. 私信与 AI 机器人消息表
-- 不设置用户外键：AI 机器人使用固定 ID -1，允许 sender_id / receiver_id 为 -1
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID，AI 机器人为 -1',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID，AI 机器人为 -1',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信与AI机器人消息表';
