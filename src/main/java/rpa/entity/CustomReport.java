package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 自定义报表配置实体类
 * <p>
 * 存储用户创建的自定义报表配置，对应数据库中的custom_report表。
 * 支持多种报表类型、统计维度和图表展示方式。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "custom_report")
public class CustomReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 报表名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 报表类型（任务执行/数据采集/机器人效能/流程效率/成本效益/自定义） */
    @Column(nullable = false, length = 50)
    private String type;

    /** 统计维度（JSON数组格式，如["execCount","successRate","dataCount"]） */
    @Column(columnDefinition = "TEXT")
    private String dimensions;

    /** 时间范围（today/week/month/quarter/year/custom） */
    @Column(length = 20)
    private String dateRange;

    /** 图表类型（line/bar/pie/table） */
    @Column(length = 20)
    private String chartType;

    /** 报表描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 创建人ID */
    @Column(nullable = false)
    private Long createUser;

    /** 创建人姓名 */
    @Column(length = 100)
    private String createUserName;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 最后运行时间 */
    private LocalDateTime lastRunTime;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 更新人ID */
    private Long updateUser;

    /** 更新时间 */
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
