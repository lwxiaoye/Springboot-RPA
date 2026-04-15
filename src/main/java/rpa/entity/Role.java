package rpa.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * <p>
 * 系统角色实体，对应数据库中的sys_role表。
 * 用于RBAC（基于角色的访问控制）权限管理。
 * </p>
 * <p>
 * 系统预定义角色：
 * <ul>
 *   <li>ROLE_ADMIN - 系统管理员，拥有所有权限</li>
 *   <li>ROLE_OPERATOR - 运营人员，负责日常运营操作</li>
 *   <li>ROLE_USER - 普通用户，拥有基础功能权限</li>
 * </ul>
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
@Table(name = "sys_role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色名称 */
    @Column(nullable = false)
    private String name;

    /** 角色编码，唯一标识 */
    @Column(unique = true, nullable = false)
    private String code;

    /** 角色描述 */
    private String description;

    /** 状态（1-启用，0-禁用） */
    private Integer status = 1;

    /** 创建时间 */
    private LocalDateTime createTime = LocalDateTime.now();

    /** 更新时间 */
    private LocalDateTime updateTime = LocalDateTime.now();

    /** 角色关联的权限集合，EAGER立即加载 */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "sys_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    /** 更新时自动设置更新时间 */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}