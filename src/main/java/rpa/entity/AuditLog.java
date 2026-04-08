package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * <p>
 * 记录全模块用户操作，实现全链路可追溯审计。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作模块 */
    @Column(nullable = false, length = 50)
    private String module;

    /** 操作类型 */
    @Column(nullable = false, length = 50)
    private String action;

    /** 操作对象类型 */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /** 操作对象ID */
    @Column(name = "target_id")
    private Long targetId;

    /** 操作对象名称 */
    @Column(name = "target_name", length = 200)
    private String targetName;

    /** 操作描述 */
    @Column(length = 500)
    private String description;

    /** 风险等级（low-低，medium-中，high-高） */
    @Column(name = "risk_level", length = 20)
    private String riskLevel = "low";

    /** 操作人ID */
    @Column(name = "user_id")
    private Long userId;

    /** 操作人名称 */
    @Column(name = "user_name", length = 100)
    private String userName;

    /** 操作IP地址 */
    @Column(length = 50)
    private String ip;

    /** 请求参数 */
    @Column(columnDefinition = "TEXT")
    private String requestParams;

    /** 响应结果 */
    @Column(columnDefinition = "TEXT")
    private String responseResult;

    /** 执行状态（success-成功，failed-失败） */
    @Column(length = 20)
    private String status = "success";

    /** 哈希值（用于完整性验证） */
    @Column(length = 64)
    private String hash;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
}
