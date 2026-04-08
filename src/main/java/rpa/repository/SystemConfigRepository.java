package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rpa.entity.SystemConfig;
import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问层
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据分类查询配置
     */
    List<SystemConfig> findByCategory(String category);

    /**
     * 根据键名查询配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据分类和键名查询
     */
    Optional<SystemConfig> findByCategoryAndConfigKey(String category, String configKey);

    /**
     * 检查键名是否存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 删除指定键名的配置
     */
    void deleteByConfigKey(String configKey);
}
