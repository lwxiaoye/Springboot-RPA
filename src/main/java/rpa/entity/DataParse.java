package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据解析配置实体类
 * <p>
 * 数据解析任务配置，对应数据库中的data_parse表。
 * 定义数据解析的规则、类型和输出格式。
 * </p>
 * <p>
 * 解析规则支持：
 * <ul>
 *   <li>regex:xxx - 正则表达式提取</li>
 *   <li>trim - 去除首尾空格</li>
 *   <li>upper/lower - 大小写转换</li>
 *   <li>replace:old,new - 字符串替换</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "data_parse")
public class DataParse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 解析任务名称 */
    @Column(nullable = false)
    private String name;

    /** 关联的采集任务ID */
    private String collectId;

    /** 解析类型（json/xml/html/regex） */
    private String parseType;

    /** 解析规则定义（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String parseRules;

    /** 输出格式（json/csv/xml） */
    private String outputFormat;

    /** 状态（0-禁用，1-启用） */
    private Integer status = 0;

    /** 成功解析条数 */
    private Integer successCount = 0;
    
    /** 失败条数 */
    private Integer failCount = 0;

    /** 最后解析时间戳 */
    private Long lastParseTime;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 更新时自动设置更新时间 */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}