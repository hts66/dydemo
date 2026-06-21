package com.example.dyhouduan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dyhouduan.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    @Select("SELECT * FROM follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    Follow selectByFollowerAndFollowee(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT u.id, u.username, u.avatar, u.bio, u.gender, u.email " +
            "FROM users u " +
            "INNER JOIN follows f ON u.id = f.followee_id " +
            "WHERE f.follower_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectFollowingList(@Param("userId") Long userId);

    @Select("SELECT u.id, u.username, u.avatar, u.bio, u.gender, u.email " +
            "FROM users u " +
            "INNER JOIN follows f ON u.id = f.follower_id " +
            "WHERE f.followee_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectFollowerList(@Param("userId") Long userId);
}
