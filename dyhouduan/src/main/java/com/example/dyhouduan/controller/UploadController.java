package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.utils.FileStorageUtil;
import com.example.dyhouduan.utils.MinioUtil;
import com.example.dyhouduan.config.MinioProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    @Autowired
    private MinioProperties minioProperties;

    private boolean useMinio() {
        return minioProperties.getEndpoint() != null && !minioProperties.getEndpoint().isEmpty();
    }

    @PostMapping("/image")
    public Response<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url;
            if (useMinio()) {
                url = minioUtil.uploadFile(file, "images");
            } else {
                url = fileStorageUtil.uploadFile(file, "images");
            }
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Response.error("上传图片失败: " + e.getMessage());
        }
    }

    @PostMapping("/video")
    public Response<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            String url;
            if (useMinio()) {
                url = minioUtil.uploadFile(file, "videos");
            } else {
                url = fileStorageUtil.uploadFile(file, "videos");
            }
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传视频失败", e);
            return Response.error("上传视频失败: " + e.getMessage());
        }
    }

    @PostMapping("/avatar")
    public Response<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url;
            if (useMinio()) {
                url = minioUtil.uploadFile(file, "avatars");
            } else {
                url = fileStorageUtil.uploadFile(file, "avatars");
            }
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传头像失败", e);
            return Response.error("上传头像失败: " + e.getMessage());
        }
    }

    @PostMapping("/background")
    public Response<String> uploadBackground(@RequestParam("file") MultipartFile file) {
        try {
            String url;
            if (useMinio()) {
                url = minioUtil.uploadFile(file, "backgrounds");
            } else {
                url = fileStorageUtil.uploadFile(file, "backgrounds");
            }
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传背景图片失败", e);
            return Response.error("上传背景图片失败: " + e.getMessage());
        }
    }

    @PostMapping("/file")
    public Response<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String path = "files";

            if (originalFilename != null) {
                if (originalFilename.matches(".*\\.(jpg|jpeg|png|gif)$")) {
                    path = "images";
                } else if (originalFilename.matches(".*\\.(mp4|webm|avi|mov)$")) {
                    path = "videos";
                }
            }

            String url;
            if (useMinio()) {
                url = minioUtil.uploadFile(file, path);
            } else {
                url = fileStorageUtil.uploadFile(file, path);
            }
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Response.error("上传文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/cleanup")
    public Response<String> cleanupFiles(@RequestBody java.util.List<String> urls) {
        try {
            if (urls == null || urls.isEmpty()) {
                return Response.success("无需清理");
            }

            for (String url : urls) {
                if (url != null && !url.isEmpty()) {
                    if (useMinio()) {
                        minioUtil.deleteFile(url);
                    } else {
                        fileStorageUtil.deleteFile(url);
                    }
                    log.info("清理未发布文件: {}", url);
                }
            }
            return Response.success("清理成功");
        } catch (Exception e) {
            log.error("清理文件失败", e);
            return Response.error("清理文件失败: " + e.getMessage());
        }
    }
}
