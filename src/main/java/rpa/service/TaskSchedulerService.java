package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rpa.distributed.DistributedLockService;
import rpa.entity.Task;
import rpa.entity.Robot;
import rpa.entity.TaskQueue;
import rpa.entity.RpaProcess;
import rpa.repository.TaskRepository;
import rpa.repository.RobotRepository;
import rpa.repository.TaskQueueRepository;
import rpa.repository.RpaProcessRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 任务调度服务类
 * <p>
 * 实现队列与机器人的智能联动调度。
 * 根据机器人的空闲状态、绑定的流程权限、分类标签，
 * 将任务分发到对应可用的机器人上执行。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final TaskQueueRepository queueRepository;
    private final RpaProcessRepository processRepository;
    private final RpaProcessService rpaProcessService;
    private final TaskQueueService queueService;
    private final ExecutionLogService executionLogService;
    private final DistributedLockService lockService;

    @Value("${rpa.node.id:node-default}")
    private String nodeId;

    private final Map<Long, Long> queueTaskCount = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60000)
    public void schedulePendingTasks() {
        List<Task> pendingTasks = taskRepository.findByStatus("pending");
        if (pendingTasks.isEmpty()) {
            return;
        }

        log.info("发现 {} 个待执行任务", pendingTasks.size());

        for (Task task : pendingTasks) {
            try {
                dispatchTask(task);
            } catch (Exception e) {
                log.error("任务调度失败: taskId={}", task.getId(), e);
            }
        }
    }

    /**
     * 执行已分配的任务（集成分布式锁）
     * 集群环境中通过抢锁确保同一任务只有一个节点执行
     */
    @Scheduled(fixedDelay = 30000)  // 改为 fixedDelay 防止任务重叠
    public void executeAssignedTasks() {
        List<Task> assignedTasks = taskRepository.findByStatus("assigned");
        if (assignedTasks.isEmpty()) {
            return;
        }

        log.info("发现 {} 个已分配任务待执行", assignedTasks.size());

        for (Task task : assignedTasks) {
            try {
                executeTaskWithLock(task);  // 使用带锁的版本
            } catch (Exception e) {
                log.error("任务执行失败: taskId={}", task.getId(), e);
                // 执行失败，标记为失败状态
                completeTask(task.getId(), "failed", null, e.getMessage());
            }
        }
    }

    /**
     * 调度任务到合适的机器人
     * 队列管理中的任务无需绑定机器人，直接标记为已分配，等待执行
     */
    public boolean dispatchTask(Task task) {
        if (task.getProcessId() == null && (task.getProcessIds() == null || task.getProcessIds().isEmpty())) {
            log.warn("任务 {} 未关联流程，无法调度，将标记为失败", task.getId());
            // 回滚队列计数器
            rollbackQueueCount(task);
            completeTask(task.getId(), "failed", null, "任务未关联流程，无法调度");
            return false;
        }

        RpaProcess process = null;
        if (task.getProcessId() != null) {
            process = processRepository.findById(task.getProcessId()).orElse(null);
        } else {
            List<RpaProcess> processes = processRepository.findAll();
            if (!processes.isEmpty()) {
                process = processes.get(0);
            }
        }

        if (process == null) {
            log.warn("任务 {} 未找到关联的流程，将标记为失败", task.getId());
            // 回滚队列计数器
            rollbackQueueCount(task);
            completeTask(task.getId(), "failed", null, "未找到关联的流程");
            return false;
        }

        // 队列任务不需要绑定机器人，直接标记为已分配
        task.setStatus("assigned");
        taskRepository.save(task);

        executionLogService.create(
            task.getId(),
            task.getProcessId(),
            task.getRobotId(),
            "任务分配",
            "assigned",
            "任务已分配，准备执行流程: " + process.getName()
        );

        // 更新队列计数：优先使用任务的queueId，其次使用流程的queueId
        Long queueId = task.getQueueId();
        if (queueId == null && task.getProcessId() != null) {
            queueId = processRepository.findById(task.getProcessId())
                .map(p -> p.getQueueId())
                .orElse(null);
        }

        if (queueId != null) {
            queueService.incrementRunningCount(queueId);
            queueService.decrementPendingCount(queueId);
            log.info("任务 {} 已分配，队列 {} pending-1, running+1", task.getName(), queueId);
        }

        log.info("任务 {} 已分配，等待执行流程 {}", task.getName(), process.getName());

        return true;
    }

    /**
     * 回滚队列计数器（当任务调度失败时调用）
     */
    private void rollbackQueueCount(Task task) {
        // 优先使用任务的queueId，其次使用流程的queueId
        Long queueId = task.getQueueId();
        if (queueId == null && task.getProcessId() != null) {
            queueId = processRepository.findById(task.getProcessId())
                .map(p -> p.getQueueId())
                .orElse(null);
        }

        if (queueId != null) {
            queueService.decrementPendingCount(queueId);
            log.info("已回滚队列 {} 的pending计数", queueId);
        }
    }

    /**
     * 带分布式锁的任务执行（核心方法）
     * 集群中多个节点同时调度同一任务时，通过抢锁确保只有一个节点执行
     * 
     * @param task 待执行的任务
     */
    private void executeTaskWithLock(Task task) {
        String lockName = "task_execution_" + task.getId();
        
        // 尝试获取锁（TTL=300秒=5分钟，等待超时=0秒不等待）
        DistributedLockService.LockResult lockResult = lockService.tryLock(
            lockName,
            nodeId,
            300,  // 锁有效期：5分钟（根据任务类型调整）
            0     // 不等待，立即返回
        );
        
        if (!lockResult.isSuccess()) {
            log.info("任务 {} 抢锁失败，可能其他节点正在执行，跳过本次调度", task.getId());
            return;  // 其他节点已获得锁，直接返回
        }
        
        log.info("任务 {} 抢锁成功，开始执行（持有者: {}）", task.getId(), nodeId);
        
        try {
            // 执行任务（带续期机制）
            executeTaskWithRenew(task, lockName, nodeId);
        } finally {
            // 确保释放锁
            boolean released = lockService.releaseLock(lockName, nodeId);
            if (released) {
                log.info("任务 {} 执行完成，锁已释放", task.getId());
            } else {
                log.warn("任务 {} 锁释放失败（可能已过期自动释放）", task.getId());
            }
        }
    }

    /**
     * 执行任务并定期续期（看门狗机制）
     * 防止长时间任务执行时锁过期
     * 
     * @param task 任务
     * @param lockName 锁名称
     * @param holder 持有者
     */
    private void executeTaskWithRenew(Task task, String lockName, String holder) {
        if (task.getProcessId() == null) {
            log.warn("任务 {} 未关联流程，无法执行", task.getId());
            completeTask(task.getId(), "failed", null, "未关联流程");
            return;
        }

        // 更新任务状态为 running
        task.setStatus("running");
        task.setStartTime(java.time.LocalDateTime.now());
        taskRepository.save(task);

        executionLogService.create(
            task.getId(),
            task.getProcessId(),
            task.getRobotId(),
            "开始执行",
            "running",
            "任务开始执行流程（节点: " + nodeId + "）"
        );

        // 启动续期线程（看门狗）
        final Task finalTask = task;
        Thread renewThread = startWatchdog(finalTask.getId(), lockName, holder, 60);  // 每60秒续期一次
        
        try {
            // 调用流程执行服务
            Map<String, Object> result = rpaProcessService.execute(task.getProcessId());

            // 判断执行结果
            Boolean success = (Boolean) result.getOrDefault("success", false);
            String message = (String) result.getOrDefault("message", "");

            if (success) {
                completeTask(task.getId(), "completed", 
                    result.containsKey("data") ? result.get("data").toString() : null, null);
                log.info("任务 {} 执行成功", task.getName());
            } else {
                completeTask(task.getId(), "failed", null, message);
                log.warn("任务 {} 执行失败: {}", task.getName(), message);
            }

        } catch (Exception e) {
            log.error("任务 {} 执行异常", task.getName(), e);
            completeTask(task.getId(), "failed", null, e.getMessage());
        } finally {
            // 停止续期线程
            renewThread.interrupt();
        }
    }

    /**
     * 启动看门狗线程（自动续期）
     * 防止长时间任务执行时锁过期
     * 
     * @param taskId 任务ID（用于线程命名）
     * @param lockName 锁名称
     * @param holder 持有者
     * @param renewIntervalSeconds 续期间隔（秒）
     * @return 续期线程
     */
    private Thread startWatchdog(Long taskId, String lockName, String holder, int renewIntervalSeconds) {
        Thread watchdog = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(renewIntervalSeconds * 1000L);
                    
                    // 续期锁
                    boolean renewed = lockService.renewLock(lockName, holder, 300);
                    if (renewed) {
                        log.debug("任务 {} 锁续期成功: lockName={}", taskId, lockName);
                    } else {
                        log.warn("任务 {} 锁续期失败（锁可能已释放）: lockName={}", taskId, lockName);
                        break;  // 锁已不存在，停止续期
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("任务 {} 看门狗线程被中断，停止续期", taskId);
            } catch (Exception e) {
                log.error("任务 {} 看门狗线程异常", taskId, e);
            }
        }, "lock-watchdog-task-" + taskId);
        
        watchdog.setDaemon(true);  // 守护线程，不影响主程序退出
        watchdog.start();
        return watchdog;
    }

    /**
     * 执行已分配的任务（原始方法，保留备用）
     * 直接执行流程，不需要机器人
     * @deprecated 已被 executeTaskWithLock 替代
     */
    @Deprecated
    private void executeTask(Task task) {
        if (task.getProcessId() == null) {
            log.warn("任务 {} 未关联流程，无法执行", task.getId());
            completeTask(task.getId(), "failed", null, "未关联流程");
            return;
        }

        // 更新任务状态为 running
        task.setStatus("running");
        task.setStartTime(java.time.LocalDateTime.now());
        taskRepository.save(task);

        executionLogService.create(
            task.getId(),
            task.getProcessId(),
            task.getRobotId(),
            "开始执行",
            "running",
            "任务开始执行流程"
        );

        try {
            // 调用流程执行服务
            Map<String, Object> result = rpaProcessService.execute(task.getProcessId());

            // 判断执行结果
            Boolean success = (Boolean) result.getOrDefault("success", false);
            String message = (String) result.getOrDefault("message", "");

            if (success) {
                completeTask(task.getId(), "completed", 
                    result.containsKey("data") ? result.get("data").toString() : null, null);
                log.info("任务 {} 执行成功", task.getName());
            } else {
                completeTask(task.getId(), "failed", null, message);
                log.warn("任务 {} 执行失败: {}", task.getName(), message);
            }

        } catch (Exception e) {
            log.error("任务 {} 执行异常", task.getName(), e);
            completeTask(task.getId(), "failed", null, e.getMessage());
        }
    }

    /**
     * 查找可用的机器人
     */
    private List<Robot> findAvailableRobots(String requiredCategory, Long preferredRobotId) {
        List<Robot> allRobots = robotRepository.findAll();

        List<Robot> available = allRobots.stream()
            .filter(r -> "idle".equals(r.getStatus()))
            .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
            .filter(r -> r.getCurrentTaskCount() < r.getMaxConcurrentTasks())
            .collect(Collectors.toList());

        if (requiredCategory != null && !requiredCategory.isEmpty()) {
            available = available.stream()
                .filter(r -> requiredCategory.equals(r.getRobotCategory()) ||
                            requiredCategory.equals(r.getAllowedCategories()))
                .collect(Collectors.toList());
        }

        if (preferredRobotId != null) {
            available.sort((r1, r2) -> {
                if (r1.getId().equals(preferredRobotId)) return -1;
                if (r2.getId().equals(preferredRobotId)) return 1;
                return 0;
            });
        }

        return available;
    }

    /**
     * 完成任务
     */
    public void completeTask(Long taskId, String status, String resultData, String errorMessage) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            task.setResultData(resultData);
            task.setErrorMessage(errorMessage);
            task.setEndTime(java.time.LocalDateTime.now());
            taskRepository.save(task);

            if (task.getRobotId() != null) {
                robotRepository.findById(task.getRobotId()).ifPresent(robot -> {
                    robot.setCurrentTaskCount(Math.max(0, robot.getCurrentTaskCount() - 1));
                    if ("completed".equals(status) || "success".equals(status)) {
                        robot.setStatus("idle");
                        robot.setSuccessExecutions(robot.getSuccessExecutions() + 1);
                    } else {
                        robot.setStatus("idle");
                        robot.setFailedExecutions(robot.getFailedExecutions() + 1);
                    }
                    robotRepository.save(robot);
                });
            }

            // 更新队列计数：优先使用任务的queueId，其次使用流程的queueId
            Long queueId = task.getQueueId();
            if (queueId == null && task.getProcessId() != null) {
                // 如果任务没有queueId，尝试从流程获取
                queueId = processRepository.findById(task.getProcessId())
                    .map(p -> p.getQueueId())
                    .orElse(null);
            }

            if (queueId != null) {
                queueService.decrementRunningCount(queueId);
                if ("completed".equals(status) || "success".equals(status)) {
                    queueService.incrementCompletedCount(queueId);
                } else {
                    queueService.incrementFailedCount(queueId);
                }
            }

            executionLogService.create(
                taskId,
                task.getProcessId(),
                task.getRobotId(),
                "任务" + ("completed".equals(status) ? "完成" : "失败"),
                status,
                errorMessage != null ? errorMessage : "任务执行" + ("completed".equals(status) ? "成功" : "失败")
            );
        });
    }

    /**
     * 检查队列状态并重新调度
     */
    @Scheduled(fixedRate = 60000)
    public void checkQueueHealth() {
        List<TaskQueue> activeQueues = queueService.findActiveQueues();

        for (TaskQueue queue : activeQueues) {
            if (queue.getCurrentRunningCount() >= queue.getMaxConcurrentTasks()) {
                log.debug("队列 {} 达到最大并发", queue.getName());
                continue;
            }

            Long pendingInQueue = queueTaskCount.getOrDefault(queue.getId(), 0L);
            if (pendingInQueue == 0) {
                continue;
            }

            log.info("队列 {} 还有 {} 个待处理任务", queue.getName(), pendingInQueue);
        }
    }

    public void addTaskToQueue(Long queueId) {
        queueTaskCount.merge(queueId, 1L, Long::sum);
        queueService.incrementPendingCount(queueId);
    }

    public void removeTaskFromQueue(Long queueId) {
        queueTaskCount.computeIfPresent(queueId, (k, v) -> Math.max(0, v - 1));
    }

    public Map<String, Object> getSchedulerStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        long totalRobots = robotRepository.count();
        long idleRobots = robotRepository.findAll().stream()
            .filter(r -> "idle".equals(r.getStatus())).count();
        long busyRobots = robotRepository.findAll().stream()
            .filter(r -> "busy".equals(r.getStatus())).count();
        long pendingTasks = taskRepository.findByStatus("pending").size();

        stats.put("totalRobots", totalRobots);
        stats.put("idleRobots", idleRobots);
        stats.put("busyRobots", busyRobots);
        stats.put("pendingTasks", pendingTasks);
        stats.put("activeQueues", queueService.findActiveQueues().size());

        return stats;
    }
}
