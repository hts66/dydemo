package com.example.dyhouduan.controller;

import com.example.dyhouduan.dto.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final int EXPIRATION_MINUTES = 5;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public Response<Map<String, String>> getCaptcha() {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            Random random = new Random();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 10; i++) {
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            for (int i = 0; i < 20; i++) {
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }

            String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            StringBuilder code = new StringBuilder();
            g.setFont(new Font("Arial", Font.BOLD, 24));

            for (int i = 0; i < CODE_LENGTH; i++) {
                char c = chars.charAt(random.nextInt(chars.length()));
                code.append(c);
                g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
                g.drawString(String.valueOf(c), 25 + i * 22, 28);
            }

            String captchaKey = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("captcha:" + captchaKey, code.toString(), EXPIRATION_MINUTES, TimeUnit.MINUTES);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String imageBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

            g.dispose();

            Map<String, String> result = new HashMap<>();
            result.put("key", captchaKey);
            result.put("image", imageBase64);

            return Response.success(result);
        } catch (Exception e) {
            return Response.error(500, "生成验证码失败: " + e.getMessage());
        }
    }
}