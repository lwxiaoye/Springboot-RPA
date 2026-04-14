package rpa.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rule")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "condition_config", columnDefinition = "TEXT", nullable = false)
    private String conditionConfig;

    @Column(name = "condition_expr", columnDefinition = "TEXT")
    private String conditionExpr;

    @Column(name = "threshold_value", length = 100)
    private String thresholdValue;

    @Column(name = "comparison_type", length = 20)
    private String comparisonType = "gt";

    @Column(name = "check_interval")
    private Integer checkInterval = 60;

    @Column(name = "consecutive_count")
    private Integer consecutiveCount = 1;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "auto_create_incident")
    private Boolean autoCreateIncident = false;

    @Column(name = "notification_channels", columnDefinition = "TEXT")
    private String notificationChannels;

    @Column(name = "notification_template", columnDefinition = "TEXT")
    private String notificationTemplate;

    @Column(name = "cooldown_period")
    private Integer cooldownPeriod = 600;

    @Column(name = "deduplication_key", length = 100)
    private String deduplicationKey;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "creator", length = 100)
    private String creator;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public AlertRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getConditionConfig() { return conditionConfig; }
    public void setConditionConfig(String conditionConfig) { this.conditionConfig = conditionConfig; }
    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String conditionExpr) { this.conditionExpr = conditionExpr; }
    public String getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(String thresholdValue) { this.thresholdValue = thresholdValue; }
    public String getComparisonType() { return comparisonType; }
    public void setComparisonType(String comparisonType) { this.comparisonType = comparisonType; }
    public Integer getCheckInterval() { return checkInterval; }
    public void setCheckInterval(Integer checkInterval) { this.checkInterval = checkInterval; }
    public Integer getConsecutiveCount() { return consecutiveCount; }
    public void setConsecutiveCount(Integer consecutiveCount) { this.consecutiveCount = consecutiveCount; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAutoCreateIncident() { return autoCreateIncident; }
    public void setAutoCreateIncident(Boolean autoCreateIncident) { this.autoCreateIncident = autoCreateIncident; }
    public String getNotificationChannels() { return notificationChannels; }
    public void setNotificationChannels(String notificationChannels) { this.notificationChannels = notificationChannels; }
    public String getNotificationTemplate() { return notificationTemplate; }
    public void setNotificationTemplate(String notificationTemplate) { this.notificationTemplate = notificationTemplate; }
    public Integer getCooldownPeriod() { return cooldownPeriod; }
    public void setCooldownPeriod(Integer cooldownPeriod) { this.cooldownPeriod = cooldownPeriod; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
