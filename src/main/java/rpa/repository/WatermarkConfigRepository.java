package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.WatermarkConfig;
import java.util.List;
import java.util.Optional;

/**
 * 水印配置Repository
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Repository
public interface WatermarkConfigRepository extends JpaRepository<WatermarkConfig, Long> {

    /**
     * 根据配置类型查询配置
     */
    Optional<WatermarkConfig> findByConfigType(String configType);

    /**
     * 根据敏感级别查询配置
     */
    Optional<WatermarkConfig> findBySensitivityLevel(String sensitivityLevel);

    /**
     * 查询所有启用的配置
     */
    List<WatermarkConfig> findByEnabledTrue();

    /**
     * 查询所有配置，按类型排序
     */
    List<WatermarkConfig> findAllByOrderByConfigTypeAsc();

    /**
     * 检查配置类型是否存在
     */
    boolean existsByConfigType(String configType);

    /**
     * 根据配置类型删除
     */
    void deleteByConfigType(String configType);
}
