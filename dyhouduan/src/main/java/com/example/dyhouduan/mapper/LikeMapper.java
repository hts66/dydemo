package com.example.dyhouduan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dyhouduan.entity.Like;
import com.example.dyhouduan.entity.Work;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Select("SELECT * FROM likes WHERE user_id = #{userId} AND work_id = #{workId}")
    Like selectByUserAndWork(@Param("userId") Long userId, @Param("workId") Long workId);

    @Select("SELECT w.*, u.username, u.avatar " +
            "FROM works w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "INNER JOIN likes l ON w.id = l.work_id " +
            "WHERE l.user_id = #{userId} " +
            "ORDER BY l.created_at DESC")
    List<Work> selectLikedWorks(@Param("userId") Long userId);
}
