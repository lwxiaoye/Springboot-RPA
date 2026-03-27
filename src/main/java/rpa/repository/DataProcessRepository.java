package rpa.repository;

import rpa.entity.DataProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 数据加工配置数据访问层接口
 * <p>
 * 提供数据加工配置实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface DataProcessRepository extends JpaRepository<DataProcess, Long> {
    
    /**
     * 根据状态查询加工配置列表
     *
     * @param status 状态（0-禁用，1-启用，2-运行中）
     * @return List<DataProcess> 加工配置列表
     */
    List<DataProcess> findByStatus(Integer status);
}
