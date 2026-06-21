package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import com.example.dyhouduan.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private OssUtil ossUtil;

    /**
     * 上传图片
     */
    @PostMapping("/image")
    public Response<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = ossUtil.uploadFile(file, "images");
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Response.error("上传图片失败: " + e.getMessage());
        }
    }

    /**
     * 上传视频
     */
    @PostMapping("/video")
    public Response<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            String url = ossUtil.uploadFile(file, "videos");
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传视频失败", e);
            return Response.error("上传视频失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Response<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String url = ossUtil.uploadFile(file, "avatars");
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传头像失败", e);
            return Response.error("上传头像失败: " + e.getMessage());
        }
    }

    /**
     * 通用文件上传
     */
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
            
            String url = ossUtil.uploadFile(file, path);
            return Response.success(url);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Response.error("上传文件失败: " + e.getMessage());
        }
    }
}
