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
}
