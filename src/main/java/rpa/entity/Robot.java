package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 机器人实体类
 * <p>
 * RPA机器人实体，对应数据库中的robot表。
 * 存储机器人的基本信息、运行状态和资源使用情况。
 * </p>
 * <p>
 * 机器人状态：
 * <ul>
 *   <li>idle - 空闲，可接受任务</li>
 *   <li>busy - 忙碌，正在执行任务</li>
 *   <li>offline - 离线，无法连接</li>
 * </ul>
 * </p>
 * <p>
 * 机器人分类：
 * <ul>
 *   <li>DATA_COLLECT - 数据采集机器人</li>
 *   <li>DATA_PARSE - 数据解析机器人</li>
 *   <li>DATA_PROCESS - 数据加工机器人</li>
 *   <li>GENERAL - 通用执行机器人</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "robot")
public class Robot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 机器人名称，唯一 */
    @Column(unique = true, nullable = false)
    private String name;

    /** 机器人分类（DATA_COLLECT-数据采集，DATA_PARSE-数据解析，DATA_PROCESS-数据加工，GENERAL-通用执行） */
    @Column(name = "robot_category")
    private String robotCategory = "GENERAL";
    
    /** 状态（idle-空闲，busy-忙碌，offline-离线） */
    private String status = "idle";

    /** 机器人能力描述 */
    @Column(length = 1000)
    private String capabilities;

    /** 机器人IP地址 */
    @Column(name = "ip", length = 50)
    private String ip;

    /** 主机名 */
    @Column(name = "hostname", length = 100)
    private String hostname;

    /** 服务端口，默认8080 */
    private Integer port = 8080;

    /** CPU使用率（百分比） */
    @Column(name = "cpu_usage")
    private Integer cpuUsage = 0;

    /** 内存使用率（百分比） */
    @Column(name = "memory_usage")
    private Integer memoryUsage = 0;

    /** 机器人描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 绑定的流程ID */
    @Column(name = "bound_process_id")
    private Long boundProcessId;
    
    /** 绑定的流程名称 */
    @Column(name = "bound_process_name", length = 200)
    private String boundProcessName;
    
    /** 机器人执行代码 */
    @Column(name = "robot_code", columnDefinition = "TEXT")
    private String robotCode;
    
    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat = LocalDateTime.now();
    
    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
}
