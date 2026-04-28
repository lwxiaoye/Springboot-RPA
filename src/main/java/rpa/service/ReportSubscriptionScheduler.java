package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rpa.entity.ReportSubscription;
import rpa.repository.ReportSubscriptionRepository;
import rpa.service.ReportAnalyticsService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 报表订阅定时任务服务
 * <p>
 * 负责定时发送报表订阅邮件，包括：
 * - 日报：每天早上9点发送
 * - 周报：每周一早上9点发送
 * - 月报：每月1号早上9点发送
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSubscriptionScheduler {

    private final ReportSubscriptionRepository subscriptionRepository;
    private final ReportAnalyticsService reportService;
    private final EmailNotificationService emailService;

    /**
     * 每天早上9点执行日报订阅任务
     * <p>
     * cron: 0 0 9 * * ? - 每天9点执行
     * </p>
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailySubscriptions() {
        log.info("开始执行日报订阅任务...");

        try {
            // 查询所有日报订阅
            List<ReportSubscription> dailySubscriptions = subscriptionRepository.findAll().stream()
                    .filter(sub -> "daily".equals(sub.getReportType()))
                    .filter(sub -> sub.getEnabled() != null && sub.getEnabled() == 1)
                    .toList();

            log.info("找到 {} 个日报订阅", dailySubscriptions.size());

            for (ReportSubscription subscription : dailySubscriptions) {
                try {
                    sendDailyReport(subscription);
                } catch (Exception e) {
                    log.error("发送日报订阅失败: subscriptionId={}, error={}",
                            subscription.getId(), e.getMessage());
                }
            }

            log.info("日报订阅任务执行完成");
        } catch (Exception e) {
            log.error("日报订阅任务执行失败", e);
        }
    }

    /**
     * 每周一早上9点执行周报订阅任务
     * <p>
     * cron: 0 0 9 ? * MON - 每周一9点执行
     * </p>
     */
    @Scheduled(cron = "0 0 9 ? * MON")
    public void sendWeeklySubscriptions() {
        log.info("开始执行周报订阅任务...");

        try {
            List<ReportSubscription> weeklySubscriptions = subscriptionRepository.findAll().stream()
                    .filter(sub -> "weekly".equals(sub.getReportType()))
                    .filter(sub -> sub.getEnabled() != null && sub.getEnabled() == 1)
                    .toList();

            log.info("找到 {} 个周报订阅", weeklySubscriptions.size());

            for (ReportSubscription subscription : weeklySubscriptions) {
                try {
                    sendWeeklyReport(subscription);
                } catch (Exception e) {
                    log.error("发送周报订阅失败: subscriptionId={}, error={}",
                            subscription.getId(), e.getMessage());
                }
            }

            log.info("周报订阅任务执行完成");
        } catch (Exception e) {
            log.error("周报订阅任务执行失败", e);
        }
    }

    /**
     * 每月1号早上9点执行月报订阅任务
     * <p>
     * cron: 0 0 9 1 * ? - 每月1号9点执行
     * </p>
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void sendMonthlySubscriptions() {
        log.info("开始执行月报订阅任务...");

        try {
            List<ReportSubscription> monthlySubscriptions = subscriptionRepository.findAll().stream()
                    .filter(sub -> "monthly".equals(sub.getReportType()))
                    .filter(sub -> sub.getEnabled() != null && sub.getEnabled() == 1)
                    .toList();

            log.info("找到 {} 个月报订阅", monthlySubscriptions.size());

            for (ReportSubscription subscription : monthlySubscriptions) {
                try {
                    sendMonthlyReport(subscription);
                } catch (Exception e) {
                    log.error("发送月报订阅失败: subscriptionId={}, error={}",
                            subscription.getId(), e.getMessage());
                }
            }

            log.info("月报订阅任务执行完成");
        } catch (Exception e) {
            log.error("月报订阅任务执行失败", e);
        }
    }

    /**
     * 手动触发所有订阅（用于测试）
     */
    public void triggerAllSubscriptions() {
        log.info("手动触发所有订阅任务...");

        // 触发日报订阅
        sendDailySubscriptions();

        // 如果是周一，触发周报订阅
        if (LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY) {
            sendWeeklySubscriptions();
        }

        // 如果是每月1号，触发月报订阅
        if (LocalDate.now().getDayOfMonth() == 1) {
            sendMonthlySubscriptions();
        }
    }

    /**
     * 手动触发指定类型的订阅
     *
     * @param reportType 报表类型（daily/weekly/monthly）
     */
    public void triggerSubscriptionsByType(String reportType) {
        log.info("手动触发订阅: reportType={}", reportType);

        switch (reportType) {
            case "daily" -> sendDailySubscriptions();
            case "weekly" -> sendWeeklySubscriptions();
            case "monthly" -> sendMonthlySubscriptions();
            default -> log.warn("未知的报表类型: {}", reportType);
        }
    }

    // ==================== 发送各类报表 ====================

    /**
     * 发送日报
     */
    private void sendDailyReport(ReportSubscription subscription) {
        String today = LocalDate.now().toString();
        Map<String, Object> reportData = reportService.getDailyStats(today);

        String reportContent = buildDailyReportContent(reportData);
        String period = today;

        emailService.sendReportSummaryEmail(
                subscription.getRecipients(),
                subscription.getName(),
                reportContent,
                period
        );

        // 更新最后发送时间
        subscription.setLastSendTime(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("日报已发送: subscriptionId={}, recipients={}",
                subscription.getId(), subscription.getRecipients());
    }

    /**
     * 发送周报
     */
    private void sendWeeklyReport(ReportSubscription subscription) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        String period = weekStart + " 至 " + weekEnd;

        // 获取本周数据
        Map<String, Object> weeklyData = reportService.getMonthlyStats(
                today.toString().substring(0, 7));

        String reportContent = buildWeeklyReportContent(weeklyData);

        emailService.sendReportSummaryEmail(
                subscription.getRecipients(),
                subscription.getName(),
                reportContent,
                "第" + today.get(java.time.temporal.WeekFields.ISO.weekOfYear()) + "周"
        );

        // 更新最后发送时间
        subscription.setLastSendTime(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("周报已发送: subscriptionId={}, recipients={}, period={}",
                subscription.getId(), subscription.getRecipients(), period);
    }

    /**
     * 发送月报
     */
    private void sendMonthlyReport(ReportSubscription subscription) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.toString().substring(0, 7);

        Map<String, Object> monthlyData = reportService.getMonthlyStats(yearMonth);

        String reportContent = buildMonthlyReportContent(monthlyData);
        String period = today.getYear() + "年" + today.getMonthValue() + "月";

        emailService.sendReportSummaryEmail(
                subscription.getRecipients(),
                subscription.getName(),
                reportContent,
                period
        );

        // 更新最后发送时间
        subscription.setLastSendTime(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("月报已发送: subscriptionId={}, recipients={}, period={}",
                subscription.getId(), subscription.getRecipients(), period);
    }

    // ==================== 报表内容构建 ====================

    /**
     * 构建日报内容
     */
    private String buildDailyReportContent(Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif;'>");

        // 概览统计
        html.append("<h3 style='color: #409eff; margin-bottom: 15px;'>📊 今日执行概览</h3>");
        html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        html.append("<tr style='background: #f5f7fa;'>");
        html.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>指标</th>");
        html.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: right;'>数值</th>");
        html.append("</tr>");

        addTableRow(html, "总执行次数", String.valueOf(data.getOrDefault("total", 0)));
        addTableRow(html, "成功次数", String.valueOf(data.getOrDefault("success", 0)));
        addTableRow(html, "失败次数", String.valueOf(data.getOrDefault("failed", 0)));
        addTableRow(html, "进行中", String.valueOf(data.getOrDefault("running", 0)));

        html.append("</table>");

        // 计算成功率
        long total = ((Number) data.getOrDefault("total", 0)).longValue();
        long success = ((Number) data.getOrDefault("success", 0)).longValue();
        double successRate = total > 0 ? (double) success / total * 100 : 0;

        html.append("<div style='background: #e8f4fd; padding: 15px; border-radius: 8px;'>");
        html.append("<strong>执行成功率：</strong>");
        html.append("<span style='color: #67c23a; font-size: 18px;'>")
                .append(String.format("%.1f", successRate)).append("%</span>");
        html.append("</div>");

        html.append("</div>");
        return html.toString();
    }

    /**
     * 构建周报内容
     */
    private String buildWeeklyReportContent(Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif;'>");

        html.append("<h3 style='color: #67c23a; margin-bottom: 15px;'>📊 本周执行汇总</h3>");

        // 统计卡片
        html.append("<div style='display: flex; gap: 15px; margin-bottom: 20px;'>");
        html.append("<div style='flex: 1; background: #f0f9eb; padding: 15px; border-radius: 8px; text-align: center;'>");
        html.append("<div style='font-size: 24px; font-weight: bold; color: #67c23a;'>")
                .append(data.getOrDefault("totalExecutions", 0)).append("</div>");
        html.append("<div style='color: #666;'>总执行次数</div>");
        html.append("</div>");

        html.append("<div style='flex: 1; background: #ecf5ff; padding: 15px; border-radius: 8px; text-align: center;'>");
        html.append("<div style='font-size: 24px; font-weight: bold; color: #409eff;'>")
                .append(data.getOrDefault("successCount", 0)).append("</div>");
        html.append("<div style='color: #666;'>成功次数</div>");
        html.append("</div>");

        html.append("<div style='flex: 1; background: #fef0f0; padding: 15px; border-radius: 8px; text-align: center;'>");
        html.append("<div style='font-size: 24px; font-weight: bold; color: #f56c6c;'>")
                .append(data.getOrDefault("failedCount", 0)).append("</div>");
        html.append("<div style='color: #666;'>失败次数</div>");
        html.append("</div>");
        html.append("</div>");

        // 详细数据
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr style='background: #f5f7fa;'>");
        html.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>指标</th>");
        html.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: right;'>数值</th>");
        html.append("</tr>");

        addTableRow(html, "数据采集总量", formatNumber(data.getOrDefault("totalData", 0)));
        addTableRow(html, "日均采集", formatNumber(data.getOrDefault("dailyAvg", 0)));
        addTableRow(html, "峰值日采集", formatNumber(data.getOrDefault("peakData", 0)));
        addTableRow(html, "采集成功率", data.getOrDefault("dataSuccessRate", 0) + "%");

        html.append("</table>");
        html.append("</div>");
        return html.toString();
    }

    /**
     * 构建月报内容
     */
    private String buildMonthlyReportContent(Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif;'>");

        html.append("<h3 style='color: #e6a23c; margin-bottom: 15px;'>📊 本月执行汇总</h3>");

        // 关键指标卡片
        html.append("<div style='display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; margin-bottom: 20px;'>");

        // 总执行
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 8px; color: white;'>");
        html.append("<div style='font-size: 28px; font-weight: bold;'>").append(data.getOrDefault("totalExecutions", 0)).append("</div>");
        html.append("<div style='opacity: 0.9;'>总执行次数</div>");
        html.append("</div>");

        // 成功率
        double successRate = ((Number) data.getOrDefault("successRate", 0)).doubleValue();
        html.append("<div style='background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 20px; border-radius: 8px; color: white;'>");
        html.append("<div style='font-size: 28px; font-weight: bold;'>").append(String.format("%.1f", successRate)).append("%</div>");
        html.append("<div style='opacity: 0.9;'>执行成功率</div>");
        html.append("</div>");

        // 数据采集
        html.append("<div style='background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); padding: 20px; border-radius: 8px; color: white;'>");
        html.append("<div style='font-size: 28px; font-weight: bold;'>").append(formatNumber(data.getOrDefault("totalData", 0))).append("</div>");
        html.append("<div style='opacity: 0.9;'>数据采集总量</div>");
        html.append("</div>");

        // 日均采集
        html.append("<div style='background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); padding: 20px; border-radius: 8px; color: white;'>");
        html.append("<div style='font-size: 28px; font-weight: bold;'>").append(formatNumber(data.getOrDefault("dailyAvg", 0))).append("</div>");
        html.append("<div style='opacity: 0.9;'>日均采集</div>");
        html.append("</div>");

        html.append("</div>");

        // 详细统计表
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr style='background: #f5f7fa;'>");
        html.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: left;'>指标</th>");
        html.append("<th style='padding: 10px; border: 1px solid #ddd; text-align: right;'>数值</th>");
        html.append("</tr>");

        addTableRow(html, "成功次数", String.valueOf(data.getOrDefault("successCount", 0)));
        addTableRow(html, "失败次数", String.valueOf(data.getOrDefault("failedCount", 0)));
        addTableRow(html, "峰值日采集", formatNumber(data.getOrDefault("peakData", 0)));
        addTableRow(html, "采集成功率", data.getOrDefault("dataSuccessRate", 0) + "%");

        html.append("</table>");
        html.append("</div>");
        return html.toString();
    }

    // ==================== 辅助方法 ====================

    /**
     * 添加表格行
     */
    private void addTableRow(StringBuilder html, String label, String value) {
        html.append("<tr>");
        html.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(label).append("</td>");
        html.append("<td style='padding: 10px; border: 1px solid #ddd; text-align: right; font-weight: bold;'>")
                .append(value).append("</td>");
        html.append("</tr>");
    }

    /**
     * 格式化数字（添加千分位）
     */
    private String formatNumber(Object num) {
        if (num == null) return "0";
        long value = ((Number) num).longValue();
        return String.format("%,d", value);
    }

    // ==================== 灵活推送时间支持 ====================

    /**
     * 检查是否应该发送订阅（基于灵活时间配置）
     */
    public boolean shouldSendSubscription(ReportSubscription subscription) {
        // 如果是固定时间，使用默认逻辑
        if (!"custom".equals(subscription.getScheduleType())) {
            return true;
        }

        // 检查Cron表达式
        if (subscription.getCronExpression() != null && !subscription.getCronExpression().isEmpty()) {
            return evaluateCronExpression(subscription.getCronExpression());
        }

        // 检查工作日
        if (subscription.getWeekdays() != null && !subscription.getWeekdays().isEmpty()) {
            int currentDayOfWeek = LocalDate.now().getDayOfWeek().getValue();
            String[] weekdays = subscription.getWeekdays().split(",");
            boolean match = false;
            for (String day : weekdays) {
                if (Integer.parseInt(day.trim()) == currentDayOfWeek) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        // 检查月份
        if (subscription.getMonths() != null && !subscription.getMonths().isEmpty()) {
            int currentMonth = LocalDate.now().getMonthValue();
            String[] months = subscription.getMonths().split(",");
            boolean match = false;
            for (String month : months) {
                if (Integer.parseInt(month.trim()) == currentMonth) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        // 检查日期
        if (subscription.getMonthDays() != null && !subscription.getMonthDays().isEmpty()) {
            int currentDayOfMonth = LocalDate.now().getDayOfMonth();
            String[] days = subscription.getMonthDays().split(",");
            boolean match = false;
            for (String day : days) {
                if (Integer.parseInt(day.trim()) == currentDayOfMonth) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        return true;
    }

    /**
     * 简单的Cron表达式评估（支持基本的格式）
     */
    private boolean evaluateCronExpression(String cron) {
        // 支持格式: 秒 分 时 日 月 周
        // 例如: 0 30 9 * * ?  表示每天9:30
        String[] parts = cron.split("\\s+");
        if (parts.length < 5) return true;

        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        // 检查小时
        if (!"*".equals(parts[2])) {
            int hour = now.getHour();
            if (!isMatch(cron, parts[2], hour)) return false;
        }

        // 检查分钟
        if (!"*".equals(parts[1])) {
            int minute = now.getMinute();
            if (!isMatch(cron, parts[1], minute)) return false;
        }

        return true;
    }

    private boolean isMatch(String cron, String field, int value) {
        if ("*".equals(field)) return true;

        // 通配符
        if (field.contains("/")) {
            String[] parts = field.split("/");
            int start = "*".equals(parts[0]) ? 0 : Integer.parseInt(parts[0]);
            int step = Integer.parseInt(parts[1]);
            return (value - start) % step == 0;
        }

        // 范围
        if (field.contains("-")) {
            String[] parts = field.split("-");
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            return value >= min && value <= max;
        }

        // 列表
        if (field.contains(",")) {
            String[] parts = field.split(",");
            for (String p : parts) {
                if (Integer.parseInt(p.trim()) == value) return true;
            }
            return false;
        }

        // 单值
        return Integer.parseInt(field) == value;
    }

    /**
     * 获取订阅的推送时间描述
     */
    public String getScheduleDescription(ReportSubscription subscription) {
        if (!"custom".equals(subscription.getScheduleType())) {
            return "每天 " + (subscription.getFixedTime() != null ? subscription.getFixedTime() : "09:00");
        }

        StringBuilder desc = new StringBuilder();

        if (subscription.getCronExpression() != null && !subscription.getCronExpression().isEmpty()) {
            desc.append("Cron: ").append(subscription.getCronExpression());
        }

        if (subscription.getWeekdays() != null && !subscription.getWeekdays().isEmpty()) {
            desc.append(" 星期: ").append(subscription.getWeekdays());
        }

        if (subscription.getMonthDays() != null && !subscription.getMonthDays().isEmpty()) {
            desc.append(" 日期: ").append(subscription.getMonthDays());
        }

        if (subscription.getFixedTime() != null) {
            desc.append(" 时间: ").append(subscription.getFixedTime());
        }

        return desc.length() > 0 ? desc.toString() : "未配置";
    }
}
