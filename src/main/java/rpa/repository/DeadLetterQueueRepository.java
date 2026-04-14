package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.DeadLetterQueue;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, Long> {

    List<DeadLetterQueue> findByStatus(String status);

    List<DeadLetterQueue> findByOriginalQueueId(Long queueId);

    List<DeadLetterQueue> findByTaskId(Long taskId);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.status = 'pending' ORDER BY dlq.createTime ASC")
    List<DeadLetterQueue> findPendingTasks();

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.retryCount < dlq.maxRetry AND dlq.status = 'pending'")
    List<DeadLetterQueue> findRetryableTasks();

    @Query("SELECT COUNT(dlq) FROM DeadLetterQueue dlq WHERE dlq.originalQueueId = :queueId AND dlq.status = 'pending'")
    long countPendingByQueueId(@Param("queueId") Long queueId);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.createTime BETWEEN :startTime AND :endTime")
    List<DeadLetterQueue> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT dlq FROM DeadLetterQueue dlq WHERE dlq.errorCode = :errorCode AND dlq.status = 'pending'")
    List<DeadLetterQueue> findByErrorCode(@Param("errorCode") String errorCode);
}
