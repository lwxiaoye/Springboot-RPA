package rpa.repository;

import rpa.entity.DataCollect;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 数据采集配置数据访问层接口
 * <p>
 * 提供数据采集配置实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface DataCollectRepository extends JpaRepository<DataCollect, Long> {
    
    /**
     * 根据状态查询采集配置列表
     *
     * @param status 状态（0-禁用，1-启用，2-运行中）
     * @return List<DataCollect> 采集配置列表
     */
    List<DataCollect> findByStatus(Integer status);
    
    /**
     * 根据创建者查询采集配置列表
     *
     * @param creatorId 创建者ID
     * @return List<DataCollect> 采集配置列表
     */
    List<DataCollect> findByCreatorId(Long creatorId);
}
