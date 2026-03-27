package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.Permission;
import java.util.List;

/**
 * 权限数据访问层接口
 * <p>
 * 提供权限实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * 根据权限类型查询权限列表
     *
     * @param type 权限类型（menu/button/api）
     * @return List<Permission> 权限列表
     */
    List<Permission> findByType(String type);
    
    /**
     * 根据状态查询权限列表
     *
     * @param status 状态（1-启用，0-禁用）
     * @return List<Permission> 权限列表
     */
    List<Permission> findByStatus(Integer status);
    
    /**
     * 根据父权限ID查询子权限列表
     *
     * @param parentId 父权限ID
     * @return List<Permission> 子权限列表
     */
    List<Permission> findByParentId(Long parentId);
    
    /**
     * 根据父权限ID查询子权限列表（按排序号排序）
     *
     * @param parentId 父权限ID
     * @return List<Permission> 排序后的子权限列表
     */
    List<Permission> findByParentIdOrderBySort(Long parentId);
}
