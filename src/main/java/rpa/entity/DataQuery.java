package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据查询配置实体类
 * <p>
 * 数据查询任务配置，对应数据库中的data_query表。
 * 定义查询的来源表、条件、列和结果。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "data_query")
public class DataQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 查询任务名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 查询来源表名
     */
    private String sourceTable;

    /**
     * 查询条件（WHERE子句，JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String queryCondition;

    /**
     * 查询列定义（逗号分隔）
     */
    @Column(columnDefinition = "TEXT")
    private String queryColumns;

    /**
     * 查询结果数据（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String resultData;

    /**
     * 结果条数
     */
    private Integer resultCount = 0;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status = 0;

    /**
     * 最后查询时间戳
     */
    private Long lastQueryTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 更新时间
     */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 更新时自动设置更新时间 */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}