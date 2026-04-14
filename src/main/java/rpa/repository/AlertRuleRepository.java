package rpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.AlertRule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    Optional<AlertRule> findByCode(String code);

    List<AlertRule> findByAlertType(String alertType);

    List<AlertRule> findBySeverity(String severity);

    List<AlertRule> findByEnabled(Boolean enabled);

    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true AND ar.alertType = :alertType")
    List<AlertRule> findEnabledByAlertType(@Param("alertType") String alertType);

    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true ORDER BY ar.severity ASC, ar.checkInterval ASC")
    List<AlertRule> findAllEnabledOrderByPriority();

    long countByEnabled(Boolean enabled);
}
