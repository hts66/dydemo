package com.example.dyhouduan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dyhouduan.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT m.*, s.username as senderUsername, s.avatar as senderAvatar, " +
            "r.username as receiverUsername, r.avatar as receiverAvatar " +
            "FROM messages m " +
            "LEFT JOIN users s ON m.sender_id = s.id " +
            "LEFT JOIN users r ON m.receiver_id = r.id " +
            "WHERE (m.sender_id = #{userId} AND m.receiver_id = #{otherUserId}) " +
            "OR (m.sender_id = #{otherUserId} AND m.receiver_id = #{userId}) " +
            "ORDER BY m.created_at ASC")
    List<Message> selectChatMessages(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}