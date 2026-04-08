package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.TriggerRule;
import rpa.entity.RpaProcess;
import rpa.repository.TriggerRuleRepository;
import rpa.repository.RpaProcessRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 触发器服务类
 * <p>
 * 提供触发器的CRUD操作和触发逻辑执行。
 * 支持定时触发、文件触发、API触发、Webhook触发。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerRuleService {

    private final TriggerRuleRepository repository;
    private final RpaProcessRepository processRepository;
    private final RpaProcessService rpaProcessService;

    private final Map<Long, Long> lastTriggerTime = new ConcurrentHashMap<>();

    public List<TriggerRule> findAll() {
        return repository.findAll();
    }

    public Optional<TriggerRule> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<TriggerRule> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<TriggerRule> findByTriggerType(String triggerType) {
        return repository.findByTriggerType(triggerType);
    }

    public List<TriggerRule> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    public List<TriggerRule> findActiveTriggers() {
        return repository.findActiveTriggers();
    }

    public List<TriggerRule> findActiveScheduleTriggers() {
        return repository.findActiveScheduleTriggers();
    }

    @Transactional
    public TriggerRule create(String name, String code, String description, String triggerType,
                              Long processId, String processName, String processCode,
                              Long queueId, String queueName,
                              String cron, String scheduleType, String scheduleTime, String scheduleDays,
                              String watchPath, String filePattern, Boolean watchSubdirs,
                              String apiKey, String webhookUrl, String httpMethod,
                              String triggerCondition, String triggerParams,
                              Boolean autoStart, Integer maxConcurrent,
                              String creator, String remark) {
        TriggerRule trigger = new TriggerRule();
        trigger.setName(name);
        trigger.setCode(code);
        trigger.setDescription(description);
        trigger.setTriggerType(triggerType);
        trigger.setProcessId(processId);
        trigger.setProcessName(processName);
        trigger.setProcessCode(processCode);
        trigger.setQueueId(queueId);
        trigger.setQueueName(queueName);
        trigger.setCron(cron);
        trigger.setScheduleType(scheduleType);
        trigger.setScheduleTime(scheduleTime);
        trigger.setScheduleDays(scheduleDays);
        trigger.setWatchPath(watchPath);
        trigger.setFilePattern(filePattern);
        trigger.setWatchSubdirs(watchSubdirs != null ? watchSubdirs : false);
        trigger.setApiKey(apiKey);
        trigger.setWebhookUrl(webhookUrl);
        trigger.setHttpMethod(httpMethod != null ? httpMethod : "POST");
        trigger.setTriggerCondition(triggerCondition);
        trigger.setTriggerParams(triggerParams);
        trigger.setAutoStart(autoStart != null ? autoStart : true);
        trigger.setMaxConcurrent(maxConcurrent != null ? maxConcurrent : 1);
        trigger.setCreator(creator);
        trigger.setRemark(remark);
        trigger.setStatus("active");
        trigger.setEnabled(true);
        trigger.setCreateTime(LocalDateTime.now());
        trigger.setUpdateTime(LocalDateTime.now());
        trigger.setTotalTriggers(0L);
        trigger.setSuccessTriggers(0L);
        trigger.setFailedTriggers(0L);
        return repository.save(trigger);
    }

    @Transactional
    public TriggerRule update(Long id, Map<String, Object> params) {
        return repository.findById(id).map(trigger -> {
            if (params.containsKey("name")) trigger.setName((String) params.get("name"));
            if (params.containsKey("description")) trigger.setDescription((String) params.get("description"));
            if (params.containsKey("status")) trigger.setStatus((String) params.get("status"));
            if (params.containsKey("cron")) trigger.setCron((String) params.get("cron"));
            if (params.containsKey("scheduleType")) trigger.setScheduleType((String) params.get("scheduleType"));
            if (params.containsKey("scheduleTime")) trigger.setScheduleTime((String) params.get("scheduleTime"));
            if (params.containsKey("scheduleDays")) trigger.setScheduleDays((String) params.get("scheduleDays"));
            if (params.containsKey("watchPath")) trigger.setWatchPath((String) params.get("watchPath"));
            if (params.containsKey("filePattern")) trigger.setFilePattern((String) params.get("filePattern"));
            if (params.containsKey("watchSubdirs")) trigger.setWatchSubdirs((Boolean) params.get("watchSubdirs"));
            if (params.containsKey("apiKey")) trigger.setApiKey((String) params.get("apiKey"));
            if (params.containsKey("webhookUrl")) trigger.setWebhookUrl((String) params.get("webhookUrl"));
            if (params.containsKey("httpMethod")) trigger.setHttpMethod((String) params.get("httpMethod"));
            if (params.containsKey("triggerCondition")) trigger.setTriggerCondition((String) params.get("triggerCondition"));
            if (params.containsKey("triggerParams")) trigger.setTriggerParams((String) params.get("triggerParams"));
            if (params.containsKey("autoStart")) trigger.setAutoStart((Boolean) params.get("autoStart"));
            if (params.containsKey("maxConcurrent")) trigger.setMaxConcurrent((Integer) params.get("maxConcurrent"));
            if (params.containsKey("remark")) trigger.setRemark((String) params.get("remark"));
            if (params.containsKey("enabled")) trigger.setEnabled((Boolean) params.get("enabled"));
            trigger.setUpdateTime(LocalDateTime.now());
            return repository.save(trigger);
        }).orElseThrow(() -> new RuntimeException("触发器不存在"));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public TriggerRule updateStatus(Long id, String status) {
        return repository.findById(id).map(trigger -> {
            trigger.setStatus(status);
            trigger.setUpdateTime(LocalDateTime.now());
            return repository.save(trigger);
        }).orElseThrow(() -> new RuntimeException("触发器不存在"));
    }

    @Transactional
    public void incrementTriggerCount(Long id) {
        repository.incrementTriggerCount(id);
    }

    @Transactional
    public void incrementSuccessCount(Long id) {
        repository.incrementSuccessCount(id);
    }

    @Transactional
    public void incrementFailedCount(Long id) {
        repository.incrementFailedTriggerCount(id);
    }

    /**
     * 触发流程执行
     */
    public Map<String, Object> triggerProcess(Long triggerId) {
        TriggerRule trigger = repository.findById(triggerId)
            .orElseThrow(() -> new RuntimeException("触发器不存在"));

        if (!"active".equals(trigger.getStatus()) || !trigger.getEnabled()) {
            throw new RuntimeException("触发器未启用");
        }

        if (trigger.getProcessId() == null) {
            throw new RuntimeException("触发器未关联流程");
        }

        Long currentCount = lastTriggerTime.getOrDefault(triggerId, 0L);
        if (currentCount >= trigger.getMaxConcurrent()) {
            throw new RuntimeException("已达到最大并发数");
        }

        lastTriggerTime.put(triggerId, currentCount + 1);

        try {
            incrementTriggerCount(triggerId);

            RpaProcess process = processRepository.findById(trigger.getProcessId())
                .orElseThrow(() -> new RuntimeException("关联的流程不存在"));

            Map<String, Object> result = new ConcurrentHashMap<>();

            if (Boolean.TRUE.equals(trigger.getAutoStart())) {
                result = rpaProcessService.execute(trigger.getProcessId());
                incrementSuccessCount(triggerId);
            } else {
                result.put("processId", trigger.getProcessId());
                result.put("processName", trigger.getProcessName());
                result.put("message", "流程已就绪，等待手动启动");
            }

            result.put("triggerId", triggerId);
            result.put("triggerName", trigger.getName());
            result.put("triggerType", trigger.getTriggerType());

            return result;

        } catch (Exception e) {
            incrementFailedCount(triggerId);
            throw new RuntimeException("触发执行失败: " + e.getMessage());
        } finally {
            lastTriggerTime.computeIfPresent(triggerId, (k, v) -> Math.max(0, v - 1));
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkScheduleTriggers() {
        List<TriggerRule> triggers = findActiveScheduleTriggers();
        for (TriggerRule trigger : triggers) {
            try {
                if (shouldTrigger(trigger)) {
                    log.info("触发定时任务: {} - {}", trigger.getName(), trigger.getCron());
                    triggerProcess(trigger.getId());
                }
            } catch (Exception e) {
                log.error("定时触发失败: {}", trigger.getName(), e);
            }
        }
    }

    private boolean shouldTrigger(TriggerRule trigger) {
        if (trigger.getCron() == null || trigger.getCron().isEmpty()) {
            return checkSimpleSchedule(trigger);
        }
        return checkCronExpression(trigger.getCron());
    }

    private boolean checkSimpleSchedule(TriggerRule trigger) {
        LocalDateTime now = LocalDateTime.now();
        String scheduleType = trigger.getScheduleType();
        String scheduleTime = trigger.getScheduleTime();

        if (scheduleTime == null) return false;

        String[] parts = scheduleTime.split(":");
        if (parts.length < 2) return false;

        int targetHour = Integer.parseInt(parts[0]);
        int targetMinute = Integer.parseInt(parts[1]);

        if (now.getHour() == targetHour && now.getMinute() == targetMinute && now.getSecond() < 60) {
            return true;
        }
        return false;
    }

    private boolean checkCronExpression(String cron) {
        return false;
    }
}
