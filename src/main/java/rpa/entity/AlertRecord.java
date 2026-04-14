package rpa.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_record")
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_rule_id")
    private Long alertRuleId;

    @Column(name = "alert_rule_code", length = 50)
    private String alertRuleCode;

    @Column(name = "alert_rule_name", length = 100)
    private String alertRuleName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Column(name = "trigger_value", length = 200)
    private String triggerValue;

    @Column(name = "threshold_value", length = 100)
    private String thresholdValue;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "status", length = 20)
    private String status = "firing";

    @Column(name = "suppressed_reason", length = 200)
    private String suppressedReason;

    @Column(name = "first_fired_at")
    private LocalDateTime firstFiredAt;

    @Column(name = "last_fired_at")
    private LocalDateTime lastFiredAt;

    @Column(name = "fired_count")
    private Integer firedCount = 1;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolution", length = 500)
    private String resolution;

    @Column(name = "notification_status", length = 20)
    private String notificationStatus = "pending";

    @Column(name = "notification_channels", columnDefinition = "TEXT")
    private String notificationChannels;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public AlertRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAlertRuleId() { return alertRuleId; }
    public void setAlertRuleId(Long alertRuleId) { this.alertRuleId = alertRuleId; }
    public String getAlertRuleCode() { return alertRuleCode; }
    public void setAlertRuleCode(String alertRuleCode) { this.alertRuleCode = alertRuleCode; }
    public String getAlertRuleName() { return alertRuleName; }
    public void setAlertRuleName(String alertRuleName) { this.alertRuleName = alertRuleName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDetailUrl() { return detailUrl; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public String getTriggerValue() { return triggerValue; }
    public void setTriggerValue(String triggerValue) { this.triggerValue = triggerValue; }
    public String getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(String thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSuppressedReason() { return suppressedReason; }
    public void setSuppressedReason(String suppressedReason) { this.suppressedReason = suppressedReason; }
    public LocalDateTime getFirstFiredAt() { return firstFiredAt; }
    public void setFirstFiredAt(LocalDateTime firstFiredAt) { this.firstFiredAt = firstFiredAt; }
    public LocalDateTime getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(LocalDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }
    public Integer getFiredCount() { return firedCount; }
    public void setFiredCount(Integer firedCount) { this.firedCount = firedCount; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getNotificationStatus() { return notificationStatus; }
    public void setNotificationStatus(String notificationStatus) { this.notificationStatus = notificationStatus; }
    public String getNotificationChannels() { return notificationChannels; }
    public void setNotificationChannels(String notificationChannels) { this.notificationChannels = notificationChannels; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
