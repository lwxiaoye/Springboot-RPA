package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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

    private final Map<Long, Long> queueTaskCount = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 10000)
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
     * 调度任务到合适的机器人
     */
    public boolean dispatchTask(Task task) {
        if (task.getProcessId() == null && (task.getProcessIds() == null || task.getProcessIds().isEmpty())) {
            log.warn("任务 {} 未关联流程，无法调度", task.getId());
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
            log.warn("未找到关联的流程");
            return false;
        }

        String requiredCategory = process.getRequiredCategory();

        List<Robot> availableRobots = findAvailableRobots(requiredCategory, task.getRobotId());

        if (availableRobots.isEmpty()) {
            log.warn("没有可用的机器人执行任务: {}", task.getName());
            return false;
        }

        Robot selectedRobot = availableRobots.get(0);

        task.setRobotId(selectedRobot.getId());
        task.setRobotName(selectedRobot.getName());
        task.setStatus("assigned");
        taskRepository.save(task);

        selectedRobot.setStatus("busy");
        selectedRobot.setCurrentTaskCount(selectedRobot.getCurrentTaskCount() + 1);
        selectedRobot.setLastExecutionTime(java.time.LocalDateTime.now());
        robotRepository.save(selectedRobot);

        executionLogService.create(
            task.getId(),
            task.getProcessId(),
            selectedRobot.getId(),
            "任务分配",
            "assigned",
            "任务已分配给机器人: " + selectedRobot.getName()
        );

        if (process.getQueueId() != null) {
            queueService.incrementRunningCount(process.getQueueId());
            queueService.decrementPendingCount(process.getQueueId());
        }

        log.info("任务 {} 已分配给机器人 {}", task.getName(), selectedRobot.getName());

        return true;
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

            if (task.getProcessId() != null) {
                processRepository.findById(task.getProcessId()).ifPresent(process -> {
                    if (process.getQueueId() != null) {
                        queueService.decrementRunningCount(process.getQueueId());
                        if ("completed".equals(status) || "success".equals(status)) {
                            queueService.incrementCompletedCount(process.getQueueId());
                        } else {
                            queueService.incrementFailedCount(process.getQueueId());
                        }
                    }
                });
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
    @Scheduled(fixedRate = 30000)
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
