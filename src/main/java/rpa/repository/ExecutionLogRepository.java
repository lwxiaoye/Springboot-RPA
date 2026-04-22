package rpa.repository;

import rpa.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * 执行日志数据访问层接口
 * <p>
 * 提供执行日志实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    
    /**
     * 根据任务ID查询执行日志列表
     *
     * @param taskId 任务ID
     * @return List<ExecutionLog> 日志列表
     */
    List<ExecutionLog> findByTaskId(Long taskId);
    
    /**
     * 根据流程ID查询执行日志列表
     *
     * @param processId 流程ID
     * @return List<ExecutionLog> 日志列表
     */
    List<ExecutionLog> findByProcessId(Long processId);
    
    /**
     * 根据机器人ID查询执行日志列表
     *
     * @param robotId 机器人ID
     * @return List<ExecutionLog> 日志列表
     */
    List<ExecutionLog> findByRobotId(Long robotId);
    
    /**
     * 查询所有执行日志（按创建时间倒序）
     *
     * @return List<ExecutionLog> 日志列表
     */
    @Query("SELECT e FROM ExecutionLog e ORDER BY e.createTime DESC")
    List<ExecutionLog> findAllOrderByCreateTimeDesc();
}
