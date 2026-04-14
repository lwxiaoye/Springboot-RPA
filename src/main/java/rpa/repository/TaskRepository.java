package rpa.repository;

import rpa.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 任务数据访问层接口
 * <p>
 * 提供任务实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * 根据任务接收人查询任务列表
     *
     * @param assigneeId 任务接收人ID
     * @return List<Task> 任务列表
     */
    List<Task> findByAssigneeId(Long assigneeId);
    
    /**
     * 根据机器人查询任务列表
     *
     * @param robotId 机器人ID
     * @return List<Task> 任务列表
     */
    List<Task> findByRobotId(Long robotId);
    
    /**
     * 根据状态查询任务列表
     *
     * @param status 状态（pending/assigned/running/completed/failed/cancelled）
     * @return List<Task> 任务列表
     */
    List<Task> findByStatus(String status);

    /**
     * 根据机器人和状态查询任务列表
     */
    List<Task> findByRobotIdAndStatus(Long robotId, String status);
    
    /**
     * 根据分类查询任务列表
     *
     * @param category 任务分类
     * @return List<Task> 任务列表
     */
    List<Task> findByCategory(String category);
}
