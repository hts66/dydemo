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

    /**
     * 个性化推荐：排除自己的作品和已看过的作品，
     * 按热度打分，并对“关注作者/曾点赞过的作者”给予加权，近 7 天新作再加成。
     */
    @Select("SELECT w.*, u.username, u.avatar, " +
            "  (w.likes_count * 0.4 + w.comments_count * 0.3 + w.views * 0.2 " +
            "   + CASE WHEN w.user_id IN ( " +
            "       SELECT following_id FROM follows WHERE follower_id = #{userId} " +
            "     ) THEN 100 ELSE 0 END " +
            "   + CASE WHEN w.user_id IN ( " +
            "       SELECT DISTINCT wk.user_id FROM likes l JOIN works wk ON l.work_id = wk.id " +
            "       WHERE l.user_id = #{userId} " +
            "     ) THEN 50 ELSE 0 END " +
            "   + CASE WHEN w.created_at > DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 20 ELSE 0 END " +
            "  ) AS recommend_score " +
            "FROM works w " +
            "LEFT JOIN users u ON w.user_id = u.id " +
            "WHERE w.user_id != #{userId} " +
            "  AND w.id NOT IN ( " +
            "    SELECT work_id FROM watch_history WHERE user_id = #{userId} " +
            "  ) " +
            "ORDER BY recommend_score DESC, w.created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Work> selectRecommendWorks(@Param("userId") Long userId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

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
