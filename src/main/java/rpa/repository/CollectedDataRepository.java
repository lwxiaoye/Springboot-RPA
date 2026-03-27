package rpa.repository;

import rpa.entity.CollectedData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 采集数据数据访问层接口
 * <p>
 * 提供采集数据实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface CollectedDataRepository extends JpaRepository<CollectedData, Long> {
    
    /**
     * 根据采集任务ID查询采集数据列表
     *
     * @param collectId 采集任务ID
     * @return List<CollectedData> 采集数据列表
     */
    List<CollectedData> findByCollectId(Long collectId);
    
    /**
     * 根据解析状态查询采集数据列表
     *
     * @param parseStatus 解析状态（0-未解析，1-已解析，2-解析失败）
     * @return List<CollectedData> 采集数据列表
     */
    List<CollectedData> findByParseStatus(Integer parseStatus);
    
    /**
     * 根据数据来源类型查询采集数据列表
     *
     * @param dataType 数据来源类型（web/api/dynamic）
     * @return List<CollectedData> 采集数据列表
     */
    List<CollectedData> findByDataType(String dataType);
}
