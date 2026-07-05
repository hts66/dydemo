package com.example.dyhouduan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dyhouduan.entity.Message;
import com.example.dyhouduan.mapper.MessageMapper;
import com.example.dyhouduan.service.MessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public List<Message> getChatMessages(Long userId, Long otherUserId) {
        return baseMapper.selectChatMessages(userId, otherUserId);
    }

    @Override
    public Message sendMessage(Long senderId, Long receiverId, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        save(message);
        return baseMapper.selectById(message.getId());
    }
}