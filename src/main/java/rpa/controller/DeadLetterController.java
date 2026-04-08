package rpa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rpa.entity.DeadLetterQueue;
import rpa.service.IntelligentSchedulerService;
import rpa.repository.DeadLetterQueueRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/dead-letter-queue")
public class DeadLetterController {

    @Autowired
    private DeadLetterQueueRepository deadLetterQueueRepository;

    @Autowired
    private IntelligentSchedulerService schedulerService;

    @GetMapping
    public Map<String, Object> getDeadLetters(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<DeadLetterQueue> deadLetters;

        if (status != null && !status.isEmpty()) {
            deadLetters = deadLetterQueueRepository.findByStatus(status);
        } else {
            deadLetters = deadLetterQueueRepository.findAll();
        }

        // 关键词过滤
        if (keyword != null && !keyword.isEmpty()) {
            String k = keyword.toLowerCase();
            deadLetters = deadLetters.stream()
                .filter(d -> (d.getTaskName() != null && d.getTaskName().toLowerCase().contains(k)) ||
                            (d.getProcessName() != null && d.getProcessName().toLowerCase().contains(k)) ||
                            (d.getErrorMessage() != null && d.getErrorMessage().toLowerCase().contains(k)))
                .collect(Collectors.toList());
        }

        return success(deadLetters);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getDeadLetter(@PathVariable Long id) {
        return deadLetterQueueRepository.findById(id)
            .map(this::success)
            .orElse(error("死信记录不存在"));
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        List<DeadLetterQueue> all = deadLetterQueueRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());
        stats.put("pending", all.stream().filter(d -> "pending".equals(d.getStatus())).count());
        stats.put("analysing", all.stream().filter(d -> "analysing".equals(d.getStatus())).count());
        stats.put("resolved", all.stream().filter(d -> "resolved".equals(d.getStatus())).count());
        stats.put("manually_closed", all.stream().filter(d -> "manually_closed".equals(d.getStatus())).count());
        return success(stats);
    }

    @PostMapping("/{id}/resolve")
    public Map<String, Object> resolveDeadLetter(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        try {
            String resolutionType = params.get("resolutionType");
            String comment = params.getOrDefault("comment", "");

            boolean result = schedulerService.processDeadLetter(id, resolutionType, comment);

            if (result) {
                return success("死信处理成功");
            } else {
                return error("处理失败");
            }
        } catch (Exception e) {
            log.error("处理死信失败", e);
            return error("处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/retry")
    public Map<String, Object> retryDeadLetter(@PathVariable Long id) {
        try {
            boolean result = schedulerService.processDeadLetter(id, "retry", "手动重试");
            if (result) {
                return success("任务已重新提交到队列");
            } else {
                return error("处理失败");
            }
        } catch (Exception e) {
            log.error("重试失败", e);
            return error("重试失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/skip")
    public Map<String, Object> skipDeadLetter(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        try {
            String comment = params.getOrDefault("comment", "");
            boolean result = schedulerService.processDeadLetter(id, "skip", comment);
            if (result) {
                return success("任务已跳过");
            } else {
                return error("处理失败");
            }
        } catch (Exception e) {
            log.error("跳过失败", e);
            return error("跳过失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        try {
            DeadLetterQueue dlq = deadLetterQueueRepository.findById(id).orElse(null);
            if (dlq == null) {
                return error("死信记录不存在");
            }

            String status = params.get("status");
            dlq.setStatus(status);

            if ("resolved".equals(status)) {
                dlq.setResolvedAt(LocalDateTime.now());
                dlq.setResolvedBy(params.get("resolvedBy"));
                dlq.setResolutionType(params.get("resolutionType"));
                dlq.setResolutionComment(params.get("resolutionComment"));
            }

            deadLetterQueueRepository.save(dlq);
            return success(dlq);
        } catch (Exception e) {
            log.error("更新状态失败", e);
            return error("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/queue/{queueId}")
    public Map<String, Object> getByQueue(@PathVariable Long queueId) {
        List<DeadLetterQueue> deadLetters = deadLetterQueueRepository.findByOriginalQueueId(queueId);
        return success(deadLetters);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }
}
