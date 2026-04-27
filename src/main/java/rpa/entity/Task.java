package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 任务实体类
 * <p>
 * RPA任务实体，对应数据库中的task表。
 * 存储任务的配置信息、执行状态和结果数据。
 * </p>
 * <p>
 * 任务状态：
 * <ul>
 *   <li>pending - 待分配</li>
 *   <li>assigned - 已分配</li>
 *   <li>running - 执行中</li>
 *   <li>completed - 已完成</li>
 *   <li>failed - 执行失败</li>
 *   <li>cancelled - 已取消</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务名称 */
    @Column(nullable = false)
    private String name;

    /** 任务分类 */
    private String category;
    
    /** 优先级（low-低，normal-普通，high-高，urgent-紧急） */
    private String priority = "normal";
    
    /** 关联的流程ID（单个） */
    private Long processId;

    /** 关联的多个流程ID（JSON格式数组，如 [1,2,3]） */
    @Column(length = 1000)
    private String processIds;

    /** 关联的流程名称（单个） */
    private String processName;

    /** 关联的多个流程名称（JSON格式数组） */
    @Column(length = 2000)
    private String processNames;

    /** 关联的队列ID */
    private Long queueId;

    /** 关联的队列名称 */
    private String queueName;

    /** 追踪ID */
    private String traceId;

    /** 重试次数 */
    private Integer retryCount = 0;

    /** 分配的机器人ID */
    private Long robotId;
    
    /** 分配的机器人名称 */
    private String robotName;
    
    /** 任务接收人ID */
    private Long assigneeId;
    
    /** 任务接收人名称 */
    private String assigneeName;
    
    /** 任务状态 */
    private String status = "pending";
    
    /** 输入数据（JSON格式） */
    @Column(length = 3000)
    private String inputData;
    
    /** 执行结果数据（JSON格式） */
    @Column(length = 5000)
    private String resultData;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 任务备注 */
    @Column(length = 500)
    private String remark;
    
    /** 开始执行时间 */
    private LocalDateTime startTime;
    
    /** 结束执行时间 */
    private LocalDateTime endTime;
    
    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
    
    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 任务创建者ID */
    private Long creatorId;

    /** 任务创建者名称 */
    private String creatorName;

    /** 通知邮箱 - 任务完成/失败时发送通知到此邮箱 */
    @Column(length = 200)
    private String notifyEmail;

    /** 是否发送邮件通知（true-发送，false-不发送） */
    private Boolean emailNotificationEnabled = true;
}
