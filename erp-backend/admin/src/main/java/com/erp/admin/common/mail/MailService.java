package com.erp.admin.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.system.model.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 通用邮件发送服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String defaultFrom;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 发送 HTML 邮件（使用默认发件人）
     *
     * @param toEmails    收件人邮箱列表
     * @param subject     邮件主题
     * @param htmlContent HTML 内容
     * @return 成功发送的邮箱数量
     */
    public int sendHtmlMail(List<String> toEmails, String subject, String htmlContent) {
        return sendHtmlMail(toEmails, subject, htmlContent, defaultFrom);
    }

    /**
     * 发送 HTML 邮件（指定发件人）
     *
     * @param toEmails    收件人邮箱列表
     * @param subject     邮件主题
     * @param htmlContent HTML 内容
     * @param from        发件人地址
     * @return 成功发送的邮箱数量
     */
    public int sendHtmlMail(List<String> toEmails, String subject, String htmlContent, String from) {
        int successCount = 0;
        for (String email : toEmails) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                successCount++;
                log.info("邮件发送成功: {}", email);
            } catch (Exception e) {
                log.error("邮件发送失败: {}, 原因: {}", email, e.getMessage());
            }
        }
        return successCount;
    }

    /**
     * 从用户列表中提取有效邮箱
     *
     * @param users 用户列表
     * @return 有效邮箱列表
     */
    public List<String> extractValidEmails(List<SysUser> users) {
        List<String> validEmails = new ArrayList<>();
        for (SysUser user : users) {
            String email = user.getEmail();
            if (email != null && EMAIL_PATTERN.matcher(email).matches()) {
                validEmails.add(email);
            } else {
                log.warn("用户 {} (ID:{}) 邮箱无效或为空，跳过通知",
                        user.getNickname(), user.getUserId());
            }
        }
        return validEmails;
    }
}
