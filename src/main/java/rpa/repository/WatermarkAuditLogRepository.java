package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.WatermarkAuditLog;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 水印审计日志Repository
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Repository
public interface WatermarkAuditLogRepository extends JpaRepository<WatermarkAuditLog, Long> {

    /**
     * 根据用户ID查询审计日志
     */
    Page<WatermarkAuditLog> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据事件类型查询审计日志
     */
    Page<WatermarkAuditLog> findByEventTypeOrderByCreateTimeDesc(String eventType, Pageable pageable);

    /**
     * 根据用户ID和事件类型查询
     */
    Page<WatermarkAuditLog> findByUserIdAndEventTypeOrderByCreateTimeDesc(
        Long userId, String eventType, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    Page<WatermarkAuditLog> findByCreateTimeBetweenOrderByCreateTimeDesc(
        LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据风险级别查询
     */
    Page<WatermarkAuditLog> findByRiskLevelOrderByCreateTimeDesc(String riskLevel, Pageable pageable);

    /**
     * 查询指定时间范围内的所有日志
     */
    @Query("SELECT w FROM WatermarkAuditLog w WHERE w.createTime >= :startTime AND w.createTime <= :endTime ORDER BY w.createTime DESC")
    List<WatermarkAuditLog> findAllByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定事件类型的数量
     */
    long countByEventType(String eventType);

    /**
     * 统计指定时间范围内的事件数量
     */
    long countByEventTypeAndCreateTimeAfter(String eventType, LocalDateTime afterTime);

    /**
     * 统计指定用户的事件数量
     */
    long countByUserId(Long userId);

    /**
     * 统计高风险事件数量
     */
    long countByRiskLevelAndCreateTimeAfter(String riskLevel, LocalDateTime afterTime);

    /**
     * 查询未处理的篡改事件
     */
    @Query("SELECT w FROM WatermarkAuditLog w WHERE w.eventType = 'WATERMARK_TAMPERING' AND w.status = 'DETECTED' ORDER BY w.createTime DESC")
    List<WatermarkAuditLog> findUnhandledTamperingEvents();

    /**
     * 查询最近的截图事件
     */
    @Query("SELECT w FROM WatermarkAuditLog w WHERE w.eventType = 'SCREENSHOT_CAPTURED' AND w.userId = :userId ORDER BY w.createTime DESC")
    List<WatermarkAuditLog> findRecentScreenshotsByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 复杂条件查询
     */
    @Query("SELECT w FROM WatermarkAuditLog w WHERE " +
           "(:userId IS NULL OR w.userId = :userId) AND " +
           "(:eventType IS NULL OR w.eventType = :eventType) AND " +
           "(:riskLevel IS NULL OR w.riskLevel = :riskLevel) AND " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:startTime IS NULL OR w.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR w.createTime <= :endTime) " +
           "ORDER BY w.createTime DESC")
    Page<WatermarkAuditLog> findByConditions(
        @Param("userId") Long userId,
        @Param("eventType") String eventType,
        @Param("riskLevel") String riskLevel,
        @Param("status") String status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable);

    /**
     * 按用户和时间范围统计各类事件
     */
    @Query("SELECT w.eventType, COUNT(w) FROM WatermarkAuditLog w WHERE w.userId = :userId AND w.createTime >= :startTime GROUP BY w.eventType")
    List<Object[]> countEventsByTypeByUser(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 查询需要上报监管的事件
     */
    List<WatermarkAuditLog> findByReportedToRegulatorFalseAndRiskLevelIn(List<String> riskLevels);
}
