package com.example.dyhouduan.utils;

import com.example.dyhouduan.config.MinioProperties;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class MinioUtil {

    @Autowired
    private MinioProperties minioProperties;

    private MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    /**
     * 上传文件到MinIO
     *
     * @param file     上传的文件
     * @param filePath 文件存储路径（不包含bucket名称）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String filePath) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        // 生成唯一文件名
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 完整存储路径
        String fullPath = filePath + "/" + newFileName;

        MinioClient minioClient = getMinioClient();
        try (InputStream inputStream = file.getInputStream()) {
            // 确保 Bucket 存在
            ensureBucketExists(minioClient);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(getContentType(extension))
                            .build()
            );

            // 返回访问URL
            return minioProperties.getUrlPrefix() + "/" + fullPath;
        } catch (Exception e) {
            log.error("上传文件到MinIO失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件到MinIO（使用原始文件名）
     *
     * @param file     上传的文件
     * @param filePath 文件存储路径
     * @return 文件访问URL
     */
    public String uploadFileOriginalName(MultipartFile file, String filePath) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String fullPath = filePath + "/" + originalFilename;

        MinioClient minioClient = getMinioClient();
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists(minioClient);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fullPath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(getContentType(originalFilename))
                            .build()
            );

            return minioProperties.getUrlPrefix() + "/" + fullPath;
        } catch (Exception e) {
            log.error("上传文件到MinIO失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除MinIO文件
     *
     * @param fileUrl 文件访问URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // 从URL中提取文件路径
        String filePath = fileUrl.replace(minioProperties.getUrlPrefix() + "/", "");

        MinioClient minioClient = getMinioClient();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("删除MinIO文件失败", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 确保 Bucket 存在，不存在则创建
     */
    private void ensureBucketExists(MinioClient minioClient) throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build()
        );
        if (!found) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build()
            );
            log.info("Bucket '{}' 创建成功", minioProperties.getBucketName());
        }
    }

    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".mp4":
                return "video/mp4";
            case ".webm":
                return "video/webm";
            case ".avi":
                return "video/x-msvideo";
            case ".mov":
                return "video/quicktime";
            default:
                return "application/octet-stream";
        }
    }
}
