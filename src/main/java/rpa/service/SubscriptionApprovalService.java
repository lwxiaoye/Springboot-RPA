package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.ReportSubscription;
import rpa.entity.SubscriptionApproval;
import rpa.entity.User;
import rpa.repository.ReportSubscriptionRepository;
import rpa.repository.SubscriptionApprovalRepository;
import rpa.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订阅审批服务
 * <p>
 * 提供报表订阅的审批流程管理功能。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionApprovalService {

    private final SubscriptionApprovalRepository approvalRepository;
    private final ReportSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /**
     * 提交订阅审批申请
     */
    @Transactional
    public SubscriptionApproval submitApproval(ReportSubscription subscription, Long applicantId, String reason) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + applicantId));

        // 创建审批记录
        SubscriptionApproval approval = new SubscriptionApproval();
        approval.setSubscriptionId(subscription.getId());
        approval.setSubscriptionName(subscription.getName());
        approval.setApplicantId(applicantId);
        approval.setApplicantName(applicant.getRealName() != null ? applicant.getRealName() : applicant.getUsername());
        approval.setStatus("pending");
        approval.setReason(reason);
        approval.setCreateTime(LocalDateTime.now());

        // 生成订阅摘要
        String summary = String.format("%s | %s | %s | 接收人: %s",
                subscription.getName(),
                subscription.getReportType(),
                subscription.getFrequency(),
                subscription.getRecipients());
        approval.setSubscriptionSummary(summary);

        // 保存审批记录
        approval = approvalRepository.save(approval);

        // 更新订阅状态为待审批
        subscription.setApprovalStatus("pending");
        subscription.setRequireApproval(1);
        subscriptionRepository.save(subscription);

        log.info("提交订阅审批申请: subscriptionId={}, applicantId={}", subscription.getId(), applicantId);

        return approval;
    }

    /**
     * 审批订阅
     */
    @Transactional
    public SubscriptionApproval approve(Long approvalId, Long approverId, String remark) {
        SubscriptionApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批记录不存在: " + approvalId));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("审批人不存在: " + approverId));

        approval.setApproverId(approverId);
        approval.setApproverName(approver.getRealName() != null ? approver.getRealName() : approver.getUsername());
        approval.setStatus("approved");
        approval.setRemark(remark);
        approval.setApprovalTime(LocalDateTime.now());

        approval = approvalRepository.save(approval);

        // 更新订阅状态
        subscriptionRepository.findById(approval.getSubscriptionId()).ifPresent(subscription -> {
            subscription.setApprovalStatus("approved");
            subscription.setApproverId(approverId);
            subscription.setApproverName(approver.getRealName() != null ? approver.getRealName() : approver.getUsername());
            subscription.setApprovalTime(LocalDateTime.now());
            subscription.setApprovalRemark(remark);
            subscriptionRepository.save(subscription);
        });

        log.info("审批通过: approvalId={}, approverId={}", approvalId, approverId);

        return approval;
    }

    /**
     * 拒绝订阅
     */
    @Transactional
    public SubscriptionApproval reject(Long approvalId, Long approverId, String remark) {
        SubscriptionApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("审批记录不存在: " + approvalId));

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("审批人不存在: " + approverId));

        approval.setApproverId(approverId);
        approval.setApproverName(approver.getRealName() != null ? approver.getRealName() : approver.getUsername());
        approval.setStatus("rejected");
        approval.setRemark(remark);
        approval.setApprovalTime(LocalDateTime.now());

        approval = approvalRepository.save(approval);

        // 更新订阅状态
        subscriptionRepository.findById(approval.getSubscriptionId()).ifPresent(subscription -> {
            subscription.setApprovalStatus("rejected");
            subscription.setEnabled(0); // 禁用订阅
            subscriptionRepository.save(subscription);
        });

        log.info("审批拒绝: approvalId={}, approverId={}", approvalId, approverId);

        return approval;
    }

    /**
     * 获取待审批列表（审批人视角）
     */
    public List<SubscriptionApproval> getPendingApprovals(Long approverId) {
        return approvalRepository.findByApproverIdAndStatusOrderByCreateTimeDesc(approverId, "pending");
    }

    /**
     * 获取所有待审批列表
     */
    public List<SubscriptionApproval> getAllPendingApprovals() {
        return approvalRepository.findByStatusOrderByCreateTimeDesc("pending");
    }

    /**
     * 获取申请人的审批历史
     */
    public List<SubscriptionApproval> getApplicantHistory(Long applicantId) {
        return approvalRepository.findByApplicantIdOrderByCreateTimeDesc(applicantId);
    }

    /**
     * 查询审批记录
     */
    public Optional<SubscriptionApproval> findById(Long id) {
        return approvalRepository.findById(id);
    }

    /**
     * 查询订阅的审批记录
     */
    public Optional<SubscriptionApproval> findBySubscriptionId(Long subscriptionId) {
        return approvalRepository.findBySubscriptionId(subscriptionId);
    }

    /**
     * 获取待审批数量
     */
    public long getPendingCount() {
        return approvalRepository.countByStatus("pending");
    }

    /**
     * 获取申请人的待审批数量
     */
    public long getApplicantPendingCount(Long applicantId) {
        return approvalRepository.countByApplicantIdAndStatus(applicantId, "pending");
    }

    /**
     * 检查用户是否为审批人
     */
    public boolean isApprover(Long userId) {
        // 管理员（role=1）可以是审批人
        return userRepository.findById(userId)
                .map(user -> user.getRole() != null && user.getRole() == 1)
                .orElse(false);
    }
}
