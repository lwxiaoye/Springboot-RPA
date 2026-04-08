package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.RobotHealth;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RobotHealthRepository extends JpaRepository<RobotHealth, Long> {

    Optional<RobotHealth> findTopByRobotIdOrderByCheckTimeDesc(Long robotId);

    List<RobotHealth> findByRobotIdOrderByCheckTimeDesc(Long robotId);

    List<RobotHealth> findByHealthStatus(String healthStatus);

    @Query("SELECT rh FROM RobotHealth rh WHERE rh.robotId = :robotId AND rh.checkTime BETWEEN :startTime AND :endTime ORDER BY rh.checkTime DESC")
    List<RobotHealth> findByRobotIdAndTimeRange(@Param("robotId") Long robotId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT AVG(rh.cpuUsage) FROM RobotHealth rh WHERE rh.robotId = :robotId AND rh.checkTime >= :since")
    Double avgCpuUsage(@Param("robotId") Long robotId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(rh.memoryUsage) FROM RobotHealth rh WHERE rh.robotId = :robotId AND rh.checkTime >= :since")
    Double avgMemoryUsage(@Param("robotId") Long robotId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(rh.healthScore) FROM RobotHealth rh WHERE rh.robotId = :robotId AND rh.checkTime >= :since")
    Double avgHealthScore(@Param("robotId") Long robotId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(rh) FROM RobotHealth rh WHERE rh.heartbeatTimeout = true AND rh.checkTime >= :since")
    long countHeartbeatTimeout(@Param("since") LocalDateTime since);

    @Query("SELECT rh FROM RobotHealth rh WHERE rh.healthScore < 60 AND rh.checkTime >= :since")
    List<RobotHealth> findCriticalRobots(@Param("since") LocalDateTime since);
}
