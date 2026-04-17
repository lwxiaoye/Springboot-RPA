package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.TriggerRule;
import java.util.List;
import java.util.Optional;

/**
 * 触发器规则数据访问层接口
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface TriggerRuleRepository extends JpaRepository<TriggerRule, Long> {

    Optional<TriggerRule> findByCode(String code);

    List<TriggerRule> findByTriggerType(String triggerType);

    List<TriggerRule> findByStatus(String status);

    List<TriggerRule> findByEnabled(Boolean enabled);

    List<TriggerRule> findByProcessId(Long processId);

    List<TriggerRule> findByQueueId(Long queueId);

    @Query("SELECT t FROM TriggerRule t WHERE t.status = 'active'")
    List<TriggerRule> findActiveTriggers();

    @Query("SELECT t FROM TriggerRule t WHERE t.triggerType = 'schedule' AND t.status = 'active'")
    List<TriggerRule> findActiveScheduleTriggers();

    @Query("SELECT t FROM TriggerRule t WHERE t.triggerType = 'file' AND t.status = 'active'")
    List<TriggerRule> findActiveFileTriggers();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TriggerRule t SET t.totalTriggers = t.totalTriggers + 1, t.lastTriggerTime = CURRENT_TIMESTAMP WHERE t.id = :id")
    void incrementTriggerCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TriggerRule t SET t.successTriggers = t.successTriggers + 1, t.lastSuccessTime = CURRENT_TIMESTAMP WHERE t.id = :id")
    void incrementSuccessCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TriggerRule t SET t.failedTriggers = t.failedTriggers + 1, t.lastFailedTime = CURRENT_TIMESTAMP WHERE t.id = :id")
    void incrementFailedTriggerCount(@Param("id") Long id);
}
