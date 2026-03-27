package rpa.repository;

import rpa.entity.DataQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 数据查询配置数据访问层接口
 * <p>
 * 提供数据查询配置实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface DataQueryRepository extends JpaRepository<DataQuery, Long> {
    
    /**
     * 根据状态查询查询配置列表
     *
     * @param status 状态（0-禁用，1-启用）
     * @return List<DataQuery> 查询配置列表
     */
    List<DataQuery> findByStatus(Integer status);
    
    /**
     * 根据来源表查询查询配置列表
     *
     * @param sourceTable 来源表名
     * @return List<DataQuery> 查询配置列表
     */
    List<DataQuery> findBySourceTable(String sourceTable);
}
