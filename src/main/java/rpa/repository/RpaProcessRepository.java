package rpa.repository;

import rpa.entity.RpaProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * RPA流程数据访问层接口
 * <p>
 * 提供RPA流程实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface RpaProcessRepository extends JpaRepository<RpaProcess, Long> {
    
    /**
     * 根据创建者查询流程列表
     *
     * @param creatorId 创建者ID
     * @return List<RpaProcess> 流程列表
     */
    List<RpaProcess> findByCreatorId(Long creatorId);
    
    /**
     * 根据状态查询流程列表
     *
     * @param status 状态（draft/active/paused/archived）
     * @return List<RpaProcess> 流程列表
     */
    List<RpaProcess> findByStatus(String status);
}
