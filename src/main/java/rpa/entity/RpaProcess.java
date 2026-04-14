package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * RPA流程实体类
 * <p>
 * RPA自动化流程定义，对应数据库中的rpa_process表。
 * 存储流程的基本信息、步骤定义和版本信息。
 * </p>
 * <p>
 * 流程状态：
 * <ul>
 *   <li>draft - 草稿</li>
 *   <li>active - 激活</li>
 *   <li>paused - 暂停</li>
 *   <li>archived - 归档</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "rpa_process")
public class RpaProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 流程名称 */
    @Column(nullable = false)
    private String name;

    /** 流程编码 */
    private String code;

    /** 流程描述 */
    @Column(length = 2000)
    private String description;

    /** 状态（draft-草稿，active-激活，paused-暂停，archived-归档） */
    private String status = "active";

    /** 流程步骤定义（JSON格式） */
    @Column(length = 5000)
    private String steps;

    /** 创建者ID */
    private Long creatorId;
    
    /** 创建者名称 */
    private String creatorName;

    /** 版本号 */
    private String version = "1.0.0";

    /** 已执行任务数 */
    private Integer taskCount = 0;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    // ==================== 流程-机器人关联 ====================

    /** 选择的机器人ID列表（JSON格式数组，流程可调用多个机器人） */
    @Column(name = "selected_robot_ids", length = 1000)
    private String selectedRobotIds;

    /** 选择的机器人名称列表（JSON格式数组） */
    @Column(name = "selected_robot_names", length = 1000)
    private String selectedRobotNames;

    /** 最后一次执行的机器人ID */
    @Column(name = "last_robot_id")
    private Long lastRobotId;

    /** 最后一次执行的机器人名称 */
    @Column(name = "last_robot_name", length = 200)
    private String lastRobotName;

    /** 最后一次执行的返回结果 */
    @Column(name = "last_execution_result", columnDefinition = "TEXT")
    private String lastExecutionResult;

    /** 最后执行时间 */
    private LocalDateTime lastExecutionTime;

    // ==================== 流程扩展字段 ====================

    /** 需要的机器人分类（DATA_COLLECT-数据采集，DATA_PARSE-数据解析，DATA_PROCESS-数据加工，GENERAL-通用执行） */
    @Column(name = "required_category", length = 50)
    private String requiredCategory;

    /** 关联的队列ID */
    @Column(name = "queue_id")
    private Long queueId;

    /** 关联的队列名称 */
    @Column(name = "queue_name", length = 200)
    private String queueName;

    /** 超时时间（分钟） */
    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes = 60;

    /** 重��次数 */
    @Column(name = "retry_count")
    private Integer retryCount = 0;
}
