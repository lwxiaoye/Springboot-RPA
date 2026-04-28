package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.NotificationTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板数据访问层
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * 根据模板编码查询
     */
    Optional<NotificationTemplate> findByCode(String code);

    /**
     * 根据模板类型查询
     */
    List<NotificationTemplate> findByTypeOrderByCreateTimeDesc(String type);

    /**
     * 查询启用的模板 (enabled = 1)
     */
    List<NotificationTemplate> findByEnabled(Integer enabled);

    /**
     * 根据类型和渠道查询启用的模板
     */
    Optional<NotificationTemplate> findByTypeAndChannelsContainingAndEnabled(String type, String channel, Integer enabled);

    /**
     * 查询默认模板
     */
    Optional<NotificationTemplate> findByTypeAndIsDefault(String type, Integer isDefault);

    /**
     * 增加使用次数
     */
    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.useCount = t.useCount + 1 WHERE t.id = :id")
    void incrementUseCount(@Param("id") Long id);

    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String code);
}
