package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rpa.entity.AlertRoster;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRosterRepository extends JpaRepository<AlertRoster, Long> {

    Optional<AlertRoster> findByCode(String code);

    List<AlertRoster> findByEnabled(Boolean enabled);

    @Query("SELECT ar FROM AlertRoster ar WHERE ar.enabled = true AND (ar.effectiveFrom IS NULL OR ar.effectiveFrom <= :now) AND (ar.effectiveTo IS NULL OR ar.effectiveTo >= :now)")
    List<AlertRoster> findActiveRosters();

    long countByEnabled(Boolean enabled);
}
