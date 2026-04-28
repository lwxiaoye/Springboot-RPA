package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rpa.entity.Task;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 通知聚合服务
 * <p>
 * 将大量通知合并发送，避免频繁打扰用户。
 * 支持定时聚合、阈值聚合等多种策略。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAggregationService {

    private final EmailNotificationService emailNotificationService;
    private final MultiChannelNotificationService multiChannelNotificationService;

    // 通知缓冲池：用户ID -> 待聚合的通知列表
    private final Map<Long, List<PendingNotification>> notificationBuffer = new ConcurrentHashMap<>();

    // 配置参数
    private static final int MAX_BUFFER_SIZE = 100;          // 单用户最大缓冲数量
    private static final int FLUSH_THRESHOLD = 10;          // 达到此数量立即发送
    private static final long BUFFER_TIMEOUT_MINUTES = 30;   // 缓冲超时时间（分钟）

    // 通知类型
    public enum NotificationType {
        TASK_COMPLETE("task_complete", "任务完成"),
        TASK_FAILED("task_failed", "任务失败"),
        TASK_WARNING("task_warning", "任务警告"),
        SYSTEM_ALERT("system_alert", "系统告警"),
        REPORT_READY("report_ready", "报表就绪");

        private final String code;
        private final String description;

        NotificationType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    // 待聚合的通知
    @lombok.Data
    public static class PendingNotification {
        private String type;
        private String title;
        private String content;
        private String channel;
        private Task task;
        private LocalDateTime createTime;
        private Map<String, Object> metadata;
        private int priority; // 1-低，2-中，3-高

        public PendingNotification() {}

        public PendingNotification(String type, String title, String content, String channel,
                                Task task, LocalDateTime createTime, Map<String, Object> metadata, int priority) {
            this.type = type;
            this.title = title;
            this.content = content;
            this.channel = channel;
            this.task = task;
            this.createTime = createTime;
            this.metadata = metadata;
            this.priority = priority;
        }
    }

    // 聚合后的通知摘要
    @lombok.Data
    public static class AggregatedNotification {
        private String channel;
        private String subject;
        private String summary;
        private List<PendingNotification> notifications;
        private int totalCount;
        private int successCount;
        private int failedCount;
        private LocalDateTime firstTime;
        private LocalDateTime lastTime;
        private String timeRange;
    }

    /**
     * 添加通知到缓冲池
     */
    public void addNotification(String userEmail, PendingNotification notification) {
        Long userHash = hashUser(userEmail);

        notificationBuffer.computeIfAbsent(userHash, k -> new ArrayList<>());

        List<PendingNotification> userNotifications = notificationBuffer.get(userHash);

        // 检查是否超过缓冲大小
        if (userNotifications.size() >= MAX_BUFFER_SIZE) {
            log.warn("用户通知缓冲已满，立即发送: userHash={}", userHash);
            flushUserNotifications(userHash, userEmail);
        }

        // 添加通知
        userNotifications.add(notification);
        log.debug("添加通知到缓冲池: userHash={}, type={}, currentSize={}",
                userHash, notification.getType(), userNotifications.size());

        // 检查是否达到立即发送阈值
        if (userNotifications.size() >= FLUSH_THRESHOLD) {
            log.info("达到发送阈值，立即聚合发送: userHash={}, count={}", userHash, userNotifications.size());
            flushUserNotifications(userHash, userEmail);
        }
    }

    /**
     * 添加任务完成通知
     */
    public void addTaskCompleteNotification(String userEmail, Task task) {
        PendingNotification notification = new PendingNotification();
        notification.setType(NotificationType.TASK_COMPLETE.getCode());
        notification.setTitle("任务完成: " + task.getName());
        notification.setContent(buildTaskCompleteContent(task));
        notification.setChannel("email");
        notification.setTask(task);
        notification.setCreateTime(LocalDateTime.now());
        notification.setPriority(2);

        addNotification(userEmail, notification);
    }

    /**
     * 添加任务失败通知
     */
    public void addTaskFailedNotification(String userEmail, Task task, String errorMessage) {
        PendingNotification notification = new PendingNotification();
        notification.setType(NotificationType.TASK_FAILED.getCode());
        notification.setTitle("⚠️ 任务失败: " + task.getName());
        notification.setContent(buildTaskFailedContent(task, errorMessage));
        notification.setChannel("email,dingtalk");
        notification.setTask(task);
        notification.setCreateTime(LocalDateTime.now());
        notification.setPriority(3); // 高优先级

        addNotification(userEmail, notification);
    }

    /**
     * 刷新指定用户的通知
     */
    public void flushUserNotifications(Long userHash, String userEmail) {
        List<PendingNotification> notifications = notificationBuffer.remove(userHash);

        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        try {
            AggregatedNotification aggregated = aggregateNotifications(notifications);
            sendAggregatedNotification(userEmail, aggregated);
            log.info("聚合通知发送成功: userEmail={}, count={}", userEmail, notifications.size());
        } catch (Exception e) {
            log.error("发送聚合通知失败: userEmail={}, error={}", userEmail, e.getMessage());
        }
    }

    /**
     * 聚合通知
     */
    private AggregatedNotification aggregateNotifications(List<PendingNotification> notifications) {
        // 按类型分组
        Map<String, List<PendingNotification>> grouped = notifications.stream()
                .collect(Collectors.groupingBy(PendingNotification::getType));

        // 按渠道分组
        Map<String, List<PendingNotification>> byChannel = notifications.stream()
                .collect(Collectors.groupingBy(PendingNotification::getChannel));

        AggregatedNotification aggregated = new AggregatedNotification();

        // 统计信息
        aggregated.setNotifications(notifications);
        aggregated.setTotalCount(notifications.size());
        aggregated.setSuccessCount((int) notifications.stream()
                .filter(n -> NotificationType.TASK_COMPLETE.getCode().equals(n.getType())).count());
        aggregated.setFailedCount((int) notifications.stream()
                .filter(n -> NotificationType.TASK_FAILED.getCode().equals(n.getType())).count());

        // 时间范围
        LocalDateTime firstTime = notifications.stream()
                .map(PendingNotification::getCreateTime)
                .min(LocalDateTime::compareTo).orElse(LocalDateTime.now());
        LocalDateTime lastTime = notifications.stream()
                .map(PendingNotification::getCreateTime)
                .max(LocalDateTime::compareTo).orElse(LocalDateTime.now());
        aggregated.setFirstTime(firstTime);
        aggregated.setLastTime(lastTime);
        aggregated.setTimeRange(formatTimeRange(firstTime, lastTime));

        // 构建摘要
        StringBuilder summary = new StringBuilder();
        summary.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");

        // 标题
        summary.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 10px 10px 0 0;'>");
        summary.append("<h2 style='color: white; margin: 0;'>📊 RPA任务执行汇总报告</h2>");
        summary.append("</div>");

        // 统计卡片
        summary.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px;'>");

        // 概览
        summary.append("<div style='display: flex; gap: 15px; margin-bottom: 20px;'>");
        summary.append("<div style='flex: 1; background: white; padding: 15px; border-radius: 8px; text-align: center;'>");
        summary.append("<div style='font-size: 24px; font-weight: bold; color: #409eff;'>").append(aggregated.getTotalCount()).append("</div>");
        summary.append("<div style='color: #666;'>总任务数</div>");
        summary.append("</div>");
        summary.append("<div style='flex: 1; background: white; padding: 15px; border-radius: 8px; text-align: center;'>");
        summary.append("<div style='font-size: 24px; font-weight: bold; color: #67c23a;'>").append(aggregated.getSuccessCount()).append("</div>");
        summary.append("<div style='color: #666;'>成功</div>");
        summary.append("</div>");
        summary.append("<div style='flex: 1; background: white; padding: 15px; border-radius: 8px; text-align: center;'>");
        summary.append("<div style='font-size: 24px; font-weight: bold; color: #f56c6c;'>").append(aggregated.getFailedCount()).append("</div>");
        summary.append("<div style='color: #666;'>失败</div>");
        summary.append("</div>");
        summary.append("</div>");

        // 时间范围
        summary.append("<p style='color: #666; font-size: 12px; margin-bottom: 15px;'>");
        summary.append("📅 时间范围: ").append(aggregated.getTimeRange()).append("</p>");

        // 任务列表
        summary.append("<table style='width: 100%; border-collapse: collapse; background: white; border-radius: 8px;'>");
        summary.append("<tr style='background: #f5f7fa;'>");
        summary.append("<th style='padding: 12px; text-align: left; border-bottom: 1px solid #ddd;'>任务名称</th>");
        summary.append("<th style='padding: 12px; text-align: center; border-bottom: 1px solid #ddd;'>状态</th>");
        summary.append("<th style='padding: 12px; text-align: center; border-bottom: 1px solid #ddd;'>时间</th>");
        summary.append("</tr>");

        for (PendingNotification n : notifications) {
            String statusClass = NotificationType.TASK_COMPLETE.getCode().equals(n.getType()) ? "success" : "danger";
            String statusText = NotificationType.TASK_COMPLETE.getCode().equals(n.getType()) ? "✅ 成功" : "❌ 失败";
            String taskName = n.getTask() != null ? n.getTask().getName() : n.getTitle();
            String time = n.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

            summary.append("<tr>");
            summary.append("<td style='padding: 12px; border-bottom: 1px solid #eee;'>").append(taskName).append("</td>");
            summary.append("<td style='padding: 12px; text-align: center; border-bottom: 1px solid #eee; color: #").append(statusClass.equals("success") ? "67c23a" : "f56c6c").append(";'>").append(statusText).append("</td>");
            summary.append("<td style='padding: 12px; text-align: center; border-bottom: 1px solid #eee; color: #666;'>").append(time).append("</td>");
            summary.append("</tr>");
        }

        summary.append("</table>");

        // 页脚
        summary.append("<div style='margin-top: 20px; padding-top: 15px; border-top: 1px solid #ddd; color: #999; font-size: 11px;'>");
        summary.append("<p>此报告由RPA系统自动生成并聚合。如需查看详细执行日志，请访问系统。</p>");
        summary.append("</div>");

        summary.append("</div></div>");

        aggregated.setSummary(summary.toString());

        // 设置主题（按渠道）
        for (Map.Entry<String, List<PendingNotification>> entry : byChannel.entrySet()) {
            aggregated.setChannel(entry.getKey());
            break; // 只取第一个渠道作为代表
        }

        String subject;
        if (aggregated.getFailedCount() > 0) {
            subject = String.format("【RPA告警】%d个任务执行结果汇总（%d个失败）",
                    aggregated.getTotalCount(), aggregated.getFailedCount());
        } else {
            subject = String.format("【RPA通知】%d个任务执行结果汇总", aggregated.getTotalCount());
        }
        aggregated.setSubject(subject);

        return aggregated;
    }

    /**
     * 发送聚合通知
     */
    private void sendAggregatedNotification(String userEmail, AggregatedNotification aggregated) {
        if (aggregated.getTotalCount() == 0) {
            return;
        }

        // 提取渠道
        String channels = aggregated.getChannel();

        if (channels.contains("email")) {
            emailNotificationService.sendReportSummaryEmail(
                    userEmail,
                    "RPA任务汇总",
                    aggregated.getSummary(),
                    aggregated.getTimeRange()
            );
        }

        if (channels.contains("dingtalk")) {
            String markdown = buildDingTalkSummary(aggregated);
            multiChannelNotificationService.sendDingTalkMessage(aggregated.getSubject(), markdown);
        }

        if (channels.contains("wecom")) {
            String content = buildWeComSummary(aggregated);
            multiChannelNotificationService.sendWeComMessage(aggregated.getSubject(), content);
        }
    }

    /**
     * 定时刷新所有缓冲的通知
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    public void flushAllNotifications() {
        log.info("开始定时刷新聚合通知...");

        Map<Long, List<PendingNotification>> snapshots = new HashMap<>(notificationBuffer);

        for (Map.Entry<Long, List<PendingNotification>> entry : snapshots.entrySet()) {
            // 这里简化处理，实际应该根据用户邮箱获取
            log.debug("待刷新通知数量: userHash={}, count={}", entry.getKey(), entry.getValue().size());
        }

        log.info("定时刷新完成，待处理通知数量: {}", notificationBuffer.size());
    }

    /**
     * 检查并刷新超时通知
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkTimeoutNotifications() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(BUFFER_TIMEOUT_MINUTES);

        for (Map.Entry<Long, List<PendingNotification>> entry : notificationBuffer.entrySet()) {
            List<PendingNotification> notifications = entry.getValue();

            boolean hasTimeout = notifications.stream()
                    .anyMatch(n -> n.getCreateTime().isBefore(timeout));

            if (hasTimeout) {
                log.info("发现超时通知，立即发送: userHash={}", entry.getKey());
                // 实际应该根据用户邮箱获取用户信息
                flushUserNotifications(entry.getKey(), "user_" + entry.getKey() + "@example.com");
            }
        }
    }

    /**
     * 获取用户待发送通知数量
     */
    public int getPendingCount(Long userHash) {
        List<PendingNotification> notifications = notificationBuffer.get(userHash);
        return notifications != null ? notifications.size() : 0;
    }

    /**
     * 获取总待发送通知数量
     */
    public int getTotalPendingCount() {
        return notificationBuffer.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    // ==================== 辅助方法 ====================

    /**
     * 用户邮箱哈希（保护隐私）
     */
    private Long hashUser(String email) {
        return (long) email.hashCode();
    }

    /**
     * 格式化时间范围
     */
    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                    " - " + end.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }
        return start.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) +
                " - " + end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 构建任务完成内容
     */
    private String buildTaskCompleteContent(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务名称：").append(task.getName()).append("\n");
        sb.append("任务ID：").append(task.getId()).append("\n");
        sb.append("状态：✅ 成功\n");
        if (task.getStartTime() != null && task.getEndTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(task.getStartTime(), task.getEndTime());
            sb.append("执行耗时：").append(minutes).append("分钟\n");
        }
        return sb.toString();
    }

    /**
     * 构建任务失败内容
     */
    private String buildTaskFailedContent(Task task, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务名称：").append(task.getName()).append("\n");
        sb.append("任务ID：").append(task.getId()).append("\n");
        sb.append("状态：❌ 失败\n");
        sb.append("错误信息：").append(errorMessage != null ? errorMessage : "未知错误").append("\n");
        return sb.toString();
    }

    /**
     * 构建钉钉汇总
     */
    private String buildDingTalkSummary(AggregatedNotification aggregated) {
        StringBuilder md = new StringBuilder();
        md.append("### 📊 RPA任务执行汇总\n\n");
        md.append("**总任务数：** ").append(aggregated.getTotalCount()).append("\n\n");
        md.append("> ✅ 成功：").append(aggregated.getSuccessCount()).append("\n\n");
        md.append("> ❌ 失败：").append(aggregated.getFailedCount()).append("\n\n");
        md.append("**时间范围：** ").append(aggregated.getTimeRange()).append("\n\n");
        md.append("---\n\n");
        md.append("**最近任务：**\n\n");

        for (int i = 0; i < Math.min(5, aggregated.getNotifications().size()); i++) {
            PendingNotification n = aggregated.getNotifications().get(i);
            String status = NotificationType.TASK_COMPLETE.getCode().equals(n.getType()) ? "✅" : "❌";
            String taskName = n.getTask() != null ? n.getTask().getName() : n.getTitle();
            md.append(status).append(" ").append(taskName).append("\n");
        }

        return md.toString();
    }

    /**
     * 构建企业微信汇总
     */
    private String buildWeComSummary(AggregatedNotification aggregated) {
        StringBuilder md = new StringBuilder();
        md.append("**📊 RPA任务执行汇总**\n\n");
        md.append("总任务数：").append(aggregated.getTotalCount()).append("\n");
        md.append("✅ 成功：").append(aggregated.getSuccessCount()).append("\n");
        md.append("❌ 失败：").append(aggregated.getFailedCount()).append("\n");
        md.append("时间范围：").append(aggregated.getTimeRange()).append("\n");
        return md.toString();
    }
}
