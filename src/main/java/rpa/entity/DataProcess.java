package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据加工配置实体类
 * <p>
 * 数据加工任务配置，对应数据库中的data_process表。
 * 定义数据加工的来源、规则和输出方式。
 * </p>
 * <p>
 * 加工规则支持：
 * <ul>
 *   <li>filter - 过滤规则，不满足条件的数据被过滤</li>
 *   <li>transform - 转换规则，支持{value}占位符替换</li>
 *   <li>mapping - 字段映射，重命名字段</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "data_process")
public class DataProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 加工任务名称 */
    @Column(nullable = false)
    private String name;

    /** 数据来源ID列表（逗号分隔） */
    private String sourceIds;

    /** 加工类型（transform-转换，filter-过滤，aggregate-聚合） */
    private String processType;

    /** 加工规则定义（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String processRules;

    /** 输出目标表名 */
    private String outputTable;

    /** 状态（0-禁用，1-启用，2-运行中） */
    private Integer status = 0;

    /** 已加工数据条数 */
    private Integer processedCount = 0;

    /** 最后加工时间戳 */
    private Long lastProcessTime;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
    
    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();
}
