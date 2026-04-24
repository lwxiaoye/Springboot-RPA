package rpa.repository;

import rpa.entity.CustomReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 自定义报表数据访问层接口
 * <p>
 * 提供自定义报表配置的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface CustomReportRepository extends JpaRepository<CustomReport, Long> {
    
    /**
     * 根据创建人查询报表列表
     *
     * @param createUser 创建人ID
     * @return List<CustomReport> 报表列表
     */
    List<CustomReport> findByCreateUserOrderByCreateTimeDesc(Long createUser);

    /**
     * 根据类型查询报表列表
     *
     * @param type 报表类型
     * @return List<CustomReport> 报表列表
     */
    List<CustomReport> findByTypeOrderByCreateTimeDesc(String type);

    /**
     * 查询所有启用的报表
     *
     * @return List<CustomReport> 启用的报表列表
     */
    List<CustomReport> findByEnabledTrueOrderByCreateTimeDesc();

    /**
     * 根据名称模糊查询
     *
     * @param name 报表名称（支持模糊匹配）
     * @return List<CustomReport> 报表列表
     */
    List<CustomReport> findByNameContainingIgnoreCaseOrderByCreateTimeDesc(String name);
}
