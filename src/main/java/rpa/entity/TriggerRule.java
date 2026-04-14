package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 触发器实体类
 * <p>
 * RPA触发器实体，对应数据库中的trigger表。
 * 支持定时触发、文件触发、API触发等。
 * </p>
 * <p>
 * 触发器类型：
 * <ul>
 *   <li>schedule - 定时触发</li>
 *   <li>file - 文件触发</li>
 *   <li>api - API触发</li>
 *   <li>webhook - Webhook触发</li>
 * </ul>
 * </p>
 * <p>
 * 触发器状态：
 * <ul>
 *   <li>active - 启用</li>
 *   <li>paused - 暂停</li>
 *   <li>disabled - 禁用</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "trigger_rule")
public class TriggerRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 触发器名称 */
    @Column(nullable = false)
    private String name;

    /** 触发器编码（唯一） */
    @Column(unique = true, nullable = false)
    private String code;

    /** 触发器描述 */
    @Column(length = 500)
    private String description;

    /** 触发器类型（schedule-定时，file-文件，api-API，webhook-Webhook） */
    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    /** 触发器状态（active-启用，paused-暂停，disabled-禁用） */
    private String status = "active";

    /** 关联的流程ID */
    @Column(name = "process_id")
    private Long processId;

    /** 关联的流程名称 */
    @Column(name = "process_name", length = 200)
    private String processName;

    /** 关联的流程编码 */
    @Column(name = "process_code", length = 100)
    private String processCode;

    /** 关联的队列ID */
    @Column(name = "queue_id")
    private Long queueId;

    /** 关联的队列名称 */
    @Column(name = "queue_name", length = 200)
    private String queueName;

    // ==================== 定时触发配置 ====================

    /** cron表达式（定时触发时使用） */
    @Column(length = 100)
    private String cron;

    /** 触发周期类型（minute-分钟，hour-小时，day-天，week-周，month-月） */
    @Column(name = "schedule_type", length = 20)
    private String scheduleType;

    /** 定时执行时间（HH:mm格式） */
    @Column(name = "schedule_time", length = 10)
    private String scheduleTime;

    /** 定时执行日期（周：1-7，月：1-31） */
    @Column(name = "schedule_days", length = 50)
    private String scheduleDays;

    // ==================== 文件触发配置 ====================

    /** 监控目录（文件触发时使用） */
    @Column(name = "watch_path", length = 500)
    private String watchPath;

    /** 文件匹配规则（glob模式） */
    @Column(name = "file_pattern", length = 200)
    private String filePattern;

    /** 是否监控子目录 */
    @Column(name = "watch_subdirs")
    private Boolean watchSubdirs = false;

    // ==================== API触发配置 ====================

    /** API密钥 */
    @Column(name = "api_key", length = 200)
    private String apiKey;

    /** Webhook URL */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /** 请求方法（POST/GET） */
    @Column(name = "http_method", length = 10)
    private String httpMethod = "POST";

    // ==================== 触发规则 ====================

    /** 触发条件（JSON格式） */
    @Column(name = "trigger_condition", columnDefinition = "TEXT")
    private String triggerCondition;

    /** 触发参数（JSON格式，传递给流程的参数） */
    @Column(name = "trigger_params", columnDefinition = "TEXT")
    private String triggerParams;

    /** 触发后是否自动启动 */
    @Column(name = "auto_start")
    private Boolean autoStart = true;

    /** 最大并发触发数 */
    @Column(name = "max_concurrent")
    private Integer maxConcurrent = 1;

    // ==================== 统计信息 ====================

    /** 累计触发次数 */
    @Column(name = "total_triggers")
    private Long totalTriggers = 0L;

    /** 成功触发次数 */
    @Column(name = "success_triggers")
    private Long successTriggers = 0L;

    /** 失败触发次数 */
    @Column(name = "failed_triggers")
    private Long failedTriggers = 0L;

    /** 最后触发时间 */
    private LocalDateTime lastTriggerTime;

    /** 最后成功时间 */
    private LocalDateTime lastSuccessTime;

    /** 最后失败时间 */
    private LocalDateTime lastFailedTime;

    // ==================== 其他 ====================

    /** 创建人 */
    private String creator;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 是否启用 */
    private Boolean enabled = true;

    /** 备注 */
    @Column(columnDefinition = "TEXT")
    private String remark;
}
