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

    @Select("SELECT * FROM follows WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    Follow selectByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Select("SELECT u.id, u.username, u.avatar, u.bio, u.gender, u.email " +
            "FROM users u " +
            "INNER JOIN follows f ON u.id = f.following_id " +
            "WHERE f.follower_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectFollowingList(@Param("userId") Long userId);

    @Select("SELECT u.id, u.username, u.avatar, u.bio, u.gender, u.email " +
            "FROM users u " +
            "INNER JOIN follows f ON u.id = f.follower_id " +
            "WHERE f.following_id = #{userId} " +
            "ORDER BY f.created_at DESC")
    List<Map<String, Object>> selectFollowerList(@Param("userId") Long userId);

    @Select("SELECT u.id, u.username, u.avatar, u.bio, u.gender, u.email " +
            "FROM users u " +
            "INNER JOIN follows f1 ON u.id = f1.following_id " +
            "INNER JOIN follows f2 ON u.id = f2.follower_id " +
            "WHERE f1.follower_id = #{userId} AND f2.following_id = #{userId} " +
            "ORDER BY f1.created_at DESC")
    List<Map<String, Object>> selectMutualFriends(@Param("userId") Long userId);
}
