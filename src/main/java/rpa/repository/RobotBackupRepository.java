package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.RobotBackup;
import java.util.List;
import java.util.Optional;

@Repository
public interface RobotBackupRepository extends JpaRepository<RobotBackup, Long> {

    List<RobotBackup> findByPrimaryRobotId(Long primaryRobotId);

    List<RobotBackup> findByBackupRobotId(Long backupRobotId);

    List<RobotBackup> findByStatus(String status);

    @Query("SELECT rb FROM RobotBackup rb WHERE rb.primaryRobotId = :robotId AND rb.status = :status")
    List<RobotBackup> findByPrimaryRobotIdAndStatus(@Param("robotId") Long robotId, @Param("status") String status);

    @Query("SELECT rb FROM RobotBackup rb WHERE rb.primaryRobotId = :robotId AND rb.status = 'active'")
    List<RobotBackup> findActiveByPrimaryRobotId(@Param("robotId") Long robotId);

    @Query("SELECT rb FROM RobotBackup rb WHERE rb.backupRobotId = :robotId AND rb.status = 'active'")
    List<RobotBackup> findActiveByBackupRobotId(@Param("robotId") Long robotId);

    Optional<RobotBackup> findByPrimaryRobotIdAndBackupRobotId(Long primaryRobotId, Long backupRobotId);

    @Query("SELECT rb FROM RobotBackup rb WHERE rb.boundProcessId = :processId AND rb.status = 'active'")
    List<RobotBackup> findByBoundProcessId(@Param("processId") Long processId);

    long countByStatus(String status);
}
