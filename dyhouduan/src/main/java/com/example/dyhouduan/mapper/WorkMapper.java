package com.example.dyhouduan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dyhouduan.entity.Work;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkMapper extends BaseMapper<Work> {

    @Select("SELECT w.*, u.username, u.avatar " +
            "FROM works w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "ORDER BY w.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Work> selectWorksWithUser(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT w.*, u.username, u.avatar " +
            "FROM works w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "ORDER BY (w.likes_count * 0.4 + w.comments_count * 0.3 + w.views * 0.2) DESC, w.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Work> selectHotWorks(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT w.*, u.username, u.avatar " +
            "FROM works w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "WHERE w.user_id = #{userId} " +
            "ORDER BY w.created_at DESC")
    List<Work> selectByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT w.*, u.username, u.avatar " +
            "FROM works w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "WHERE w.user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>#{userId}</foreach> " +
            "ORDER BY w.created_at DESC" +
            "</script>")
    List<Work> selectByUserIds(@Param("userIds") List<Long> userIds);
}
