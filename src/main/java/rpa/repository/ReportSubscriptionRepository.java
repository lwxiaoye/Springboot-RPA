package rpa.repository;

import rpa.entity.ReportSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 报表订阅数据访问层接口
 * <p>
 * 提供报表订阅配置的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {
    
    /**
     * 根据创建人查询订阅列表
     *
     * @param createUser 创建人ID
     * @return List<ReportSubscription> 订阅列表
     */
    List<ReportSubscription> findByCreateUserOrderByCreateTimeDesc(Long createUser);

    /**
     * 查询所有启用的订阅
     *
     * @return List<ReportSubscription> 启用的订阅列表
     */
    List<ReportSubscription> findByEnabledTrueOrderByCreateTimeDesc();

    /**
     * 根据频率查询订阅
     *
     * @param frequency 发送频率（daily/weekly/monthly）
     * @return List<ReportSubscription> 订阅列表
     */
    List<ReportSubscription> findByFrequencyAndEnabledTrue(String frequency);

    /**
     * 根据报表类型查询订阅
     *
     * @param reportType 报表类型
     * @return List<ReportSubscription> 订阅列表
     */
    List<ReportSubscription> findByReportTypeOrderByCreateTimeDesc(String reportType);
}
