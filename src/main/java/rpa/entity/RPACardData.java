package rpa.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RPA消息卡片数据实体
 */
@Data
public class RPACardData {

    /** 卡片类型 */
    private String cardType;

    // ==================== 任务卡片 ====================
    private Long taskId;
    private String taskName;
    private String taskStatus;
    private String robotName;
    private String processName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
    private String reportUrl;

    // ==================== 机器人卡片 ====================
    private Long robotId;
    private String robotStatus;
    private String robotIp;
    private Integer cpuUsage;
    private Integer memoryUsage;
    private Integer diskUsage;
    private String alertLevel;  // 用于机器人告警级别
    private String alertMessage;

    // ==================== 流程卡片 ====================
    private Long flowId;
    private String flowName;
    private String flowCategory;
    private Integer flowVersion;
    private String flowStatus;

    // ==================== 日志卡片 ====================
    private Long logId;
    private String logType;
    private LocalDateTime logTime;
    private String logSummary;
    private String screenshotUrl;
    private String recordingUrl;

    // ==================== 审批卡片 ====================
    private Long approvalId;
    private String approvalType;
    private String approvalReason;
    private String approvalStatus;

    // ==================== 告警卡片 ====================
    private Long alertId;
    private String alertTitle;
    private String alertContent;
    private String alertLevelForNotify;  // 用于告警通知的级别
    private LocalDateTime alertTime;

    // 快捷操作按钮
    private List<CardAction> actions;

    /**
     * 卡片操作按钮
     */
    @Data
    public static class CardAction {
        private String action;
        private String label;
        private String confirm;
        private String icon;

        public CardAction() {}

        public CardAction(String action, String label, String confirm, String icon) {
            this.action = action;
            this.label = label;
            this.confirm = confirm;
            this.icon = icon;
        }
    }
}
