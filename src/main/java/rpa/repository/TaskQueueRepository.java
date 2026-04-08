package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.TaskQueue;
import java.util.List;
import java.util.Optional;

/**
 * 任务队列数据访问层接口
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface TaskQueueRepository extends JpaRepository<TaskQueue, Long> {

    Optional<TaskQueue> findByCode(String code);

    List<TaskQueue> findByStatus(String status);

    List<TaskQueue> findByEnabled(Boolean enabled);

    List<TaskQueue> findByStatusAndEnabled(String status, Boolean enabled);

    @Query("SELECT q FROM TaskQueue q WHERE q.status = 'active' AND q.enabled = true")
    List<TaskQueue> findActiveQueues();

    @Modifying
    @Query("UPDATE TaskQueue q SET q.currentPendingCount = q.currentPendingCount + 1 WHERE q.id = :id")
    void incrementPendingCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TaskQueue q SET q.currentPendingCount = q.currentPendingCount - 1 WHERE q.id = :id AND q.currentPendingCount > 0")
    void decrementPendingCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TaskQueue q SET q.currentRunningCount = q.currentRunningCount + 1 WHERE q.id = :id")
    void incrementRunningCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TaskQueue q SET q.currentRunningCount = q.currentRunningCount - 1 WHERE q.id = :id AND q.currentRunningCount > 0")
    void decrementRunningCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TaskQueue q SET q.completedCount = q.completedCount + 1 WHERE q.id = :id")
    void incrementCompletedCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TaskQueue q SET q.failedCount = q.failedCount + 1 WHERE q.id = :id")
    void incrementFailedCount(@Param("id") Long id);
}
