package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知模板实体类
 * <p>
 * 存储通知消息的模板配置，支持HTML模板和变量替换。
 * 对应数据库中的 notification_template 表。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 模板名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 模板编码（唯一标识） */
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    /** 模板类型（task_complete-任务完成, task_failed-任务失败, subscription-订阅, alert-告警, custom-自定义） */
    @Column(nullable = false, length = 30)
    private String type;

    /** 模板内容（HTML格式，支持变量替换） */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 模板描述 */
    @Column(length = 500)
    private String description;

    /** 是否启用 (0-禁用, 1-启用) */
    @Column(nullable = false)
    private Integer enabled = 1;

    /** 是否为默认模板 (0-否, 1-是) */
    @Column(nullable = false)
    private Integer isDefault = 0;

    /** 支持的渠道（email, dingtalk, wecom, feishu, sms）多个用逗号分隔 */
    @Column(length = 100)
    private String channels = "email";

    /** 主题模板（邮件主题/钉钉标题等） */
    @Column(length = 200)
    private String subjectTemplate;

    /** 创建者ID */
    private Long creatorId;

    /** 创建者名称 */
    @Column(length = 50)
    private String creatorName;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 使用次数统计 */
    private Integer useCount = 0;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = 1;
        }
        if (isDefault == null) {
            isDefault = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
