package rpa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import rpa.service.SystemConfigService;

import java.util.Properties;

/**
 * 邮件服务配置类
 * <p>
 * 配置JavaMailSender用于发送邮件通知。
 * 邮件配置从数据库动态读取，支持前端修改后实时生效。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MailConfig {

    private final SystemConfigService systemConfigService;

    /**
     * 创建JavaMailSender Bean
     * <p>
     * 从数据库动态读取SMTP配置，支持前端修改后实时生效。
     * </p>
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 从数据库读取配置，支持前端修改后实时生效
        String host = systemConfigService.getConfig(SystemConfigService.KEY_SMTP_HOST, "smtp.qq.com");
        String portStr = systemConfigService.getConfig(SystemConfigService.KEY_SMTP_PORT, "587");
        String username = systemConfigService.getConfig(SystemConfigService.KEY_SMTP_USERNAME, "");
        String password = systemConfigService.getConfig(SystemConfigService.KEY_SMTP_PASSWORD, "");
        String sslStr = systemConfigService.getConfig(SystemConfigService.KEY_SMTP_SSL, "true");

        int port = 587;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            log.warn("SMTP端口配置无效，使用默认端口587");
        }
        boolean ssl = "true".equalsIgnoreCase(sslStr);

        log.info("初始化邮件服务 - Host: {}, Port: {}, Username: {}, SSL: {}",
                host, port, username != null && !username.isEmpty() ? "已设置" : "未设置", ssl);

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");

        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.socketFactory", "javax.net.ssl.SSLSocketFactory");
        }

        return mailSender;
    }

    /**
     * 获取发件人邮箱地址
     */
    @Bean
    public String fromEmail() {
        return systemConfigService.getConfig(SystemConfigService.KEY_SMTP_USERNAME, "noreply@rpa.com");
    }
}
