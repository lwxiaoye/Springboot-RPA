package rpa.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限实体类
 * <p>
 * 系统权限实体，对应数据库中的sys_permission表。
 * 定义系统中的菜单、按钮和API权限。
 * </p>
 * <p>
 * 权限类型：
 * <ul>
 *   <li>menu - 菜单权限</li>
 *   <li>button - 按钮权限</li>
 *   <li>api - API权限</li>
 * </ul>
 * </p>
 * <p>
 * 权限结构为树形结构，通过parentId关联父子关系。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_permission")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限名称 */
    @Column(nullable = false)
    private String name;

    /** 权限编码，唯一标识 */
    @Column(unique = true, nullable = false)
    private String code;

    /** 权限类型（menu-菜单，button-按钮，api-API） */
    private String type = "menu";

    /** 权限对应的URL路径 */
    private String url;

    /** HTTP请求方法（GET/POST/PUT/DELETE） */
    private String method;

    /** 图标样式 */
    private String icon;

    /** 排序号 */
    private Integer sort = 0;

    /** 父权限ID，0表示顶级权限 */
    private Long parentId = 0L;

    /** 状态（1-启用，0-禁用） */
    private Integer status = 1;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 子权限列表，不持久化到数据库 */
    @Transient
    private List<Permission> children = new ArrayList<>();

    /** 更新时自动设置更新时间 */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
