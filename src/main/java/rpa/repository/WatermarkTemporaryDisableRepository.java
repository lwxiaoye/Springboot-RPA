package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.WatermarkTemporaryDisable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 水印临时关闭记录Repository
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Repository
public interface WatermarkTemporaryDisableRepository extends JpaRepository<WatermarkTemporaryDisable, Long> {

    /**
     * 查询用户当前生效中的关闭记录
     */
    Optional<WatermarkTemporaryDisable> findByUserIdAndStatus(Long userId, String status);

    /**
     * 查询所有生效中的关闭记录
     */
    List<WatermarkTemporaryDisable> findByStatus(String status);

    /**
     * 查询需要自动恢复的记录（已过期但状态仍为ACTIVE）
     */
    @Query("SELECT w FROM WatermarkTemporaryDisable w WHERE w.status = 'ACTIVE' AND w.scheduledRestoreTime <= :now")
    List<WatermarkTemporaryDisable> findExpiredRecords(@Param("now") LocalDateTime now);

    /**
     * 查询用户的所有关闭记录
     */
    List<WatermarkTemporaryDisable> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 检查用户是否有生效中的关闭记录
     */
    boolean existsByUserIdAndStatus(Long userId, String status);

    /**
     * 查询指定时间范围内的关闭记录
     */
    List<WatermarkTemporaryDisable> findByCreateTimeBetweenOrderByCreateTimeDesc(
        LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户在指定时间范围内的关闭次数
     */
    @Query("SELECT COUNT(w) FROM WatermarkTemporaryDisable w WHERE w.userId = :userId AND w.createTime >= :startTime")
    long countByUserIdAndCreateTimeAfter(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
