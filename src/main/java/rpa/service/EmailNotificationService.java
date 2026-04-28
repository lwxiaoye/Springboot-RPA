package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rpa.entity.Task;
import rpa.entity.User;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 邮件通知服务
 * <p>
 * 提供系统各类邮件通知功能：
 * - 任务完成通知
 * - 任务失败通知
 * - 报表订阅通知
 * - 系统告警通知
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${rpa.notification.task-complete.enabled:true}")
    private boolean taskCompleteEnabled;

    @Value("${rpa.notification.task-failed.enabled:true}")
    private boolean taskFailedEnabled;

    @Value("${rpa.notification.alert.enabled:true}")
    private boolean alertEnabled;

    @Value("${rpa.notification.subscription.enabled:true}")
    private boolean subscriptionEnabled;

    @Value("${rpa.notification.subscription.report-url:http://localhost:8080}")
    private String reportBaseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 任务完成通知 ====================

    /**
     * 发送任务完成通知邮件
     * <p>
     * 当任务成功完成时，自动发送通知邮件给任务创建者。
     * </p>
     *
     * @param task  完成的任务
     * @param user  任务创建者
     * @param recipients 收件人邮箱（可为空，使用user的邮箱）
     */
    @Async
    public void sendTaskCompletionEmail(Task task, User user, String recipients) {
        if (!taskCompleteEnabled) {
            log.debug("任务完成通知已禁用");
            return;
        }

        String toEmail = getRecipientEmail(user, recipients);
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("无法获取任务 {} 的通知邮箱", task.getName());
            return;
        }

        try {
            String subject = String.format("【RPA通知】任务已完成：%s", task.getName());
            String content = buildTaskCompletionContent(task);

            sendHtmlEmail(toEmail, subject, content);
            log.info("任务完成通知邮件已发送: taskId={}, to={}", task.getId(), toEmail);

        } catch (Exception e) {
            log.error("发送任务完成通知邮件失败: taskId={}, error={}", task.getId(), e.getMessage());
        }
    }

    /**
     * 发送任务完成通知邮件（简单版）
     */
    @Async
    public void sendTaskCompletionEmail(Task task, String toEmail) {
        sendTaskCompletionEmail(task, null, toEmail);
    }

    // ==================== 任务失败通知 ====================

    /**
     * 发送任务失败通知邮件
     * <p>
     * 当任务执行失败时，自动发送通知邮件给任务创建者。
     * </p>
     *
     * @param task      失败的任务
     * @param user      任务创建者
     * @param errorMessage 错误信息
     * @param recipients 收件人邮箱
     */
    @Async
    public void sendTaskFailedEmail(Task task, User user, String errorMessage, String recipients) {
        if (!taskFailedEnabled) {
            log.debug("任务失败通知已禁用");
            return;
        }

        String toEmail = getRecipientEmail(user, recipients);
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("无法获取任务 {} 的通知邮箱", task.getName());
            return;
        }

        try {
            String subject = String.format("【RPA告警】任务执行失败：%s", task.getName());
            String content = buildTaskFailedContent(task, errorMessage);

            sendHtmlEmail(toEmail, subject, content);
            log.info("任务失败通知邮件已发送: taskId={}, to={}", task.getId(), toEmail);

        } catch (Exception e) {
            log.error("发送任务失败通知邮件失败: taskId={}, error={}", task.getId(), e.getMessage());
        }
    }

    /**
     * 发送任务失败通知邮件（简单版）
     */
    @Async
    public void sendTaskFailedEmail(Task task, String errorMessage, String toEmail) {
        sendTaskFailedEmail(task, null, errorMessage, toEmail);
    }

    // ==================== 报表订阅通知 ====================

    /**
     * 发送报表订阅通知邮件
     * <p>
     * 定时发送报表数据给订阅用户。
     * </p>
     *
     * @param subscriptionName 订阅名称
     * @param reportType       报表类型
     * @param toEmail          收件人邮箱
     * @param reportData       报表数据（HTML格式）
     * @param period           报表周期（日报/周报/月报）
     */
    @Async
    public void sendReportSubscriptionEmail(String subscriptionName, String reportType,
                                           String toEmail, String reportData, String period) {
        if (!subscriptionEnabled) {
            log.debug("报表订阅通知已禁用");
            return;
        }

        try {
            String subject = String.format("【RPA报表】%s - %s", period, subscriptionName);
            String content = buildReportSubscriptionContent(subscriptionName, reportType, reportData, period);

            sendHtmlEmail(toEmail, subject, content);
            log.info("报表订阅邮件已发送: subscription={}, to={}", subscriptionName, toEmail);

        } catch (Exception e) {
            log.error("发送报表订阅邮件失败: subscription={}, error={}", subscriptionName, e.getMessage());
        }
    }

    /**
     * 发送简单的报表摘要邮件
     */
    @Async
    public void sendReportSummaryEmail(String toEmail, String reportTitle,
                                       String summaryContent, String period) {
        try {
            String subject = String.format("【RPA报表】%s - %s", period, reportTitle);
            String content = buildSimpleReportContent(reportTitle, summaryContent, period);

            sendHtmlEmail(toEmail, subject, content);
            log.info("报表摘要邮件已发送: to={}, title={}", toEmail, reportTitle);

        } catch (Exception e) {
            log.error("发送报表摘要邮件失败: to={}, error={}", toEmail, e.getMessage());
        }
    }

    // ==================== 系统告警通知 ====================

    /**
     * 发送系统告警邮件
     *
     * @param alertTitle   告警标题
     * @param alertContent 告警内容
     * @param level        告警级别（info/warning/error/critical）
     */
    @Async
    public void sendAlertEmail(String alertTitle, String alertContent, String level) {
        if (!alertEnabled) {
            log.debug("系统告警通知已禁用");
            return;
        }

        try {
            String subject = String.format("【RPA告警-%s】%s", level.toUpperCase(), alertTitle);
            String content = buildAlertContent(alertTitle, alertContent, level);

            sendHtmlEmail(fromEmail, subject, content);
            log.info("系统告警邮件已发送: title={}, level={}", alertTitle, level);

        } catch (Exception e) {
            log.error("发送系统告警邮件失败: title={}, error={}", alertTitle, e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取收件人邮箱
     */
    private String getRecipientEmail(User user, String recipients) {
        if (recipients != null && !recipients.isEmpty()) {
            return recipients;
        }
        if (user != null && user.getEmail() != null) {
            return user.getEmail();
        }
        return null;
    }

    /**
     * 发送HTML格式邮件
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * 发送纯文本邮件
     */
    private void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // ==================== 邮件内容构建 ====================

    /**
     * 构建任务完成邮件内容
     */
    private String buildTaskCompletionContent(Task task) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // 标题
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 10px 10px 0 0;'>");
        html.append("<h2 style='color: white; margin: 0;'>✅ 任务执行完成</h2>");
        html.append("</div>");

        // 内容
        html.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px;'>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>任务名称：</strong></td>");
        html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd;'>").append(task.getName()).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>任务ID：</strong></td>");
        html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd;'>").append(task.getId()).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>执行状态：</strong></td>");
        html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd; color: #28a745;'><strong>✅ 成功</strong></td></tr>");

        if (task.getStartTime() != null) {
            html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>开始时间：</strong></td>");
            html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd;'>")
                .append(task.getStartTime().format(DATE_FORMATTER)).append("</td></tr>");
        }

        if (task.getEndTime() != null) {
            html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>完成时间：</strong></td>");
            html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd;'>")
                .append(task.getEndTime().format(DATE_FORMATTER)).append("</td></tr>");

            // 计算耗时
            long durationMinutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
            html.append("<tr><td style='padding: 10px;'><strong>执行耗时：</strong></td>");
            html.append("<td style='padding: 10px;'>").append(durationMinutes).append(" 分钟</td></tr>");
        }

        html.append("</table>");

        // 按钮
        html.append("<div style='margin-top: 20px; text-align: center;'>");
        html.append("<a href='").append(reportBaseUrl).append("' style='display: inline-block; padding: 12px 30px; ");
        html.append("background: #667eea; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>");
        html.append("查看报表详情 →</a>");
        html.append("</div>");

        // 页脚
        html.append("<div style='margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px;'>");
        html.append("<p>此邮件由RPA系统自动发送，请勿回复。</p>");
        html.append("<p>如需管理通知设置，请访问系统通知中心。</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * 构建任务失败邮件内容
     */
    private String buildTaskFailedContent(Task task, String errorMessage) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // 标题
        html.append("<div style='background: linear-gradient(135deg, #f56c6c 0%, #e64a19 100%); padding: 20px; border-radius: 10px 10px 0 0;'>");
        html.append("<h2 style='color: white; margin: 0;'>⚠️ 任务执行失败</h2>");
        html.append("</div>");

        // 内容
        html.append("<div style='background: #fff3e0; padding: 20px; border-radius: 0 0 10px 10px;'>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>任务名称：</strong></td>");
        html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd;'>").append(task.getName()).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>任务ID：</strong></td>");
        html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd;'>").append(task.getId()).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border-bottom: 1px solid #ddd;'><strong>执行状态：</strong></td>");
        html.append("<td style='padding: 10px; border-bottom: 1px solid #ddd; color: #dc3545;'><strong>❌ 失败</strong></td></tr>");

        if (task.getStartTime() != null) {
            html.append("<tr><td style='padding: 10px;'><strong>开始时间：</strong></td>");
            html.append("<td style='padding: 10px;'>")
                .append(task.getStartTime().format(DATE_FORMATTER)).append("</td></tr>");
        }

        html.append("</table>");

        // 错误信息
        html.append("<div style='margin-top: 15px; padding: 15px; background: #ffebee; border-radius: 5px; border-left: 4px solid #f56c6c;'>");
        html.append("<strong style='color: #dc3545;'>错误信息：</strong>");
        html.append("<p style='margin: 10px 0 0 0; color: #333;'>").append(errorMessage != null ? errorMessage : "未知错误").append("</p>");
        html.append("</div>");

        // 按钮
        html.append("<div style='margin-top: 20px; text-align: center;'>");
        html.append("<a href='").append(reportBaseUrl).append("' style='display: inline-block; padding: 12px 30px; ");
        html.append("background: #f56c6c; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>");
        html.append("查看任务详情 →</a>");
        html.append("</div>");

        // 页脚
        html.append("<div style='margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px;'>");
        html.append("<p>此邮件由RPA系统自动发送，请勿回复。</p>");
        html.append("<p>如有问题，请联系系统管理员。</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * 构建报表订阅邮件内容
     */
    private String buildReportSubscriptionContent(String subscriptionName, String reportType,
                                                  String reportData, String period) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 800px; margin: 0 auto; padding: 20px;'>");

        // 标题
        html.append("<div style='background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%); padding: 20px; border-radius: 10px 10px 0 0;'>");
        html.append("<h2 style='color: white; margin: 0;'>📊 RPA报表订阅 - ").append(period).append("</h2>");
        html.append("<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0;'>").append(subscriptionName).append("</p>");
        html.append("</div>");

        // 内容
        html.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px;'>");

        // 报表类型
        html.append("<div style='margin-bottom: 20px;'>");
        html.append("<strong>报表类型：</strong>").append(reportType);
        html.append(" &nbsp;|&nbsp; <strong>生成时间：</strong>").append(LocalDateTime.now().format(DATE_FORMATTER));
        html.append("</div>");

        // 报表数据
        if (reportData != null && !reportData.isEmpty()) {
            html.append("<div style='background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px;'>");
            html.append(reportData);
            html.append("</div>");
        }

        // 按钮
        html.append("<div style='margin-top: 20px; text-align: center;'>");
        html.append("<a href='").append(reportBaseUrl).append("' style='display: inline-block; padding: 12px 30px; ");
        html.append("background: #409eff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin-right: 10px;'>");
        html.append("查看完整报表 →</a>");
        html.append("</div>");

        // 页脚
        html.append("<div style='margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px;'>");
        html.append("<p>此报表由RPA系统自动生成并发送，如需修改订阅设置，请访问系统。</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * 构建简单报表邮件内容
     */
    private String buildSimpleReportContent(String reportTitle, String summaryContent, String period) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // 标题
        html.append("<div style='background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%); padding: 20px; border-radius: 10px 10px 0 0;'>");
        html.append("<h2 style='color: white; margin: 0;'>📊 ").append(reportTitle).append("</h2>");
        html.append("<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0;'>").append(period).append("</p>");
        html.append("</div>");

        // 内容
        html.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px;'>");
        html.append(summaryContent);

        // 按钮
        html.append("<div style='margin-top: 20px; text-align: center;'>");
        html.append("<a href='").append(reportBaseUrl).append("' style='display: inline-block; padding: 12px 30px; ");
        html.append("background: #409eff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>");
        html.append("查看完整报表 →</a>");
        html.append("</div>");

        // 页脚
        html.append("<div style='margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px;'>");
        html.append("<p>此报表由RPA系统自动生成，如需修改订阅设置，请联系管理员。</p>");
        html.append("</div>");

        html.append("</div></div></body></html>");

        return html.toString();
    }

    /**
     * 构建系统告警邮件内容
     */
    private String buildAlertContent(String alertTitle, String alertContent, String level) {
        String color = "#ffc107";
        if ("error".equals(level) || "critical".equals(level)) {
            color = "#dc3545";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // 标题
        html.append("<div style='background: linear-gradient(135deg, ").append(color).append(" 0%, #").append(color).append(" 100%);");
        html.append(" padding: 20px; border-radius: 10px 10px 0 0;'>");
        html.append("<h2 style='color: white; margin: 0;'>🚨 系统告警</h2>");
        html.append("<p style='color: rgba(255,255,255,0.9); margin: 10px 0 0 0;'>级别：").append(level.toUpperCase()).append("</p>");
        html.append("</div>");

        // 内容
        html.append("<div style='background: #fff3e0; padding: 20px; border-radius: 0 0 10px 10px;'>");
        html.append("<h3 style='margin-top: 0;'>").append(alertTitle).append("</h3>");
        html.append("<p>").append(alertContent).append("</p>");
        html.append("<p style='color: #666; font-size: 12px;'>发送时间：").append(LocalDateTime.now().format(DATE_FORMATTER)).append("</p>");
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    // ==================== 批量发送 ====================

    /**
     * 批量发送邮件（用于报表订阅）
     *
     * @param toEmails  收件人邮箱列表（逗号分隔）
     * @param subject   邮件主题
     * @param content   邮件内容（HTML）
     */
    @Async
    public void sendBatchEmail(String toEmails, String subject, String content) {
        if (toEmails == null || toEmails.isEmpty()) {
            return;
        }

        String[] emails = toEmails.split("[,;]");
        for (String email : emails) {
            email = email.trim();
            if (!email.isEmpty()) {
                try {
                    sendHtmlEmail(email, subject, content);
                    log.info("批量邮件发送成功: to={}", email);
                } catch (Exception e) {
                    log.error("批量邮件发送失败: to={}, error={}", email, e.getMessage());
                }
            }
        }
    }

    /**
     * 测试邮件发送
     */
    public boolean testEmail(String toEmail) {
        try {
            String subject = "【RPA系统】邮件测试";
            String content = "<html><body><h2>邮件测试成功！</h2><p>这是一封来自RPA系统的测试邮件。</p><p>时间：" +
                    LocalDateTime.now().format(DATE_FORMATTER) + "</p></body></html>";
            sendHtmlEmail(toEmail, subject, content);
            log.info("测试邮件发送成功: to={}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("测试邮件发送失败: to={}, error={}", toEmail, e.getMessage());
            return false;
        }
    }
}
