package com.example.dyhouduan.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@RestController
@RequestMapping("/api/video")
public class VideoProxyController {

    @GetMapping("/proxy")
    public void proxyVideo(
            @RequestParam String url,
            @RequestHeader(value = "Range", required = false) String range,
            @RequestHeader(value = "Access-Control-Request-Method", required = false) String acrm,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        if (acrm != null || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Range");
            response.setStatus(HttpStatus.OK.value());
            return;
        }
        
        log.info("视频代理请求 - URL: {}, Range: {}", url, range);
        
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            URL videoUrl = new URL(url);
            connection = (HttpURLConnection) videoUrl.openConnection();
            connection.setRequestMethod("HEAD".equalsIgnoreCase(request.getMethod()) ? "HEAD" : "GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            if (range != null && !range.isEmpty()) {
                connection.setRequestProperty("Range", range);
            }

            int responseCode = connection.getResponseCode();
            
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                log.error("视频代理请求失败 - URL: {}, 响应码: {}", url, responseCode);
                response.setStatus(HttpStatus.BAD_GATEWAY.value());
                return;
            }

            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();
            String contentRange = connection.getHeaderField("Content-Range");
            boolean isHead = "HEAD".equalsIgnoreCase(request.getMethod());
            if (!isHead) {
                inputStream = connection.getInputStream();
            }

            if (contentType == null || contentType.isEmpty()) {
                if (url.toLowerCase().endsWith(".mp4")) {
                    contentType = "video/mp4";
                } else if (url.toLowerCase().endsWith(".webm")) {
                    contentType = "video/webm";
                } else if (url.toLowerCase().endsWith(".ogg")) {
                    contentType = "video/ogg";
                } else {
                    contentType = "video/mp4";
                }
            }

            log.info("视频代理请求成功 - URL: {}, ContentType: {}, ContentLength: {}", url, contentType, contentLength);

            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Range");
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentType(contentType);
            
            if (contentRange != null) {
                response.setHeader("Content-Range", contentRange);
            }
            if (contentLength > 0) {
                response.setContentLengthLong(contentLength);
            }

            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            } else {
                response.setStatus(HttpStatus.OK.value());
            }

            if (isHead) {
                log.info("视频代理 HEAD 请求完成 - URL: {}", url);
                return;
            }

            outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
                totalRead += bytesRead;
            }
            
            log.info("视频代理传输完成 - URL: {}, 传输字节数: {}", url, totalRead);

        } catch (IOException e) {
            log.error("视频代理异常 - URL: {}", url, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输出流失败", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输入流失败", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}