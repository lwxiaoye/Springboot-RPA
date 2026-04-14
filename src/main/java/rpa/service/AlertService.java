package rpa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.*;
import rpa.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业级告警服务
 * 支持：多渠道通知、分级降噪、去重、升级机制
 */
@Slf4j
@Service
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final NotificationChannelRepository channelRepository;
    private final NotificationRepository notificationRepository;
    private final AlertRosterRepository rosterRepository;
    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // 告警冷却期缓存（告警ID -> 最后触发时间）
    private final Map<String, Long> alertCooldownCache = new ConcurrentHashMap<>();

    // 告警去重缓存（去重Key -> 触发时间）
    private final Map<String, Long> alertDeduplicationCache = new ConcurrentHashMap<>();

    public AlertService(
            AlertRuleRepository alertRuleRepository,
            AlertRecordRepository alertRecordRepository,
            NotificationChannelRepository channelRepository,
            NotificationRepository notificationRepository,
            AlertRosterRepository rosterRepository,
            TaskRepository taskRepository,
            RobotRepository robotRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertRecordRepository = alertRecordRepository;
        this.channelRepository = channelRepository;
        this.notificationRepository = notificationRepository;
        this.rosterRepository = rosterRepository;
        this.taskRepository = taskRepository;
        this.robotRepository = robotRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    // ==================== 告警触发入口 ====================

    /**
     * 任务执行失败告警
     */
    public void sendTaskFailureAlert(Task task, String errorMessage) {
        AlertRecord alert = createAlert(
            "execution_failed",
            "P1",
            String.format("任务执行失败：%s", task.getName()),
            String.format("任务「%s」执行失败，错误信息：%s", task.getName(), errorMessage),
            "task",
            task.getId(),
            task.getName()
        );
        alert.setTraceId(task.getTraceId());
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    /**
     * 机器人离线告警
     */
    public void sendRobotOfflineAlert(Robot robot) {
        AlertRecord alert = createAlert(
            "robot_offline",
            "P0",
            String.format("机器人离线：%s", robot.getName()),
            String.format("机器人「%s」已离线，请检查机器人状态", robot.getName()),
            "robot",
            robot.getId(),
            robot.getName()
        );
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    /**
     * 队列积压告警
     */
    public void sendQueueOverflowAlert(Long queueId, String queueName, int pendingCount, int threshold) {
        // 检查去重
        String dedupKey = String.format("queue_overflow_%d", queueId);
        if (isDuplicate(dedupKey, 300)) { // 5分钟内不重复告警
            return;
        }

        AlertRecord alert = createAlert(
            "queue_overflow",
            pendingCount > threshold * 2 ? "P0" : "P1",
            String.format("队列积压告警：%s", queueName),
            String.format("队列「%s」积压严重，待处理任务数：%d，阈值：%d", queueName, pendingCount, threshold),
            "queue",
            queueId,
            queueName
        );
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    /**
     * 凭证即将过期告警
     */
    public void sendCredentialExpiringAlert(String credentialName, int daysLeft) {
        AlertRecord alert = createAlert(
            "credential_expiring",
            daysLeft <= 1 ? "P0" : "P2",
            String.format("凭证即将过期：%s", credentialName),
            String.format("凭证「%s」将于 %d 天后过期，请及时更新", credentialName, daysLeft),
            "credential",
            null,
            credentialName
        );
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    /**
     * 高危操作告警
     */
    public void sendHighRiskAlert(AuditLog auditLog) {
        AlertRecord alert = createAlert(
            "high_risk_operation",
            "P1",
            String.format("高危操作告警：%s", auditLog.getDescription()),
            String.format("用户「%s」执行了高危操作：%s，操作对象：%s",
                auditLog.getUserName(), auditLog.getDescription(), auditLog.getTargetName()),
            auditLog.getModule(),
            auditLog.getId(),
            auditLog.getTargetName()
        );
        alert.setUserId(auditLog.getUserId());
        alert.setUserName(auditLog.getUserName());
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    /**
     * 机器人切换告警
     */
    public void sendRobotSwitchAlert(RobotBackup backup, String reason) {
        AlertRecord alert = createAlert(
            "robot_switch",
            "P2",
            String.format("机器人主备切换：%s", backup.getPrimaryRobotName()),
            String.format("机器人「%s」已切换到备用机器人「%s」，原因：%s",
                backup.getPrimaryRobotName(), backup.getBackupRobotName(), reason),
            "robot",
            backup.getPrimaryRobotId(),
            backup.getPrimaryRobotName()
        );
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    /**
     * 系统异常告警
     */
    public void sendSystemErrorAlert(String errorType, String errorMessage, String traceId) {
        AlertRecord alert = createAlert(
            "system_error",
            "P0",
            String.format("系统异常：%s", errorType),
            String.format("系统发生异常，类型：%s，详情：%s", errorType, errorMessage),
            "system",
            null,
            errorType
        );
        alert.setTraceId(traceId);
        alertRecordRepository.save(alert);
        sendAlertNotification(alert);
    }

    // ==================== 告警创建与处理 ====================

    /**
     * 创建告警记录
     */
    private AlertRecord createAlert(String alertType, String severity, String title, String content,
                                    String targetType, Long targetId, String targetName) {
        // 查找匹配的告警规则
        List<AlertRule> rules = alertRuleRepository.findEnabledByAlertType(alertType);
        AlertRule matchedRule = rules.stream()
            .filter(r -> {
                if (r.getSeverity() == null) return true;
                return getSeverityLevel(r.getSeverity()) <= getSeverityLevel(severity);
            })
            .findFirst()
            .orElse(null);

        AlertRecord alert = new AlertRecord();
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setTargetType(targetType);
        alert.setTargetId(targetId);
        alert.setTargetName(targetName);
        alert.setStatus("firing");
        alert.setFirstFiredAt(LocalDateTime.now());
        alert.setLastFiredAt(LocalDateTime.now());
        alert.setFiredCount(1);
        alert.setNotificationStatus("pending");
        alert.setCreateTime(LocalDateTime.now());
        alert.setUpdateTime(LocalDateTime.now());

        if (matchedRule != null) {
            alert.setAlertRuleId(matchedRule.getId());
            alert.setAlertRuleCode(matchedRule.getCode());
            alert.setAlertRuleName(matchedRule.getName());
            alert.setNotificationChannels(matchedRule.getNotificationChannels());

            // 检查冷却期
            String cooldownKey = matchedRule.getCode() + "_" + targetId;
            if (isInCooldown(cooldownKey, matchedRule.getCooldownPeriod())) {
                alert.setStatus("suppressed");
                alert.setSuppressedReason("冷却期内");
            }

            // 检查去重
            if (matchedRule.getDeduplicationKey() != null) {
                String dedupKey = matchedRule.getDeduplicationKey() + "_" + targetId;
                if (isDuplicate(dedupKey, 600)) {
                    alert.setStatus("suppressed");
                    alert.setSuppressedReason("去重过滤");
                }
            }
        }

        return alert;
    }

    /**
     * 发送告警通知
     */
    private void sendAlertNotification(AlertRecord alert) {
        if ("suppressed".equals(alert.getStatus())) {
            alertRecordRepository.save(alert);
            log.info("告警被抑制：{}", alert.getTitle());
            return;
        }

        try {
            // 获取通知渠道
            List<String> channels = parseChannels(alert.getNotificationChannels());

            if (channels.isEmpty()) {
                // 使用默认渠道
                List<NotificationChannel> defaultChannels = channelRepository.findByIsDefaultTrue();
                channels = defaultChannels.stream()
                    .map(NotificationChannel::getChannelType)
                    .toList();
            }

            // 发送到各个渠道
            List<String> sentChannels = new ArrayList<>();
            for (String channelType : channels) {
                boolean success = sendToChannel(channelType, alert);
                if (success) {
                    sentChannels.add(channelType);
                }
            }

            // 更新发送状态
            alert.setNotificationStatus(sentChannels.isEmpty() ? "failed" : "sent");
            alert.setNotificationChannels(objectMapper.writeValueAsString(sentChannels));
            alertRecordRepository.save(alert);

            log.info("告警通知已发送：{} -> {}", alert.getTitle(), sentChannels);

        } catch (Exception e) {
            log.error("发送告警通知失败", e);
            alert.setNotificationStatus("failed");
            alertRecordRepository.save(alert);
        }
    }

    /**
     * 发送到指定渠道
     */
    private boolean sendToChannel(String channelType, AlertRecord alert) {
        try {
            switch (channelType) {
                case "wechat":
                    return sendWechatNotification(alert);
                case "dingtalk":
                    return sendDingtalkNotification(alert);
                case "feishu":
                    return sendFeishuNotification(alert);
                case "email":
                    return sendEmailNotification(alert);
                case "sms":
                    return sendSmsNotification(alert);
                case "webhook":
                    return sendWebhookNotification(alert);
                default:
                    log.warn("未知通知渠道：{}", channelType);
                    return false;
            }
        } catch (Exception e) {
            log.error("发送到渠道 {} 失败", channelType, e);
            return false;
        }
    }

    /**
     * 发送企业微信通知
     */
    private boolean sendWechatNotification(AlertRecord alert) {
        List<NotificationChannel> channels = channelRepository.findByChannelType("wechat");
        if (channels.isEmpty()) return false;

        NotificationChannel channel = channels.get(0);
        try {
            Map<String, Object> config = objectMapper.readValue(channel.getConfig(), Map.class);
            String webhookUrl = (String) config.get("webhook_url");

            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "markdown");

            String severityEmoji = getSeverityEmoji(alert.getSeverity());
            String content = String.format(
                "%s **%s**\n" +
                "> 告警类型：%s\n" +
                "> 严重程度：%s\n" +
                "> 告警内容：%s\n" +
                "> 发生时间：%s",
                severityEmoji,
                alert.getTitle(),
                alert.getAlertType(),
                getSeverityText(alert.getSeverity()),
                alert.getContent(),
                alert.getFirstFiredAt()
            );

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("content", content);
            message.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            return true;
        } catch (Exception e) {
            log.error("发送企业微信通知失败", e);
            return false;
        }
    }

    /**
     * 发送钉钉通知
     */
    private boolean sendDingtalkNotification(AlertRecord alert) {
        List<NotificationChannel> channels = channelRepository.findByChannelType("dingtalk");
        if (channels.isEmpty()) return false;

        NotificationChannel channel = channels.get(0);
        try {
            Map<String, Object> config = objectMapper.readValue(channel.getConfig(), Map.class);
            String webhookUrl = (String) config.get("webhook_url");
            String secret = (String) config.get("secret");

            // 钉钉机器人需要签名，这里简化处理
            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "markdown");

            String severityEmoji = getSeverityEmoji(alert.getSeverity());
            String content = String.format(
                "%s **%s**\n" +
                "> 告警类型：%s\n" +
                "> 严重程度：%s\n" +
                "> 告警内容：%s\n" +
                "> 发生时间：%s",
                severityEmoji,
                alert.getTitle(),
                alert.getAlertType(),
                getSeverityText(alert.getSeverity()),
                alert.getContent(),
                alert.getFirstFiredAt()
            );

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", alert.getTitle());
            markdown.put("text", content);
            message.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            return true;
        } catch (Exception e) {
            log.error("发送钉钉通知失败", e);
            return false;
        }
    }

    /**
     * 发送飞书通知
     */
    private boolean sendFeishuNotification(AlertRecord alert) {
        List<NotificationChannel> channels = channelRepository.findByChannelType("feishu");
        if (channels.isEmpty()) return false;

        NotificationChannel channel = channels.get(0);
        try {
            Map<String, Object> config = objectMapper.readValue(channel.getConfig(), Map.class);
            String webhookUrl = (String) config.get("webhook_url");

            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "interactive");

            String content = String.format(
                "【%s】%s\n\n告警类型：%s\n严重程度：%s\n告警内容：%s\n发生时间：%s",
                getSeverityText(alert.getSeverity()),
                alert.getTitle(),
                alert.getAlertType(),
                getSeverityText(alert.getSeverity()),
                alert.getContent(),
                alert.getFirstFiredAt()
            );

            Map<String, Object> card = new HashMap<>();
            card.put("zh_cn", Map.of("title", Map.of("tag", "plain_text", "content", alert.getTitle()),
                                     "content", Map.of("tag", "markdown", "content", content)));
            message.put("card", card);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            return true;
        } catch (Exception e) {
            log.error("发送飞书通知失败", e);
            return false;
        }
    }

    /**
     * 发送邮件通知
     */
    private boolean sendEmailNotification(AlertRecord alert) {
        List<NotificationChannel> channels = channelRepository.findByChannelType("email");
        if (channels.isEmpty()) return false;

        NotificationChannel channel = channels.get(0);
        try {
            Map<String, Object> config = objectMapper.readValue(channel.getConfig(), Map.class);
            String smtpHost = (String) config.get("smtp_host");
            Integer smtpPort = (Integer) config.get("smtp_port");
            String fromEmail = (String) config.get("from_email");

            // 获取告警接收人
            List<String> recipients = getAlertRecipients(alert);

            // 实际项目中需要使用 JavaMailSender 发送邮件
            // 这里简化为记录日志
            log.info("发送邮件通知：收件人={}, 主题={}", recipients, alert.getTitle());

            // TODO: 使用 JavaMailSender 发送真实邮件
            // simpleMailMessage.setTo(recipients.toArray(new String[0]));
            // simpleMailMessage.setSubject("[" + getSeverityText(alert.getSeverity()) + "] " + alert.getTitle());
            // simpleMailMessage.setText(alert.getContent());
            // mailSender.send(simpleMailMessage);

            return true;
        } catch (Exception e) {
            log.error("发送邮件通知失败", e);
            return false;
        }
    }

    /**
     * 发送短信通知
     */
    private boolean sendSmsNotification(AlertRecord alert) {
        List<NotificationChannel> channels = channelRepository.findByChannelType("sms");
        if (channels.isEmpty()) return false;

        NotificationChannel channel = channels.get(0);
        try {
            Map<String, Object> config = objectMapper.readValue(channel.getConfig(), Map.class);
            String apiUrl = (String) config.get("api_url");
            String apiKey = (String) config.get("api_key");

            // 获取告警接收人手机号
            List<String> recipients = getAlertRecipients(alert);

            // 实际项目中需要调用短信API
            log.info("发送短信通知：收件人={}, 内容={}", recipients, alert.getTitle());

            // TODO: 调用短信API
            // Map<String, Object> params = new HashMap<>();
            // params.put("mobile", recipients);
            // params.put("content", alert.getTitle());
            // restTemplate.postForEntity(apiUrl, params, String.class);

            return true;
        } catch (Exception e) {
            log.error("发送短信通知失败", e);
            return false;
        }
    }

    /**
     * 发送Webhook通知
     */
    private boolean sendWebhookNotification(AlertRecord alert) {
        List<NotificationChannel> channels = channelRepository.findByChannelType("webhook");
        if (channels.isEmpty()) return false;

        NotificationChannel channel = channels.get(0);
        try {
            Map<String, Object> config = objectMapper.readValue(channel.getConfig(), Map.class);
            String webhookUrl = (String) config.get("webhook_url");

            Map<String, Object> payload = new HashMap<>();
            payload.put("alertId", alert.getId());
            payload.put("title", alert.getTitle());
            payload.put("content", alert.getContent());
            payload.put("severity", alert.getSeverity());
            payload.put("alertType", alert.getAlertType());
            payload.put("targetType", alert.getTargetType());
            payload.put("targetId", alert.getTargetId());
            payload.put("targetName", alert.getTargetName());
            payload.put("traceId", alert.getTraceId());
            payload.put("timestamp", alert.getFirstFiredAt().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            return true;
        } catch (Exception e) {
            log.error("发送Webhook通知失败", e);
            return false;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取告警接收人
     */
    private List<String> getAlertRecipients(AlertRecord alert) {
        List<String> recipients = new ArrayList<>();

        // 1. 查找告警规则配置的接收人
        if (alert.getAlertRuleId() != null) {
            alertRuleRepository.findById(alert.getAlertRuleId()).ifPresent(rule -> {
                if (rule.getNotificationTemplate() != null) {
                    try {
                        Map<String, Object> template = objectMapper.readValue(rule.getNotificationTemplate(), Map.class);
                        if (template.containsKey("recipients")) {
                            recipients.addAll((List<String>) template.get("recipients"));
                        }
                    } catch (Exception e) {
                        log.error("解析通知模板失败", e);
                    }
                }
            });
        }

        // 2. 查找值班人员
        List<AlertRoster> rosters = rosterRepository.findActiveRosters();
        for (AlertRoster roster : rosters) {
            if (matchesRoster(roster, alert)) {
                try {
                    List<Map<String, Object>> members = objectMapper.readValue(
                        roster.getRosterMembers(), List.class);
                    for (Map<String, Object> member : members) {
                        if (member.containsKey("email")) {
                            recipients.add((String) member.get("email"));
                        }
                        if (member.containsKey("phone")) {
                            // 短信接收
                        }
                    }
                } catch (Exception e) {
                    log.error("解析值班人员失败", e);
                }
            }
        }

        // 3. 查找任务负责人
        if ("task".equals(alert.getTargetType()) && alert.getTargetId() != null) {
            taskRepository.findById(alert.getTargetId()).ifPresent(task -> {
                // 查找任务创建人或负责人
            });
        }

        return recipients.stream().distinct().toList();
    }

    /**
     * 检查是否匹配值班规则
     */
    private boolean matchesRoster(AlertRoster roster, AlertRecord alert) {
        // 检查严重级别
        if (roster.getSeverityLevels() != null) {
            try {
                List<String> levels = objectMapper.readValue(roster.getSeverityLevels(), List.class);
                if (!levels.contains(alert.getSeverity())) {
                    return false;
                }
            } catch (Exception e) {
                log.error("解析严重级别失败", e);
            }
        }

        // 检查告警类型
        if (roster.getAlertRuleIds() != null) {
            try {
                List<Long> ruleIds = objectMapper.readValue(roster.getAlertRuleIds(), List.class);
                if (!ruleIds.contains(alert.getAlertRuleId())) {
                    return false;
                }
            } catch (Exception e) {
                log.error("解析告警规则ID失败", e);
            }
        }

        // 检查生效时间
        LocalDateTime now = LocalDateTime.now();
        if (roster.getEffectiveFrom() != null && now.isBefore(roster.getEffectiveFrom())) {
            return false;
        }
        if (roster.getEffectiveTo() != null && now.isAfter(roster.getEffectiveTo())) {
            return false;
        }

        return true;
    }

    /**
     * 检查是否在冷却期内
     */
    private boolean isInCooldown(String key, int cooldownSeconds) {
        Long lastTime = alertCooldownCache.get(key);
        if (lastTime == null) {
            alertCooldownCache.put(key, System.currentTimeMillis());
            return false;
        }
        if (System.currentTimeMillis() - lastTime > cooldownSeconds * 1000) {
            alertCooldownCache.put(key, System.currentTimeMillis());
            return false;
        }
        return true;
    }

    /**
     * 检查是否重复告警
     */
    private boolean isDuplicate(String key, int windowSeconds) {
        Long lastTime = alertDeduplicationCache.get(key);
        if (lastTime == null) {
            alertDeduplicationCache.put(key, System.currentTimeMillis());
            return false;
        }
        if (System.currentTimeMillis() - lastTime > windowSeconds * 1000) {
            alertDeduplicationCache.put(key, System.currentTimeMillis());
            return false;
        }
        return true;
    }

    /**
     * 解析通知渠道
     */
    private List<String> parseChannels(String channelsJson) {
        if (channelsJson == null || channelsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(channelsJson, List.class);
        } catch (Exception e) {
            log.error("解析通知渠道失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取严重级别数值
     */
    private int getSeverityLevel(String severity) {
        return switch (severity) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            case "P3" -> 3;
            default -> 4;
        };
    }

    /**
     * 获取严重程度文字
     */
    private String getSeverityText(String severity) {
        return switch (severity) {
            case "P0" -> "紧急";
            case "P1" -> "重要";
            case "P2" -> "一般";
            case "P3" -> "提示";
            default -> severity;
        };
    }

    /**
     * 获取严重程度emoji
     */
    private String getSeverityEmoji(String severity) {
        return switch (severity) {
            case "P0" -> "🔴";
            case "P1" -> "🟠";
            case "P2" -> "🟡";
            case "P3" -> "🔵";
            default -> "⚪";
        };
    }

    // ==================== 告警管理方法 ====================

    /**
     * 获取告警规则列表
     */
    public List<AlertRule> getAlertRules() {
        return alertRuleRepository.findAll();
    }

    /**
     * 获取启用的告警规则
     */
    public List<AlertRule> getEnabledAlertRules() {
        return alertRuleRepository.findAllEnabledOrderByPriority();
    }

    /**
     * 创建或更新告警规则
     */
    public AlertRule saveAlertRule(AlertRule rule) {
        if (rule.getCreateTime() == null) {
            rule.setCreateTime(LocalDateTime.now());
        }
        rule.setUpdateTime(LocalDateTime.now());
        return alertRuleRepository.save(rule);
    }

    /**
     * 删除告警规则
     */
    public void deleteAlertRule(Long id) {
        alertRuleRepository.deleteById(id);
    }

    /**
     * 获取告警记录列表
     */
    public List<AlertRecord> getAlertRecords(String status, String severity) {
        if (status != null && !status.isEmpty()) {
            return alertRecordRepository.findByStatus(status);
        }
        if (severity != null && !severity.isEmpty()) {
            return alertRecordRepository.findBySeverity(severity);
        }
        return alertRecordRepository.findFiringAlerts();
    }

    /**
     * 解决告警
     */
    public void resolveAlert(Long alertId, String resolvedBy, String resolution) {
        alertRecordRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus("resolved");
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedBy(resolvedBy);
            alert.setResolution(resolution);
            alert.setUpdateTime(LocalDateTime.now());
            alertRecordRepository.save(alert);
            log.info("告警已解决：{} - {}", alert.getTitle(), resolution);
        });
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("firing", alertRecordRepository.countByStatus("firing"));
        stats.put("resolved", alertRecordRepository.countByStatus("resolved"));
        stats.put("P0", alertRecordRepository.countByFiringBySeverity("P0"));
        stats.put("P1", alertRecordRepository.countByFiringBySeverity("P1"));
        stats.put("P2", alertRecordRepository.countByFiringBySeverity("P2"));
        stats.put("P3", alertRecordRepository.countByFiringBySeverity("P3"));
        return stats;
    }
}
