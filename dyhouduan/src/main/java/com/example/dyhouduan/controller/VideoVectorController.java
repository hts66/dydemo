package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.entity.Work;
import com.example.dyhouduan.service.QdrantService;
import com.example.dyhouduan.service.VideoTagService;
import com.example.dyhouduan.service.WorkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 视频向量化控制器 — 批量分析视频内容并存入 Qdrant
 */
@Slf4j
@RestController
@RequestMapping("/api/vector")
public class VideoVectorController {

    private final WorkService workService;
    private final VideoTagService videoTagService;
    private final QdrantService qdrantService;

    // 处理状态
    private final AtomicInteger total = new AtomicInteger(0);
    private final AtomicInteger processed = new AtomicInteger(0);
    private final AtomicInteger success = new AtomicInteger(0);
    private volatile boolean running = false;
    private volatile String currentStatus = "idle";

    public VideoVectorController(WorkService workService, VideoTagService videoTagService,
                                  QdrantService qdrantService) {
        this.workService = workService;
        this.videoTagService = videoTagService;
        this.qdrantService = qdrantService;
    }

    /** 获取处理进度 */
    @GetMapping("/progress")
    public Response<Map<String, Object>> progress() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("running", running);
        map.put("total", total.get());
        map.put("processed", processed.get());
        map.put("success", success.get());
        map.put("status", currentStatus);
        return Response.success(map);
    }

    /** 启动全量处理 */
    @PostMapping("/process-all")
    public Response<Map<String, Object>> processAll() {
        if (running) {
            return Response.error("正在处理中，请等待完成");
        }

        running = true;
        total.set(0);
        processed.set(0);
        success.set(0);

        CompletableFuture.runAsync(() -> {
            try {
                currentStatus = "初始化 Qdrant collection";
                qdrantService.ensureCollection();

                currentStatus = "获取已处理列表";
                Set<Long> done = qdrantService.getProcessedIds();
                log.info("已有 {} 个视频已完成向量化", done.size());

                currentStatus = "加载所有作品";
                // 分批从数据库加载
                List<Work> allWorks = loadAllWorks();
                total.set(allWorks.size());
                log.info("共 {} 个视频待处理，跳过 {} 个已处理", allWorks.size(), done.size());

                List<QdrantService.Point> batch = new ArrayList<>();
                int batchSize = 10;

                for (Work work : allWorks) {
                    if (!running) break;

                    if (done.contains(work.getId())) {
                        processed.incrementAndGet();
                        continue;
                    }

                    currentStatus = "分析: " + (work.getTitle() != null ?
                            work.getTitle().substring(0, Math.min(30, work.getTitle().length())) : "无标题");

                    try {
                        VideoTagService.TagResult result = videoTagService.analyze(work);

                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("title", work.getTitle());
                        payload.put("description", work.getDescription());
                        payload.put("tags", result.tags());
                        payload.put("work_id", work.getId());
                        payload.put("user_id", work.getUserId());

                        batch.add(new QdrantService.Point(work.getId(), result.vector(), payload));
                        success.incrementAndGet();

                        if (batch.size() >= batchSize) {
                            qdrantService.upsertPoints(batch);
                            batch.clear();
                            Thread.sleep(500); // 避免 DeepSeek 限流
                        }
                    } catch (Exception e) {
                        log.error("处理视频 {} 失败: {}", work.getId(), e.getMessage());
                    }

                    processed.incrementAndGet();
                }

                // 处理剩余批次
                if (!batch.isEmpty()) {
                    qdrantService.upsertPoints(batch);
                }

                currentStatus = "完成";
                log.info("向量化完成! 成功: {}, 总计: {}", success.get(), total.get());

            } catch (Exception e) {
                log.error("批量处理异常", e);
                currentStatus = "异常: " + e.getMessage();
            } finally {
                running = false;
            }
        });

        return Response.success(Map.of("message", "开始后台处理", "progress", "/api/vector/progress"));
    }

    /** 单个视频重新分析 */
    @PostMapping("/reprocess/{workId}")
    public Response<Map<String, Object>> reprocess(@PathVariable Long workId) {
        try {
            Work work = workService.getById(workId);
            if (work == null) {
                return Response.error("视频不存在");
            }

            VideoTagService.TagResult result = videoTagService.analyze(work);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", work.getTitle());
            payload.put("description", work.getDescription());
            payload.put("tags", result.tags());
            payload.put("work_id", work.getId());
            payload.put("user_id", work.getUserId());

            qdrantService.upsertPoints(List.of(
                    new QdrantService.Point(work.getId(), result.vector(), payload)
            ));

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("tags", result.tags());
            resp.put("analysis", result.rawAnalysis());
            return Response.success(resp);

        } catch (Exception e) {
            log.error("重新分析失败", e);
            return Response.error("分析失败: " + e.getMessage());
        }
    }

    /** 停止处理 */
    @PostMapping("/stop")
    public Response<String> stop() {
        running = false;
        currentStatus = "stopped";
        return Response.success("已停止");
    }

    /** 测试：分析单个视频并返回标签（不存 Qdrant） */
    @GetMapping("/test/{workId}")
    public Response<Map<String, Object>> test(@PathVariable Long workId) {
        try {
            Work work = workService.getById(workId);
            if (work == null) {
                return Response.error("视频不存在");
            }

            VideoTagService.TagResult result = videoTagService.analyze(work);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("work_id", work.getId());
            resp.put("title", work.getTitle());
            resp.put("tags", result.tags());
            resp.put("vector_dim", result.vector().length);
            resp.put("vector_sample", Arrays.toString(Arrays.copyOf(result.vector(), 20)) + "...");
            resp.put("analysis", result.rawAnalysis());
            return Response.success(resp);

        } catch (Exception e) {
            log.error("测试失败", e);
            return Response.error("测试失败: " + e.getMessage());
        }
    }

    /** 分页加载所有作品 */
    private List<Work> loadAllWorks() {
        List<Work> all = new ArrayList<>();
        int page = 1;
        int size = 200;
        while (true) {
            List<Work> batch = workService.getWorksWithUser(page, size);
            if (batch.isEmpty()) break;
            all.addAll(batch);
            if (batch.size() < size) break;
            page++;
        }
        return all;
    }
}
