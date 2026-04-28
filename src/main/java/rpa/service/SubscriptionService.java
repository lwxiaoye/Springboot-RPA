package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.ReportSubscription;
import rpa.repository.ReportSubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订阅服务
 * <p>
 * 提供报表订阅的CRUD操作。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final ReportSubscriptionRepository repository;

    /**
     * 查询所有订阅
     */
    public List<ReportSubscription> findAll() {
        return repository.findAll();
    }

    /**
     * 根据ID查询订阅
     */
    public Optional<ReportSubscription> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 查询启用的订阅
     */
    public List<ReportSubscription> findEnabled() {
        return repository.findByEnabledOrderByCreateTimeDesc(1);
    }

    /**
     * 查询指定类型的订阅
     */
    public List<ReportSubscription> findByReportType(String reportType) {
        return repository.findByReportTypeOrderByCreateTimeDesc(reportType);
    }

    /**
     * 查询已批准且启用的订阅
     */
    public List<ReportSubscription> findApprovedAndEnabled() {
        return repository.findAll().stream()
                .filter(sub -> sub.getEnabled() != null && sub.getEnabled() == 1)
                .filter(sub -> "approved".equals(sub.getApprovalStatus()) || sub.getApprovalStatus() == null)
                .toList();
    }

    /**
     * 创建订阅
     */
    @Transactional
    public ReportSubscription create(ReportSubscription subscription) {
        subscription.setCreateTime(LocalDateTime.now());
        subscription.setEnabled(1);
        subscription.setApprovalStatus("approved"); // 默认直接批准
        return repository.save(subscription);
    }

    /**
     * 更新订阅
     */
    @Transactional
    public ReportSubscription update(Long id, ReportSubscription subscription) {
        return repository.findById(id).map(existing -> {
            existing.setName(subscription.getName());
            existing.setReportType(subscription.getReportType());
            existing.setFrequency(subscription.getFrequency());
            existing.setChannel(subscription.getChannel());
            existing.setRecipients(subscription.getRecipients());

            if (subscription.getEnabled() != null) {
                existing.setEnabled(subscription.getEnabled());
            }

            // 推送时间配置
            if (subscription.getScheduleType() != null) {
                existing.setScheduleType(subscription.getScheduleType());
            }
            if (subscription.getFixedTime() != null) {
                existing.setFixedTime(subscription.getFixedTime());
            }
            if (subscription.getCronExpression() != null) {
                existing.setCronExpression(subscription.getCronExpression());
            }
            if (subscription.getWeekdays() != null) {
                existing.setWeekdays(subscription.getWeekdays());
            }
            if (subscription.getMonths() != null) {
                existing.setMonths(subscription.getMonths());
            }
            if (subscription.getMonthDays() != null) {
                existing.setMonthDays(subscription.getMonthDays());
            }

            // 审批配置
            if (subscription.getRequireApproval() != null) {
                existing.setRequireApproval(subscription.getRequireApproval());
            }

            // 通知配置
            if (subscription.getTemplateId() != null) {
                existing.setTemplateId(subscription.getTemplateId());
            }
            if (subscription.getIncludeAttachment() != null) {
                existing.setIncludeAttachment(subscription.getIncludeAttachment());
            }
            if (subscription.getAttachmentType() != null) {
                existing.setAttachmentType(subscription.getAttachmentType());
            }

            existing.setUpdateTime(LocalDateTime.now());
            return repository.save(existing);
        }).orElseThrow(() -> new RuntimeException("订阅不存在: " + id));
    }

    /**
     * 删除订阅
     */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 切换启用状态
     */
    @Transactional
    public ReportSubscription toggleEnabled(Long id) {
        return repository.findById(id).map(subscription -> {
            subscription.setEnabled(subscription.getEnabled() == 1 ? 0 : 1);
            subscription.setUpdateTime(LocalDateTime.now());
            return repository.save(subscription);
        }).orElseThrow(() -> new RuntimeException("订阅不存在: " + id));
    }

    /**
     * 更新发送状态
     */
    @Transactional
    public void updateSendStatus(Long id, boolean success) {
        repository.findById(id).ifPresent(subscription -> {
            subscription.setLastSendTime(LocalDateTime.now());
            subscription.setLastSendStatus(success ? "success" : "failed");
            if (success) {
                subscription.setSuccessCount(subscription.getSuccessCount() + 1);
            } else {
                subscription.setFailedCount(subscription.getFailedCount() + 1);
            }
            repository.save(subscription);
        });
    }
}
