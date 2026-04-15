package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 资源实体类
 * <p>
 * 系统资源实体，对应数据库中的resource表。
 * 用于管理系统的菜单、按钮等资源。
 * </p>
 * <p>
 * 资源类型：
 * <ul>
 *   <li>menu - 菜单资源</li>
 *   <li>button - 按钮资源</li>
 *   <li>api - API资源</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "resource")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资源名称 */
    @Column(nullable = false)
    private String name;

    /** 资源编码，唯一标识 */
    @Column(nullable = false, unique = true)
    private String code;

    /** 资源类型（menu-菜单，button-按钮，api-API） */
    @Column(nullable = false)
    private String type = "menu";

    /** 资源对应的URL路径 */
    private String url;

    /** 图标样式 */
    private String icon;

    /** 排序号 */
    private Integer sort = 0;

    /** 状态（1-启用，0-禁用） */
    private Integer status = 1;

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