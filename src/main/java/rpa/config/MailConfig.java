package rpa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件服务配置类
 * <p>
 * 配置JavaMailSender用于发送邮件通知。
 * 使用163邮箱SMTP服务，支持SSL加密连接。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.default-encoding}")
    private String defaultEncoding;

    /**
     * 创建JavaMailSender Bean
     * <p>
     * 配置163邮箱SMTP服务器，支持SSL加密连接。
     * </p>
     *
     * @return 配置好的JavaMailSender实例
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding(defaultEncoding);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.socketFactory", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");

        return mailSender;
    }

    /**
     * 获取发件人邮箱地址
     */
    @Bean
    public String fromEmail(@Value("${spring.mail.username}") String fromEmail) {
        return fromEmail;
    }
}
