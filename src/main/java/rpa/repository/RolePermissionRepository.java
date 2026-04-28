package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.RolePermission;

import java.util.List;

/**
 * 角色-权限关联数据访问层
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * 根据角色ID查询所有权限关联
     */
    List<RolePermission> findByRoleId(Long roleId);

    /**
     * 根据权限ID查询所有角色关联
     */
    List<RolePermission> findByPermissionId(Long permissionId);

    /**
     * 检查是否存在关联
     */
    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    /**
     * 删除指定角色的所有权限关联
     */
    void deleteByRoleId(Long roleId);

    /**
     * 删除指定权限的所有角色关联
     */
    void deleteByPermissionId(Long permissionId);
}
