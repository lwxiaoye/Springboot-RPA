package rpa.repository;

import rpa.entity.DataParse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 数据解析配置数据访问层接口
 * <p>
 * 提供数据解析配置实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface DataParseRepository extends JpaRepository<DataParse, Long> {
    
    /**
     * 根据状态查询解析配置列表
     *
     * @param status 状态（0-禁用，1-启用）
     * @return List<DataParse> 解析配置列表
     */
    List<DataParse> findByStatus(Integer status);
    
    /**
     * 根据采集任务ID查询解析配置列表
     *
     * @param collectId 采集任务ID
     * @return List<DataParse> 解析配置列表
     */
    List<DataParse> findByCollectId(String collectId);
}
