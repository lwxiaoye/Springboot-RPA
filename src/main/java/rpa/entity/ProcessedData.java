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
@Table(name = "processed_data")
public class ProcessedData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 数据名称 */
    @Column(name = "name", nullable = false)
    private String name;

    /** 关联的加工任务ID */
    @Column(name = "process_id")
    private Long processId;

    /** 关联的加工任务名称 */
    @Column(name = "process_name", length = 100)
    private String processName;

    /** 关联的采集任务ID */
    @Column(name = "collect_id")
    private Long collectId;

    /** 关联的采集任务名称 */
    @Column(name = "collect_name", length = 100)
    private String collectName;

    /** 原始数据（JSON格式） */
    @Column(name = "source_data", columnDefinition = "TEXT")
    private String sourceData;

    /** 加工后的数据（JSON格式） */
    @Column(name = "processed_data", columnDefinition = "TEXT")
    private String processedData;

    /** 数据来源URL */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /** 数据来源类型 */
    @Column(name = "source_type", length = 50)
    private String sourceType;

    /** 数据分类/标签 */
    @Column(name = "data_category", length = 100)
    private String dataCategory;

    /** 状态（0-处理中，1-已完成） */
    @Column(name = "status")
    private Integer status = 0;

    /** 加工时间 */
    @Column(name = "process_time")
    private LocalDateTime processTime = LocalDateTime.now();

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
