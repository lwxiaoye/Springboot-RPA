package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ProcessedData;
import java.util.List;

/**
 * 加工数据数据访问层接口
 * <p>
 * 提供加工数据实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
@Repository
public interface ProcessedDataRepository extends JpaRepository<ProcessedData, Long> {
    
    /**
     * 根据加工任务ID查询加工数据列表
     *
     * @param processId 加工任务ID
     * @return List<ProcessedData> 加工数据列表
     */
    List<ProcessedData> findByProcessId(Long processId);
    
    /**
     * 根据采集来源ID查询加工数据列表
     *
     * @param collectId 采集来源ID
     * @return List<ProcessedData> 加工数据列表
     */
    List<ProcessedData> findByCollectId(Long collectId);
    
    /**
     * 根据数据来源类型查询加工数据列表
     *
     * @param sourceType 数据来源类型
     * @return List<ProcessedData> 加工数据列表
     */
    List<ProcessedData> findBySourceType(String sourceType);
    
    /**
     * 根据加工任务ID分页查询加工数据
     *
     * @param processId 加工任务ID
     * @param pageable 分页参数
     * @return Page<ProcessedData> 分页结果
     */
    Page<ProcessedData> findByProcessId(Long processId, Pageable pageable);
    
    /**
     * 查询最新的加工数据（按创建时间倒序）
     *
     * @param processId 加工任务ID
     * @return List<ProcessedData> 加工数据列表
     */
    @Query("SELECT p FROM ProcessedData p WHERE p.processId = :processId ORDER BY p.createTime DESC")
    List<ProcessedData> findLatestByProcessId(@Param("processId") Long processId);
    
    /**
     * 关键字搜索（搜索名称和采集名称）
     *
     * @param keyword 搜索关键字
     * @return List<ProcessedData> 匹配的加工数据列表
     */
    @Query("SELECT p FROM ProcessedData p WHERE p.name LIKE %:keyword% OR p.collectName LIKE %:keyword%")
    List<ProcessedData> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 根据加工任务ID删除数据
     *
     * @param processId 加工任务ID
     */
    void deleteByProcessId(Long processId);
    
    /**
     * 根据采集来源ID删除数据
     *
     * @param collectId 采集来源ID
     */
    void deleteByCollectId(Long collectId);
    
    /**
     * 统计加工任务的数据条数
     *
     * @param processId 加工任务ID
     * @return long 数据条数
     */
    long countByProcessId(Long processId);
    
    /**
     * 统计采集来源的数据条数
     *
     * @param collectId 采集来源ID
     * @return long 数据条数
     */
    long countByCollectId(Long collectId);
}
