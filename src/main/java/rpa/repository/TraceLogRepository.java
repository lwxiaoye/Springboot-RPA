package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.TraceLog;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TraceLogRepository extends JpaRepository<TraceLog, Long> {

    List<TraceLog> findByTraceId(String traceId);

    List<TraceLog> findByOperationName(String operationName);

    List<TraceLog> findByStatus(String status);

    @Query("SELECT tl FROM TraceLog tl WHERE tl.startTime BETWEEN :start AND :end ORDER BY tl.startTime DESC")
    List<TraceLog> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT tl.traceId) FROM TraceLog tl WHERE tl.startTime >= :since")
    long countActiveTraces(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(tl.durationMs) FROM TraceLog tl WHERE tl.startTime >= :since")
    Double avgDuration(@Param("since") LocalDateTime since);
}
