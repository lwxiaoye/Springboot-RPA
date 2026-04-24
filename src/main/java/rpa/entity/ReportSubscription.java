package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 报表订阅配置实体类
 * <p>
 * 存储用户的报表订阅配置，对应数据库中的report_subscription表。
 * 支持定时推送报表到指定接收人。
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

    /** 报表类型（daily/weekly/monthly/robot/roi） */
    @Column(nullable = false, length = 50)
    private String reportType;

    /** 发送频率（daily/weekly/monthly） */
    @Column(nullable = false, length = 20)
    private String frequency;

    /** 推送渠道（email/dingtalk/wecom） */
    @Column(nullable = false, length = 20)
    private String channel;

    /** 接收人列表（逗号分隔的邮箱或账号） */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String recipients;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

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
            enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
