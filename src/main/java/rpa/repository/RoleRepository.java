package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.Role;
import java.util.Optional;

/**
 * 角色数据访问层接口
 * <p>
 * 提供角色实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 根据角色编码查询角色
     *
     * @param code 角色编码
     * @return Optional<Role> 角色信息
     */
    Optional<Role> findByCode(String code);
    
    /**
     * 检查角色编码是否存在
     *
     * @param code 角色编码
     * @return boolean 是否存在
     */
    boolean existsByCode(String code);
    
    /**
     * 检查角色编码是否存在（排除指定ID）
     * 用于更新时验证编码唯一性
     *
     * @param code 角色编码
     * @param id 排除的角色ID
     * @return boolean 是否存在
     */
    boolean existsByCodeAndIdNot(String code, Long id);
}
