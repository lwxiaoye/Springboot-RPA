package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 水印配置实体类
 * <p>
 * 存储水印系统的配置信息，包括：
 * - 水印启用/禁用状态
 * - 各敏感级别的默认配置
 * - 临时关闭记录
 * - 配置修改历史
 * </p>
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Data
@Entity
@Table(name = "watermark_config")
public class WatermarkConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置类型：GLOBAL-全局配置、LEVEL_EXTREME-极高敏感、LEVEL_HIGH-高敏感、
     * LEVEL_MEDIUM-中敏感、LEVEL_LOW-低敏感
     */
    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    /**
     * 敏感级别：extreme、high、medium、low
     */
    @Column(name = "sensitivity_level", length = 20)
    private String sensitivityLevel;

    /**
     * 敏感级别名称
     */
    @Column(name = "sensitivity_name", length = 50)
    private String sensitivityName;

    /**
     * 水印透明度（0.0 - 1.0）
     */
    @Column(name = "opacity", nullable = false)
    private Double opacity = 0.10;

    /**
     * 字体大小（像素）
     */
    @Column(name = "font_size", nullable = false)
    private Integer fontSize = 14;

    /**
     * 水印块宽度（像素）
     */
    @Column(name = "tile_width", nullable = false)
    private Integer tileWidth = 200;

    /**
     * 水印块高度（像素）
     */
    @Column(name = "tile_height", nullable = false)
    private Integer tileHeight = 200;

    /**
     * 旋转角度（度数）
     */
    @Column(name = "rotation", nullable = false)
    private Integer rotation = -45;

    /**
     * 是否启用随机偏移
     */
    @Column(name = "random_offset", nullable = false)
    private Boolean randomOffset = true;

    /**
     * 是否显示时间戳
     */
    @Column(name = "show_timestamp", nullable = false)
    private Boolean showTimestamp = true;

    /**
     * 时间戳格式
     */
    @Column(name = "timestamp_format", length = 50)
    private String timestampFormat = "YYYY-MM-DD HH:mm";

    /**
     * 水印颜色（十六进制）
     */
    @Column(name = "color", length = 20)
    private String color = "#000000";

    /**
     * 水印字体
     */
    @Column(name = "font_family", length = 100)
    private String fontFamily = "Microsoft YaHei";

    /**
     * 配置是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 描述信息
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 适用页面（逗号分隔）
     */
    @Column(name = "pages", length = 1000)
    private String pages;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @Column(name = "create_by")
    private Long createBy;

    /**
     * 创建人姓名
     */
    @Column(name = "create_by_name", length = 100)
    private String createByName;

    /**
     * 更新人ID
     */
    @Column(name = "update_by")
    private Long updateBy;

    /**
     * 更新人姓名
     */
    @Column(name = "update_by_name", length = 100)
    private String updateByName;

    /**
     * 配置版本号（用于乐观锁）
     */
    @Version
    private Long version;

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
