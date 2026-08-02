package com.example.dyhouduan.service;

import com.example.dyhouduan.config.QdrantConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class QdrantService {

    private final QdrantConfig config;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public static final String COLLECTION_VIDEO_TAGS = "video_tags";

    private boolean collectionEnsured = false;

    public QdrantService(QdrantConfig config) {
        this.config = config;
        this.mapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            ensureCollection();
        } catch (Exception e) {
            log.warn("Qdrant 初始化失败（可能 Qdrant 尚未启动）: {}", e.getMessage());
        }
    }

    /** 创建带 API key 的请求构建器 */
    private HttpRequest.Builder apiRequest(String url) {
        var builder = HttpRequest.newBuilder().uri(URI.create(url));
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("api-key", config.getApiKey());
        }
        return builder;
    }

    /** 确保 collection 存在，不存在则创建 */
    public synchronized void ensureCollection() throws Exception {
        if (collectionEnsured) return;
        String url = config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS;
        HttpRequest req = apiRequest(url).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 200) {
            log.info("Qdrant collection '{}' 已存在", COLLECTION_VIDEO_TAGS);
            collectionEnsured = true;
            return;
        }

        // 创建 collection
        Map<String, Object> body = new HashMap<>();
        body.put("vectors", Map.of("size", TagVocabulary.VECTOR_SIZE, "distance", "Cosine"));
        String json = mapper.writeValueAsString(body);

        HttpRequest putReq = apiRequest(config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> putResp = httpClient.send(putReq, HttpResponse.BodyHandlers.ofString());
        log.info("创建 Qdrant collection '{}': {} {}", COLLECTION_VIDEO_TAGS, putResp.statusCode(), putResp.body());
        if (putResp.statusCode() == 200) {
            collectionEnsured = true;
        }
    }

    /** 批量 upsert points */
    public void upsertPoints(List<Point> points) throws Exception {
        if (points.isEmpty()) return;

        List<Map<String, Object>> pointList = new ArrayList<>();
        for (Point p : points) {
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("id", p.id);
            pt.put("vector", p.vector);
            pt.put("payload", p.payload);
            pointList.add(pt);
        }

        Map<String, Object> body = Map.of("points", pointList);
        String json = mapper.writeValueAsString(body);

        HttpRequest req = apiRequest(config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS + "/points?wait=true")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            log.info("Qdrant upsert {} 个向量成功", points.size());
        } else {
            log.error("Qdrant upsert 失败: {} {}", resp.statusCode(), resp.body());
        }
    }

    /** 搜索相似视频 */
    public List<SearchResult> searchSimilar(float[] vector, int limit) throws Exception {
        ensureCollection();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", vector);
        body.put("limit", limit);
        body.put("with_payload", true);

        String json = mapper.writeValueAsString(body);

        HttpRequest req = apiRequest(config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS + "/points/search")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        List<SearchResult> results = new ArrayList<>();

        if (resp.statusCode() == 200) {
            var root = mapper.readTree(resp.body());
            var points = root.get("result");
            if (points != null && points.isArray()) {
                for (var pt : points) {
                    long id = pt.get("id").asLong();
                    double score = pt.get("score").asDouble();
                    Map<String, Object> payload = new LinkedHashMap<>();
                    var p = pt.get("payload");
                    if (p != null) {
                        var fields = p.fields();
                        while (fields.hasNext()) {
                            var f = fields.next();
                            payload.put(f.getKey(), f.getValue().asText());
                        }
                    }
                    results.add(new SearchResult(id, score, payload));
                }
            }
        }
        return results;
    }

    /** 获取已处理的 workId 集合 */
    public Set<Long> getProcessedIds() throws Exception {
        String url = config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS + "/points/scroll";
        Map<String, Object> body = Map.of(
            "limit", 10000,
            "with_payload", false,
            "with_vector", false
        );
        String json = mapper.writeValueAsString(body);

        HttpRequest req = apiRequest(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Set<Long> ids = new HashSet<>();
        if (resp.statusCode() == 200) {
            var root = mapper.readTree(resp.body());
            var points = root.get("result").get("points");
            if (points != null) {
                for (var pt : points) {
                    ids.add(pt.get("id").asLong());
                }
            }
        }
        return ids;
    }

    /** 删除单个 point */
    public void deletePoint(Long id) {
        try {
            String url = config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS + "/points/delete";
            Map<String, Object> body = Map.of("points", List.of(id));
            String json = mapper.writeValueAsString(body);

            HttpRequest req = apiRequest(url)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                log.info("Qdrant 删除 point 成功: id={}", id);
            } else {
                log.warn("Qdrant 删除 point 失败: id={} status={}", id, resp.statusCode());
            }
        } catch (Exception e) {
            log.error("Qdrant 删除 point 异常: id={}", id, e);
        }
    }

    /** 获取单个 point 的向量 */
    public float[] getPointVector(Long workId) {
        try {
            String url = config.getBaseUrl() + "/collections/" + COLLECTION_VIDEO_TAGS + "/points/" + workId;
            HttpRequest req = apiRequest(url).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                var root = mapper.readTree(resp.body());
                var result = root.get("result");
                if (result != null && result.has("vector")) {
                    var vecNode = result.get("vector");
                    float[] vec = new float[TagVocabulary.VECTOR_SIZE];
                    if (vecNode.isArray()) {
                        for (int i = 0; i < Math.min(vecNode.size(), vec.length); i++) {
                            vec[i] = vecNode.get(i).floatValue();
                        }
                    }
                    return vec;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("获取 point 向量失败: workId={}", workId, e);
            return null;
        }
    }

    /** Qdrant point */
    public static class Point {
        public long id;
        public float[] vector;
        public Map<String, Object> payload;

        public Point(long id, float[] vector, Map<String, Object> payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }
    }

    /** 搜索结果 */
    public record SearchResult(long id, double score, Map<String, Object> payload) {}
}
