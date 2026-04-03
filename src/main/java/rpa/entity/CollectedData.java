package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 采集数据实体类
 * <p>
 * 采集到的原始数据，对应数据库中的collected_data表。
 * 存储采集任务的原始数据、解析状态和数据来源信息。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "collected_data")
public class CollectedData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的采集任务ID */
    @Column(name = "collect_id")
    private Long collectId;
    
    /** 关联的采集任务名称 */
    @Column(name = "collect_name", length = 100)
    private String collectName;

    /** 原始数据（JSON格式） */
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    /** 解析后的结构化数据（JSON格式） */
    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData;

    /** 数据类型（web/api/dynamic） */
    @Column(name = "data_type", length = 20)
    private String dataType;

    /** 来源URL */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /** 解析状态（0-未解析，1-已解析，2-解析失败） */
    @Column(name = "parse_status")
    private Integer parseStatus = 0;

    /** 采集时间 */
    @Column(name = "collect_time")
    private LocalDateTime collectTime = LocalDateTime.now();
    
    /** 解析时间 */
    @Column(name = "parse_time")
    private LocalDateTime parseTime;
}
