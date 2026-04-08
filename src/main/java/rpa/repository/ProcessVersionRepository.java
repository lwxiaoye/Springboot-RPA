package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.ProcessVersion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessVersionRepository extends JpaRepository<ProcessVersion, Long> {

    List<ProcessVersion> findByProcessIdOrderByCreateTimeDesc(Long processId);

    Optional<ProcessVersion> findByProcessIdAndVersion(Long processId, String version);

    Optional<ProcessVersion> findTopByProcessIdOrderByVersionDesc(Long processId);

    List<ProcessVersion> findByStatus(String status);

    List<ProcessVersion> findByGrayRobotIdsContaining(Long robotId);

    Page<ProcessVersion> findByProcessId(Long processId, Pageable pageable);

    @Query("SELECT pv FROM ProcessVersion pv WHERE pv.processId = :processId AND pv.status = 'published' ORDER BY pv.publishedAt DESC")
    List<ProcessVersion> findPublishedVersions(@Param("processId") Long processId);

    @Query("SELECT pv FROM ProcessVersion pv WHERE pv.status = 'published' AND pv.publishedAt BETWEEN :startTime AND :endTime")
    List<ProcessVersion> findPublishedBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
