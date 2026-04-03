package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 执行日志实体类
 * <p>
 * RPA任务执行日志，对应数据库中的execution_log表。
 * 记录任务的执行过程、状态变化和错误信息。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "execution_log")
public class ExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的任务ID */
    private Long taskId;
    
    /** 关联的流程ID */
    private Long processId;
    
    /** 关联的机器人ID */
    private Long robotId;

    /** 任务名称 */
    private String taskName;
    
    /** 机器人名称 */
    private String robotName;

    /** 执行动作描述 */
    private String action;
    
    /** 执行状态 */
    private String status;

    /** 日志消息 */
    @Column(length = 2000)
    private String message;

    /** 开始时间 */
    private LocalDateTime startTime;
    
    /** 结束时间 */
    private LocalDateTime endTime;

    /** 执行耗时（格式：HH:mm:ss） */
    private String duration;

    /** 执行步骤详情（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String steps;
    
    /** 执行结果数据（JSON格式） */
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
}
