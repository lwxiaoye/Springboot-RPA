package rpa.repository;

import rpa.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
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
     * 根据任务名称查询执行日志列表
     *
     * @param taskName 任务名称
     * @return List<ExecutionLog> 日志列表
     */
    List<ExecutionLog> findByTaskName(String taskName);
    
    /**
     * 查询所有执行日志（按创建时间倒序）
     *
     * @return List<ExecutionLog> 日志列表
     */
    @Query("SELECT e FROM ExecutionLog e ORDER BY e.createTime DESC")
    List<ExecutionLog> findAllOrderByCreateTimeDesc();
    
    /**
     * 根据状态统计执行次数
     */
    @Query("SELECT COUNT(e) FROM ExecutionLog e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);
    
    /**
     * 统计指定时间范围内的执行次数
     */
    @Query("SELECT COUNT(e) FROM ExecutionLog e WHERE e.createTime BETWEEN :start AND :end")
    long countByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 统计指定时间范围内指定状态的执行次数
     */
    @Query("SELECT COUNT(e) FROM ExecutionLog e WHERE e.createTime BETWEEN :start AND :end AND e.status = :status")
    long countByTimeRangeAndStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("status") String status);
    
    /**
     * 根据状态分组统计
     */
    @Query("SELECT e.status, COUNT(e) FROM ExecutionLog e GROUP BY e.status")
    List<Object[]> countGroupByStatus();
    
    /**
     * 查询最近N条执行日志
     */
    @Query(value = "SELECT * FROM execution_log ORDER BY create_time DESC LIMIT :limit", nativeQuery = true)
    List<ExecutionLog> findRecentLogs(@Param("limit") int limit);
    
    /**
     * 根据时间范围查询
     */
    @Query("SELECT e FROM ExecutionLog e WHERE e.createTime BETWEEN :start AND :end ORDER BY e.createTime DESC")
    List<ExecutionLog> findByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 根据创建时间范围查询执行日志（用于MonitorController）
     */
    @Query("SELECT e FROM ExecutionLog e WHERE e.createTime >= :startTime AND e.createTime <= :endTime ORDER BY e.createTime DESC")
    List<ExecutionLog> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按小时统计执行次数（用于执行趋势）
     */
    @Query("SELECT HOUR(e.createTime) as hour, COUNT(e) FROM ExecutionLog e WHERE DATE(e.createTime) = DATE(:date) GROUP BY HOUR(e.createTime)")
    List<Object[]> countByHour(@Param("date") LocalDateTime date);
    
    /**
     * 按天统计执行次数（用于执行趋势）
     */
    @Query("SELECT DATE(e.createTime) as day, COUNT(e) FROM ExecutionLog e WHERE e.createTime >= :startDate GROUP BY DATE(e.createTime)")
    List<Object[]> countByDay(@Param("startDate") LocalDateTime startDate);
    
    /**
     * 统计各状态的数量
     */
    @Query("SELECT e.status, COUNT(e) FROM ExecutionLog e WHERE e.createTime >= :startDate GROUP BY e.status")
    List<Object[]> countStatusGroupByStatus(@Param("startDate") LocalDateTime startDate);
    
    /**
     * 根据创建时间范围统计执行日志数量（用于MonitorController）
     */
    @Query("SELECT COUNT(e) FROM ExecutionLog e WHERE e.createTime >= :startTime AND e.createTime <= :endTime")
    long countByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据创建时间范围和状态统计执行日志数量（用于MonitorController）
     */
    @Query("SELECT COUNT(e) FROM ExecutionLog e WHERE e.createTime >= :startTime AND e.createTime <= :endTime AND e.status = :status")
    long countByCreateTimeBetweenAndStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("status") String status);
}
