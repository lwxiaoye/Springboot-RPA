package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rpa.entity.Resource;
import java.util.List;

/**
 * 资源数据访问层接口
 * <p>
 * 提供资源实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    
    /**
     * 根据状态查询资源列表
     *
     * @param status 状态（1-启用，0-禁用）
     * @return List<Resource> 资源列表
     */
    List<Resource> findByStatus(Integer status);
    
    /**
     * 根据类型查询资源列表
     *
     * @param type 资源类型（menu/button/api）
     * @return List<Resource> 资源列表
     */
    List<Resource> findByType(String type);
}
