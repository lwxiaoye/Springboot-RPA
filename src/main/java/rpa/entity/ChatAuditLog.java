package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天审计日志实体
 */
@Data
@Entity
@Table(name = "chat_audit_log")
public class ChatAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话ID */
    private Long conversationId;

    /** 消息ID */
    private Long messageId;

    /** 操作用户ID */
    private Long userId;

    /** 操作用户姓名 */
    @Column(length = 100)
    private String userName;

    /** IP地址 */
    @Column(length = 50)
    private String userIp;

    /** 设备信息 */
    @Column(length = 200)
    private String userDevice;

    /** 操作类型 */
    @Column(length = 50)
    private String action;

    /** 操作详情 */
    @Column(length = 500)
    private String actionDetail;

    /** 旧值(修改前) */
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    /** 新值(修改后) */
    @Column(columnDefinition = "TEXT")
    private String newValue;

    /** 关联类型: task/robot/flow */
    @Column(length = 50)
    private String relatedType;

    /** 关联ID */
    private Long relatedId;

    /** 结果: success/failed */
    @Column(length = 20)
    private String result = "success";

    /** 错误信息 */
    @Column(length = 500)
    private String errorMessage;

    /** 操作时间 */
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (result == null) result = "success";
    }
}
