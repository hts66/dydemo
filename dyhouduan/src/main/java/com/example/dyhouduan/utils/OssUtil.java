package com.example.dyhouduan.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.dyhouduan.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class OssUtil {

    @Autowired
    private OssProperties ossProperties;

    private OSS getOssClient() {
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }

    /**
     * 上传文件到OSS
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

        OSS ossClient = getOssClient();
        try {
            InputStream inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(extension));

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    fullPath,
                    inputStream,
                    metadata
            );

            ossClient.putObject(putObjectRequest);

            // 返回访问URL
            return ossProperties.getUrlPrefix() + "/" + fullPath;
        } catch (OSSException | ClientException | IOException e) {
            log.error("上传文件到OSS失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 上传文件到OSS（使用原始文件名）
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

        OSS ossClient = getOssClient();
        try {
            InputStream inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(originalFilename));

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    fullPath,
                    inputStream,
                    metadata
            );

            ossClient.putObject(putObjectRequest);

            return ossProperties.getUrlPrefix() + "/" + fullPath;
        } catch (OSSException | ClientException | IOException e) {
            log.error("上传文件到OSS失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 删除OSS文件
     *
     * @param fileUrl 文件访问URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // 从URL中提取文件路径
        String filePath = fileUrl.replace(ossProperties.getUrlPrefix() + "/", "");

        OSS ossClient = getOssClient();
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), filePath);
        } catch (OSSException | ClientException e) {
            log.error("删除OSS文件失败", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        } finally {
            ossClient.shutdown();
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
