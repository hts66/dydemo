package com.example.dyhouduan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dyhouduan.entity.WatchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WatchHistoryMapper extends BaseMapper<WatchHistory> {

    @Select("SELECT work_id FROM watch_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Long> selectWatchedWorkIds(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM watch_history WHERE user_id = #{userId} AND work_id = #{workId}")
    int countByUserIdAndWorkId(@Param("userId") Long userId, @Param("workId") Long workId);
}
