package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 报表订阅配置实体类
 * <p>
 * 存储用户的报表订阅配置，对应数据库中的report_subscription表。
 * 支持定时推送报表到指定接收人，支持灵活的推送时间配置。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "report_subscription")
public class ReportSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 订阅名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 订阅编码（系统生成） */
    @Column(length = 50)
    private String code;

    /** 报表类型（daily/weekly/monthly/robot/roi） */
    @Column(nullable = false, length = 50)
    private String reportType;

    /** 发送频率（daily/weekly/monthly/custom） */
    @Column(nullable = false, length = 20)
    private String frequency;

    /** 推送渠道（email/dingtalk/wecom/feishu）多个用逗号分隔 */
    @Column(nullable = false, length = 100)
    private String channel;

    /** 接收人列表（逗号分隔的邮箱或账号） */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String recipients;

    /** 是否启用 (0-禁用, 1-启用) */
    @Column(nullable = false)
    private Integer enabled = 1;

    // ==================== 推送时间配置 ====================

    /** 推送时间类型（fixed-固定时间，custom-自定义时间） */
    @Column(length = 20)
    private String scheduleType = "fixed";

    /** 固定推送时间（HH:mm格式） */
    @Column(length = 10)
    private String fixedTime = "09:00";

    /** 自定义Cron表达式（高级用户使用） */
    @Column(length = 100)
    private String cronExpression;

    /** 推送星期（1-7，逗号分隔，如"1,2,3,4,5"表示工作日） */
    @Column(length = 20)
    private String weekdays;

    /** 推送月份（1-12，逗号分隔，如"1,3,6,9,12"表示特定月份） */
    @Column(length = 50)
    private String months;

    /** 推送日期（1-31，逗号分隔，用于月报） */
    @Column(length = 100)
    private String monthDays;

    /** 时区 */
    @Column(length = 50)
    private String timezone = "Asia/Shanghai";

    // ==================== 订阅审批流程 ====================

    /** 是否需要审批 (0-否, 1-是) */
    private Integer requireApproval = 0;

    /** 审批状态（pending-待审批，approved-已批准，rejected-已拒绝） */
    @Column(length = 20)
    private String approvalStatus = "approved";

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名 */
    @Column(length = 100)
    private String approverName;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 审批备注 */
    @Column(length = 500)
    private String approvalRemark;

    // ==================== 订阅来源 ====================

    /** 订阅来源（user-用户创建，system-系统预置） */
    @Column(length = 20)
    private String source = "user";

    /** 敏感级别（normal-普通，sensitive-敏感） */
    @Column(length = 20)
    private String sensitivityLevel = "normal";

    // ==================== 通知内容配置 ====================

    /** 通知模板ID */
    private Long templateId;

    /** 是否包含附件 (0-否, 1-是) */
    private Integer includeAttachment = 1;

    /** 附件类型（log-日志，summary-汇总，full-完整报告） */
    @Column(length = 20)
    private String attachmentType = "log";

    // ==================== 统计信息 ====================

    /** 创建人ID */
    @Column(nullable = false)
    private Long createUser;

    /** 创建人姓名 */
    @Column(length = 100)
    private String createUserName;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 最后发送时间 */
    private LocalDateTime lastSendTime;

    /** 最后发送状态（success/failed） */
    @Column(length = 20)
    private String lastSendStatus;

    /** 发送成功次数 */
    private Integer successCount = 0;

    /** 发送失败次数 */
    private Integer failedCount = 0;

    /** 更新人ID */
    private Long updateUser;

    /** 更新时间 */
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = 1;
        }
        if (requireApproval == null) {
            requireApproval = 0;
        }
        if (approvalStatus == null) {
            approvalStatus = "approved";
        }
        if (includeAttachment == null) {
            includeAttachment = 1;
        }
        if (code == null || code.isEmpty()) {
            // 生成订阅编码
            code = "SUB_" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
