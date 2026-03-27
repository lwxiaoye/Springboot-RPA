package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知数据访问层接口
 * <p>
 * 提供通知实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface NotificationRepository extends JpaRepository<rpa.entity.Notification, Long> {
    
    /**
     * 根据类型查询通知列表（按创建时间倒序）
     *
     * @param type 通知类型（collect/temp/user）
     * @return List<Notification> 通知列表
     */
    List<rpa.entity.Notification> findByTypeOrderByCreateTimeDesc(String type);
    
    /**
     * 根据类型和状态查询通知列表（按创建时间倒序）
     *
     * @param type 通知类型
     * @param status 状态（unread/read）
     * @return List<Notification> 通知列表
     */
    List<rpa.entity.Notification> findByTypeAndStatusOrderByCreateTimeDesc(String type, String status);
    
    /**
     * 根据状态查询通知列表（按创建时间倒序）
     *
     * @param status 状态（unread/read）
     * @return List<Notification> 通知列表
     */
    List<rpa.entity.Notification> findByStatusOrderByCreateTimeDesc(String status);
    
    /**
     * 根据接收者查询通知列表（按创建时间倒序）
     *
     * @param receiverId 接收者ID
     * @return List<Notification> 通知列表
     */
    List<rpa.entity.Notification> findByReceiverIdOrderByCreateTimeDesc(Long receiverId);
    
    /**
     * 查询指定接收者或全员通知（按创建时间倒序）
     *
     * @param receiverId 接收者ID
     * @return List<Notification> 通知列表
     */
    List<rpa.entity.Notification> findByReceiverIdOrReceiverIdIsNullOrderByCreateTimeDesc(Long receiverId);
    
    /**
     * 统计指定时间范围内的指定类型通知数量
     *
     * @param type 通知类型
     * @param start 开始时间
     * @param end 结束时间
     * @return Long 通知数量
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.type = :type AND n.createTime BETWEEN :start AND :end")
    Long countByTypeAndTimeRange(@Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 按日期统计各类型通知数量（用于图表展示）
     *
     * @param start 开始时间
     * @return List<Object[]> 统计数据
     */
    @Query("SELECT FUNCTION('DATE', n.createTime) as dateStr, n.type, COUNT(n) FROM Notification n WHERE n.createTime >= :start GROUP BY FUNCTION('DATE', n.createTime), n.type ORDER BY dateStr")
    List<Object[]> countByDateAndType(@Param("start") LocalDateTime start);
}
