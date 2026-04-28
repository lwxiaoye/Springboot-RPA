package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 多渠道通知推送服务
 * <p>
 * 支持邮件、钉钉、企业微信、飞书等多个通知渠道的统一推送。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiChannelNotificationService {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${rpa.notification.dingtalk.webhook:}")
    private String dingtalkWebhook;

    @Value("${rpa.notification.wecom.corp-id:}")
    private String wecomCorpId;

    @Value("${rpa.notification.wecom.agent-id:}")
    private String wecomAgentId;

    @Value("${rpa.notification.wecom.secret:}")
    private String wecomSecret;

    @Value("${rpa.notification.feishu.app-id:}")
    private String feishuAppId;

    @Value("${rpa.notification.feishu.app-secret:}")
    private String feishuAppSecret;

    @Value("${rpa.notification.feishu.webhook:}")
    private String feishuWebhook;

    @Value("${rpa.notification.enabled:true}")
    private boolean notificationEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 渠道枚举 ====================

    public enum Channel {
        EMAIL("email", "邮件"),
        DINGTALK("dingtalk", "钉钉"),
        WECOM("wecom", "企业微信"),
        FEISHU("feishu", "飞书");

        private final String code;
        private final String name;

        Channel(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
    }

    // ==================== 统一推送接口 ====================

    /**
     * 发送多渠道通知
     */
    @Async
    public void sendMultiChannelNotification(String channels, String subject, String content,
                                           List<NotificationAttachmentService.Attachment> attachments) {
        if (!notificationEnabled) {
            log.debug("通知功能已禁用");
            return;
        }

        String[] channelList = channels.split("[,，;]");

        for (String channel : channelList) {
            channel = channel.trim().toLowerCase();
            try {
                switch (channel) {
                    case "email" -> sendEmail(subject, content, attachments);
                    case "dingtalk" -> sendDingTalkMessage(subject, content);
                    case "wecom" -> sendWeComMessage(subject, content);
                    case "feishu" -> sendFeiShuMessage(subject, content);
                    default -> log.warn("未知的通知渠道: {}", channel);
                }
            } catch (Exception e) {
                log.error("发送{}通知失败: {}", channel, e.getMessage());
            }
        }
    }

    /**
     * 发送多渠道通知（带收件人）
     */
    @Async
    public void sendMultiChannelNotification(String channels, String recipients, String subject,
                                           String content, List<NotificationAttachmentService.Attachment> attachments) {
        if (!notificationEnabled) {
            return;
        }

        String[] channelList = channels.split("[,，;]");

        for (String channel : channelList) {
            channel = channel.trim().toLowerCase();
            try {
                switch (channel) {
                    case "email" -> sendEmailTo(recipients, subject, content, attachments);
                    case "dingtalk" -> sendDingTalkMessageTo(recipients, subject, content);
                    case "wecom" -> sendWeComMessageTo(recipients, subject, content);
                    case "feishu" -> sendFeiShuMessageTo(recipients, subject, content);
                    default -> log.warn("未知的通知渠道: {}", channel);
                }
            } catch (Exception e) {
                log.error("发送{}通知失败: {}", channel, e.getMessage());
            }
        }
    }

    // ==================== 邮件推送 ====================

    /**
     * 发送邮件（使用默认配置）
     */
    @Async
    public void sendEmail(String subject, String content, List<NotificationAttachmentService.Attachment> attachments) {
        sendEmailTo(null, subject, content, attachments);
    }

    /**
     * 发送邮件（指定收件人）
     */
    @Async
    public void sendEmailTo(String recipients, String subject, String content,
                          List<NotificationAttachmentService.Attachment> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);

            // 设置收件人
            if (recipients != null && !recipients.isEmpty()) {
                helper.setTo(recipients.split("[,，;]"));
            }

            helper.setSubject(subject);
            helper.setText(content, true);

            // 添加附件
            if (attachments != null && !attachments.isEmpty()) {
                for (NotificationAttachmentService.Attachment attachment : attachments) {
                    if (attachment.getContent() != null) {
                        helper.addAttachment(attachment.getFileName(),
                                new javax.mail.util.ByteArrayDataSource(
                                        attachment.getContent(),
                                        attachment.getContentType()));
                    }
                }
            }

            mailSender.send(message);
            log.info("邮件发送成功: subject={}, to={}", subject, recipients);

        } catch (MessagingException e) {
            log.error("发送邮件失败: {}", e.getMessage());
        }
    }

    // ==================== 钉钉推送 ====================

    /**
     * 发送钉钉消息
     */
    @Async
    public void sendDingTalkMessage(String title, String content) {
        sendDingTalkMessageTo(null, title, content);
    }

    /**
     * 发送钉钉消息（指定Webhook）
     */
    @Async
    public void sendDingTalkMessageTo(String webhookUrl, String title, String content) {
        try {
            String url = webhookUrl != null ? webhookUrl : dingtalkWebhook;

            if (url == null || url.isEmpty()) {
                log.warn("钉钉Webhook未配置");
                return;
            }

            // 转换HTML为Markdown
            String markdown = convertHtmlToMarkdown(content);

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, Object> markdownContent = new HashMap<>();
            markdownContent.put("title", title);
            markdownContent.put("text", "### " + title + "\n\n" + markdown);
            body.put("markdown", markdownContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("钉钉消息发送成功: title={}", title);

        } catch (Exception e) {
            log.error("发送钉钉消息失败: {}", e.getMessage());
        }
    }

    // ==================== 企业微信推送 ====================

    /**
     * 发送企业微信消息
     */
    @Async
    public void sendWeComMessage(String title, String content) {
        sendWeComMessageTo(null, title, content);
    }

    /**
     * 发送企业微信消息（指定用户）
     */
    @Async
    public void sendWeComMessageTo(String userIds, String title, String content) {
        try {
            // 获取access token
            String token = getWeComAccessToken();
            if (token == null) {
                log.warn("企业微信access_token获取失败");
                return;
            }

            String url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + token;

            // 构建消息
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");
            body.put("agentid", wecomAgentId);

            // 设置接收人
            if (userIds != null && !userIds.isEmpty()) {
                body.put("touser", userIds);
            } else {
                body.put("touser", "@all");
            }

            Map<String, Object> markdownContent = new HashMap<>();
            markdownContent.put("content", "**" + title + "**\n\n" + content);
            body.put("markdown", markdownContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("企业微信消息发送成功: title={}", title);

        } catch (Exception e) {
            log.error("发送企业微信消息失败: {}", e.getMessage());
        }
    }

    /**
     * 获取企业微信Access Token
     */
    private String getWeComAccessToken() {
        try {
            String url = String.format(
                    "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
                    wecomCorpId, wecomSecret);

            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> result = parseJson(response);

            if (result != null && result.get("access_token") != null) {
                return result.get("access_token").toString();
            }

        } catch (Exception e) {
            log.error("获取企业微信access_token失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 飞书推送 ====================

    /**
     * 发送飞书消息
     */
    @Async
    public void sendFeiShuMessage(String title, String content) {
        sendFeiShuMessageTo(null, title, content);
    }

    /**
     * 发送飞书消息（指定用户）
     */
    @Async
    public void sendFeiShuMessageTo(String email, String title, String content) {
        try {
            String webhook = feishuWebhook;

            if (webhook == null || webhook.isEmpty()) {
                log.warn("飞书Webhook未配置");
                return;
            }

            // 转换HTML为飞书格式
            String text = convertHtmlToLarkText(content);

            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "interactive");
            body.put("card", buildLarkCard(title, text));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(webhook, request, String.class);

            log.info("飞书消息发送成功: title={}", title);

        } catch (Exception e) {
            log.error("发送飞书消息失败: {}", e.getMessage());
        }
    }

    /**
     * 构建飞书卡片消息
     */
    private Map<String, Object> buildLarkCard(String title, String content) {
        Map<String, Object> card = new HashMap<>();
        card.put("config", Map.of("wide_screen_mode", true));

        Map<String, Object> header = new HashMap<>();
        header.put("title", Map.of("tag", "plain_text", "content", title));
        header.put("template", "blue");
        card.put("header", header);

        java.util.List<Map<String, Object>> elements = new java.util.ArrayList<>();
        elements.add(Map.of("tag", "div", "text", Map.of(
                "content", content,
                "tag", "lark_md"
        )));
        elements.add(Map.of("tag", "hr"));
        elements.add(Map.of("tag", "note", "elements", List.of(
                Map.of("tag", "plain_text", "content", "来自RPA系统 | " +
                        LocalDateTime.now().format(DATE_FORMATTER))
        )));

        card.put("elements", elements);

        return card;
    }

    // ==================== 辅助方法 ====================

    /**
     * HTML转Markdown
     */
    private String convertHtmlToMarkdown(String html) {
        if (html == null) return "";

        String markdown = html;

        // 移除HTML标签
        markdown = markdown.replaceAll("<[^>]+>", "");
        // 替换常见HTML实体
        markdown = markdown.replaceAll("&nbsp;", " ");
        markdown = markdown.replaceAll("&lt;", "<");
        markdown = markdown.replaceAll("&gt;", ">");
        markdown = markdown.replaceAll("&amp;", "&");
        markdown = markdown.replaceAll("&quot;", "\"");

        // 标题转换
        markdown = Pattern.compile("<h[1-6]>([^<]*)</h[1-6]>")
                .matcher(markdown)
                .replaceAll(match -> "\n### " + match.group(1) + "\n");

        // 换行
        markdown = markdown.replaceAll("<br\\s*/?>", "\n");
        markdown = markdown.replaceAll("\\n{3,}", "\n\n");

        return markdown.trim();
    }

    /**
     * HTML转飞书格式
     */
    private String convertHtmlToLarkText(String html) {
        if (html == null) return "";

        String text = html;

        // 移除HTML标签
        text = text.replaceAll("<[^>]+>", "");
        // 替换常见HTML实体
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&amp;", "&");

        // 换行
        text = text.replaceAll("<br\\s*/?>", "\n");
        text = text.replaceAll("\\n{3,}", "\n\n");

        return text.trim();
    }

    /**
     * 解析JSON字符串
     */
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 测试方法 ====================

    /**
     * 测试钉钉连接
     */
    public boolean testDingTalk(String webhookUrl) {
        try {
            sendDingTalkMessageTo(webhookUrl, "RPA系统测试", "这是一条测试消息，用于验证钉钉配置是否正确。");
            return true;
        } catch (Exception e) {
            log.error("钉钉测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试企业微信连接
     */
    public boolean testWeCom() {
        try {
            String token = getWeComAccessToken();
            return token != null;
        } catch (Exception e) {
            log.error("企业微信测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试飞书连接
     */
    public boolean testFeiShu() {
        try {
            sendFeiShuMessage("RPA系统测试", "这是一条测试消息，用于验证飞书配置是否正确。");
            return true;
        } catch (Exception e) {
            log.error("飞书测试失败: {}", e.getMessage());
            return false;
        }
    }
}
