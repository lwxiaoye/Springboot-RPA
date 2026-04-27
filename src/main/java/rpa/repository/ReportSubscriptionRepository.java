package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.ReportSubscription;

import java.util.List;

/**
 * 报表订阅数据访问层接口
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {

    /**
     * 根据创建人查询订阅列表
     */
    List<ReportSubscription> findByCreateUserOrderByCreateTimeDesc(Long createUser);

    /**
     * 查询所有启用的订阅 (enabled = 1)
     */
    List<ReportSubscription> findByEnabledOrderByCreateTimeDesc(Integer enabled);

    /**
     * 根据频率查询订阅
     */
    List<ReportSubscription> findByFrequencyAndEnabled(String frequency, Integer enabled);

    /**
     * 根据报表类型查询订阅
     */
    List<ReportSubscription> findByReportTypeOrderByCreateTimeDesc(String reportType);
}
