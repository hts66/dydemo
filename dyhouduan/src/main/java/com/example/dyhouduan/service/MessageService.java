package com.example.dyhouduan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dyhouduan.entity.Message;

import java.util.List;

public interface MessageService extends IService<Message> {

    List<Message> getChatMessages(Long userId, Long otherUserId);

    Message sendMessage(Long senderId, Long receiverId, String content);

    Message saveBotMessage(Long userId, String content);

    /**
     * 查询单条消息详情（联表带出发送/接收方的用户名和头像），用于 WebSocket 推送
     */
    Message getMessageDetail(Long id);
}