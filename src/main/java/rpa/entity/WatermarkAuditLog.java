package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 水印审计日志实体类
 * <p>
 * 专门用于记录水印相关的所有操作事件：
 * - 水印篡改检测
 * - 水印临时关闭/恢复
 * - 截图行为记录
 * - 打印行为记录
 * - 导出文件记录
 * </p>
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Data
@Entity
@Table(name = "watermark_audit_log", indexes = {
    @Index(name = "idx_wal_user_time", columnList = "user_id, create_time"),
    @Index(name = "idx_wal_event_type", columnList = "event_type"),
    @Index(name = "idx_wal_create_time", columnList = "create_time")
})
public class WatermarkAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 事件类型：
     * WATERMARK_TAMPERING - 水印篡改检测
     * WATERMARK_DISABLED - 水印临时关闭
     * WATERMARK_ENABLED - 水印恢复启用
     * SCREENSHOT_CAPTURED - 截图操作
     * PRINT_ACTION - 打印操作
     * EXPORT_WITH_WATERMARK - 导出文件
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 用户姓名
     */
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    /**
     * 客户端IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 发生事件的页面URL
     */
    @Column(name = "page_url", length = 500)
    private String pageUrl;

    /**
     * 事件描述
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 篡改原因（仅WATERMARK_TAMPERING类型）
     */
    @Column(name = "tamper_reason", length = 100)
    private String tamperReason;

    /**
     * 篡改详情（JSON格式）
     */
    @Column(name = "tamper_details", columnDefinition = "TEXT")
    private String tamperDetails;

    /**
     * 截图方式（仅SCREENSHOT_CAPTURED类型）
     */
    @Column(name = "screenshot_method", length = 50)
    private String screenshotMethod;

    /**
     * 打印内容类型（仅PRINT_ACTION类型）
     */
    @Column(name = "print_content_type", length = 100)
    private String printContentType;

    /**
     * 导出文件类型（仅EXPORT_WITH_WATERMARK类型）
     */
    @Column(name = "export_file_type", length = 20)
    private String exportFileType;

    /**
     * 导出文件名（仅EXPORT_WITH_WATERMARK类型）
     */
    @Column(name = "export_file_name", length = 255)
    private String exportFileName;

    /**
     * 风险级别：LOW、MEDIUM、HIGH、CRITICAL
     */
    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel = "LOW";

    /**
     * 处理状态：DETECTED-已检测、PENDING-待处理、RESOLVED-已解决、FALSE_POSITIVE-误报
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DETECTED";

    /**
     * 处理人ID
     */
    @Column(name = "handled_by")
    private Long handledBy;

    /**
     * 处理人姓名
     */
    @Column(name = "handled_by_name", length = 100)
    private String handledByName;

    /**
     * 处理时间
     */
    @Column(name = "handled_time")
    private LocalDateTime handledTime;

    /**
     * 处理备注
     */
    @Column(name = "handle_comment", length = 500)
    private String handleComment;

    /**
     * 附加数据（JSON格式）
     */
    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 完整性哈希值
     */
    @Column(name = "hash", length = 100)
    private String hash;

    /**
     * 事件发生时间（精确到毫秒）
     */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    /**
     * 是否已上报监管
     */
    @Column(name = "reported_to_regulator", nullable = false)
    private Boolean reportedToRegulator = false;

    /**
     * 上报监管时间
     */
    @Column(name = "report_time")
    private LocalDateTime reportTime;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

    /**
     * 预持久化操作
     */
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (eventTime == null) {
            eventTime = LocalDateTime.now();
        }
    }
}
