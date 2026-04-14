package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findTopByOrderByIdDesc();

    List<AuditLog> findByModule(String module);

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByRiskLevel(String riskLevel);

    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:module IS NULL OR al.module = :module) AND " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:riskLevel IS NULL OR al.riskLevel = :riskLevel) AND " +
           "(:startTime IS NULL OR al.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR al.createTime <= :endTime) " +
           "ORDER BY al.createTime DESC")
    Page<AuditLog> findAuditLogs(@Param("module") String module,
                                  @Param("action") String action,
                                  @Param("riskLevel") String riskLevel,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.createTime BETWEEN :startTime AND :endTime ORDER BY al.createTime DESC")
    List<AuditLog> findByUserIdAndCreateTimeBetween(@Param("userId") Long userId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    @Query("SELECT al FROM AuditLog al WHERE al.createTime BETWEEN :startTime AND :endTime ORDER BY al.createTime DESC")
    List<AuditLog> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    @Query("SELECT al FROM AuditLog al WHERE al.targetType = :targetType AND al.targetId = :targetId ORDER BY al.createTime DESC")
    List<AuditLog> findByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Query("SELECT al FROM AuditLog al WHERE al.riskLevel = 'high' AND al.createTime >= :since ORDER BY al.createTime DESC")
    List<AuditLog> findHighRiskSince(@Param("since") LocalDateTime since);

    long countByRiskLevelAndCreateTimeAfter(String riskLevel, LocalDateTime since);

    long countByModuleAndCreateTimeAfter(String module, LocalDateTime since);
}
