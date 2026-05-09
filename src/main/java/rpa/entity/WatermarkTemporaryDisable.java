package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 水印临时关闭记录实体类
 * <p>
 * 记录水印的临时关闭操作，用于审计追溯：
 * - 记录关闭原因
 * - 记录操作人信息
 * - 记录关闭和恢复时间
 * - 记录IP地址
 * </p>
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Data
@Entity
@Table(name = "watermark_temporary_disable")
public class WatermarkTemporaryDisable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
     * 关闭原因（必填）
     */
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /**
     * 关闭开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 计划恢复时间
     */
    @Column(name = "scheduled_restore_time", nullable = false)
    private LocalDateTime scheduledRestoreTime;

    /**
     * 实际恢复时间（null表示尚未恢复）
     */
    @Column(name = "actual_restore_time")
    private LocalDateTime actualRestoreTime;

    /**
     * 关闭时长的分钟数
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

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
     * 关闭状态：ACTIVE-生效中、RESTORED-已恢复、CANCELLED-已取消
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    /**
     * 是否为手动恢复（false表示自动恢复）
     */
    @Column(name = "manual_restore", nullable = false)
    private Boolean manualRestore = false;

    /**
     * 审批人ID（如果需要审批）
     */
    @Column(name = "approved_by")
    private Long approvedBy;

    /**
     * 审批人姓名
     */
    @Column(name = "approved_by_name", length = 100)
    private String approvedByName;

    /**
     * 审批备注
     */
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 哈希值（用于完整性验证）
     */
    @Column(name = "hash", length = 100)
    private String hash;

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 1000)
    private String remark;
}
