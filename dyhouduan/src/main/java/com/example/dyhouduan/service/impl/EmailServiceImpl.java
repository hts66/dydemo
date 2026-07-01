package com.example.dyhouduan.service.impl;

import com.example.dyhouduan.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendVerificationCode(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject("短视频平台验证码");

            String content = "<div style=\"font-family: Arial, sans-serif; padding: 20px;\">" +
                    "<h2 style=\"color: #333;\">欢迎使用短视频平台</h2>" +
                    "<p style=\"font-size: 16px; color: #666;\">您的验证码是：</p>" +
                    "<div style=\"font-size: 32px; font-weight: bold; color: #fe2c55; margin: 20px 0;\">" +
                    code +
                    "</div>" +
                    "<p style=\"font-size: 14px; color: #999;\">验证码有效期为5分钟，请尽快使用。</p>" +
                    "</div>";

            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }
}