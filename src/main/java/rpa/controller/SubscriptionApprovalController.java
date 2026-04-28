package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.entity.ReportSubscription;
import rpa.entity.SubscriptionApproval;
import rpa.service.SubscriptionApprovalService;
import rpa.service.SubscriptionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订阅审批控制器
 * <p>
 * 提供报表订阅的审批流程API。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/subscription-approval")
@RequiredArgsConstructor
@CrossOrigin
public class SubscriptionApprovalController {

    private final SubscriptionApprovalService approvalService;
    private final SubscriptionService subscriptionService;

    /**
     * 获取所有待审批列表
     */
    @GetMapping("/pending")
    public Map<String, Object> getPendingList() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SubscriptionApproval> approvals = approvalService.getAllPendingApprovals();
            response.put("code", 0);
            response.put("data", approvals);
            response.put("total", approvals.size());
        } catch (Exception e) {
            log.error("获取待审批列表失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取我的待审批列表（审批人视角）
     */
    @GetMapping("/my-pending")
    public Map<String, Object> getMyPendingList(@RequestParam Long approverId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SubscriptionApproval> approvals = approvalService.getPendingApprovals(approverId);
            response.put("code", 0);
            response.put("data", approvals);
            response.put("total", approvals.size());
        } catch (Exception e) {
            log.error("获取待审批列表失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取我的申请历史
     */
    @GetMapping("/my-history")
    public Map<String, Object> getMyHistory(@RequestParam Long applicantId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<SubscriptionApproval> approvals = approvalService.getApplicantHistory(applicantId);
            response.put("code", 0);
            response.put("data", approvals);
            response.put("total", approvals.size());
        } catch (Exception e) {
            log.error("获取申请历史失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 提交订阅审批申请
     */
    @PostMapping("/submit")
    public Map<String, Object> submitApproval(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long subscriptionId = Long.valueOf(request.get("subscriptionId").toString());
            Long applicantId = Long.valueOf(request.get("applicantId").toString());
            String reason = (String) request.getOrDefault("reason", "");

            ReportSubscription subscription = subscriptionService.findById(subscriptionId)
                    .orElseThrow(() -> new RuntimeException("订阅不存在"));

            SubscriptionApproval approval = approvalService.submitApproval(subscription, applicantId, reason);

            response.put("code", 0);
            response.put("data", approval);
            response.put("message", "审批申请已提交");
        } catch (Exception e) {
            log.error("提交审批申请失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    public Map<String, Object> approve(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long approvalId = Long.valueOf(request.get("approvalId").toString());
            Long approverId = Long.valueOf(request.get("approverId").toString());
            String remark = (String) request.getOrDefault("remark", "");

            SubscriptionApproval approval = approvalService.approve(approvalId, approverId, remark);

            response.put("code", 0);
            response.put("data", approval);
            response.put("message", "审批已通过");
        } catch (Exception e) {
            log.error("审批失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/reject")
    public Map<String, Object> reject(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long approvalId = Long.valueOf(request.get("approvalId").toString());
            Long approverId = Long.valueOf(request.get("approverId").toString());
            String remark = (String) request.getOrDefault("remark", "");

            SubscriptionApproval approval = approvalService.reject(approvalId, approverId, remark);

            response.put("code", 0);
            response.put("data", approval);
            response.put("message", "审批已拒绝");
        } catch (Exception e) {
            log.error("审批失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取待审批数量
     */
    @GetMapping("/pending-count")
    public Map<String, Object> getPendingCount() {
        Map<String, Object> response = new HashMap<>();
        try {
            long count = approvalService.getPendingCount();
            response.put("code", 0);
            response.put("data", Map.of("count", count));
        } catch (Exception e) {
            log.error("获取待审批数量失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 查询审批详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            return approvalService.findById(id)
                    .map(approval -> {
                        response.put("code", 0);
                        response.put("data", approval);
                        return response;
                    })
                    .orElseGet(() -> {
                        response.put("code", -1);
                        response.put("message", "审批记录不存在");
                        return response;
                    });
        } catch (Exception e) {
            log.error("查询审批详情失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
