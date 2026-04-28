package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.SubscriptionApproval;

import java.util.List;
import java.util.Optional;

/**
 * 订阅审批数据访问层
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Repository
public interface SubscriptionApprovalRepository extends JpaRepository<SubscriptionApproval, Long> {

    /**
     * 根据订阅ID查询审批记录
     */
    Optional<SubscriptionApproval> findBySubscriptionId(Long subscriptionId);

    /**
     * 根据申请人ID查询审批列表
     */
    List<SubscriptionApproval> findByApplicantIdOrderByCreateTimeDesc(Long applicantId);

    /**
     * 根据审批人ID查询待审批列表
     */
    List<SubscriptionApproval> findByApproverIdAndStatusOrderByCreateTimeDesc(Long approverId, String status);

    /**
     * 查询待审批列表
     */
    List<SubscriptionApproval> findByStatusOrderByCreateTimeDesc(String status);

    /**
     * 查询所有待审批数量
     */
    long countByStatus(String status);

    /**
     * 查询申请人的待审批数量
     */
    long countByApplicantIdAndStatus(Long applicantId, String status);

    /**
     * 删除订阅的审批记录
     */
    @Modifying
    @Query("DELETE FROM SubscriptionApproval a WHERE a.subscriptionId = :subscriptionId")
    void deleteBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
