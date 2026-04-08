package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.AlertRecord;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findByStatus(String status);

    List<AlertRecord> findBySeverity(String severity);

    List<AlertRecord> findByAlertRuleId(Long alertRuleId);

    @Query("SELECT ar FROM AlertRecord ar WHERE ar.status = 'firing' ORDER BY ar.firstFiredAt DESC")
    List<AlertRecord> findFiringAlerts();

    @Query("SELECT ar FROM AlertRecord ar WHERE ar.severity IN :severities AND ar.status = 'firing' ORDER BY ar.firstFiredAt DESC")
    List<AlertRecord> findFiringBySeverities(@Param("severities") List<String> severities);

    Page<AlertRecord> findByStatusOrderByFirstFiredAtDesc(String status, Pageable pageable);

    @Query("SELECT ar FROM AlertRecord ar WHERE ar.targetType = :targetType AND ar.targetId = :targetId ORDER BY ar.firstFiredAt DESC")
    List<AlertRecord> findByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Query("SELECT COUNT(ar) FROM AlertRecord ar WHERE ar.status = 'firing' AND ar.severity = :severity")
    long countByFiringBySeverity(@Param("severity") String severity);

    long countByStatus(String status);

    @Query("SELECT ar FROM AlertRecord ar WHERE ar.firstFiredAt BETWEEN :startTime AND :endTime")
    List<AlertRecord> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT ar.alertType, COUNT(ar) FROM AlertRecord ar WHERE ar.firstFiredAt >= :since GROUP BY ar.alertType")
    List<Object[]> countByAlertTypeSince(@Param("since") LocalDateTime since);
}
