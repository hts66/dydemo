package com.example.dyhouduan.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class FileStorageUtil {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String UPLOAD_DIR = "./uploads";

    public String uploadFile(MultipartFile file, String filePath) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR, filePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetPath);

            String url = "http://localhost:" + serverPort + "/uploads/" + filePath + "/" + newFileName;
            log.info("文件上传成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String path = fileUrl.replace("http://localhost:" + serverPort + "/uploads/", "");
            Path filePath = Paths.get(UPLOAD_DIR, path);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", fileUrl);
        } catch (IOException e) {
            log.error("删除文件失败", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }
}
