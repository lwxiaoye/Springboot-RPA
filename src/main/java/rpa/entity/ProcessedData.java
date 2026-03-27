package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 加工数据实体类
 * <p>
 * 加工后的数据，对应数据库中的processed_data表。
 * 存储原始数据、加工结果和数据来源追溯信息。
 * </p>
 * <p>
 * 数据库索引：
 * <ul>
 *   <li>idx_process_id - 按加工任务查询</li>
 *   <li>idx_collect_id - 按采集来源查询</li>
 *   <li>idx_source_type - 按数据类型筛选</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "processed_data", indexes = {
    @Index(name = "idx_process_id", columnList = "processId"),
    @Index(name = "idx_collect_id", columnList = "collectId"),
    @Index(name = "idx_source_type", columnList = "sourceType")
})
public class ProcessedData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 数据名称 */
    @Column(nullable = false)
    private String name;

    /** 关联的加工任务ID */
    private Long processId;

    /** 关联的加工任务名称 */
    private String processName;

    /** 关联的采集任务ID */
    private Long collectId;

    /** 关联的采集任务名称 */
    private String collectName;

    /** 原始数据（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String sourceData;

    /** 加工后的数据（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String processedData;

    /** 数据来源URL */
    private String sourceUrl;

    /** 数据来源类型 */
    private String sourceType;

    /** 数据分类/标签 */
    private String dataCategory;

    /** 状态（0-处理中，1-已完成） */
    private Integer status = 0;

    /** 加工时间 */
    private LocalDateTime processTime = LocalDateTime.now();

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
}
