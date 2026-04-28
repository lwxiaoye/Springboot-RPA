package rpa.entity;

import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 角色-权限关联实体
 * <p>
 * 角色和权限的多对多关联中间表实体。
 * 对应数据库中的 sys_role_permission 表。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Data
@Entity
@Table(name = "sys_role_permission")
@IdClass(RolePermissionId.class)
public class RolePermission {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Id
    @Column(name = "permission_id")
    private Long permissionId;
}

/**
 * 角色-权限关联复合主键
 */
class RolePermissionId implements Serializable {
    private Long roleId;
    private Long permissionId;

    public RolePermissionId() {}

    public RolePermissionId(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermissionId that = (RolePermissionId) o;
        return roleId.equals(that.roleId) && permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return 31 * roleId.hashCode() + permissionId.hashCode();
    }
}
