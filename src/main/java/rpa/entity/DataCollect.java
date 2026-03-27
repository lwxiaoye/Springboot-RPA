package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据采集配置实体类
 * <p>
 * 数据采集任务配置，对应数据库中的data_collect表。
 * 定义采集数据源的信息、选择器规则和采集方式。
 * </p>
 * <p>
 * 数据来源类型：
 * <ul>
 *   <li>web - 静态网页（使用JSoup采集）</li>
 *   <li>dynamic - 动态页面（使用Playwright采集）</li>
 *   <li>api - API接口</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "data_collect")
public class DataCollect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 采集任务名称 */
    @Column(nullable = false)
    private String name;

    /** 数据源URL地址 */
    @Column(nullable = false)
    private String sourceUrl;

    /** 数据来源类型（web-静态页面，dynamic-动态页面，api-API接口） */
    private String sourceType;

    /** CSS选择器规则（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String selectorRules;

    /** 自定义HTTP请求头（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String headers;

    /** 认证Cookies（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String cookies;

    /** 定时任务表达式（CRON格式） */
    private String cronExpression;

    /** 状态（0-禁用，1-启用，2-运行中） */
    private Integer status = 0;

    /** 最后采集时间戳 */
    private Long lastCollectTime;

    /** 已采集数据条数 */
    private Integer dataCount = 0;

    /** 创建者ID */
    private Long creatorId;
    
    /** 创建者名称 */
    private String creatorName;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();
    
    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();
}
