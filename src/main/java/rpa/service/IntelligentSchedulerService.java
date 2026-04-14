package rpa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.*;
import rpa.repository.DeadLetterQueueRepository;
import rpa.repository.TaskRepository;
import rpa.repository.TaskQueueRepository;
import rpa.repository.RobotRepository;
import rpa.repository.TaskDependencyRepository;
import rpa.repository.RobotHealthRepository;
import rpa.repository.RobotBackupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IntelligentSchedulerService {

    private final TaskRepository taskRepository;
    private final TaskQueueRepository queueRepository;
    private final RobotRepository robotRepository;
    private final DeadLetterQueueRepository dlqRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final RobotHealthRepository robotHealthRepository;
    private final RobotBackupRepository robotBackupRepository;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    private final Map<String, LoadBalanceStrategy> loadBalanceStrategies = new ConcurrentHashMap<>();

    public IntelligentSchedulerService(
            TaskRepository taskRepository,
            TaskQueueRepository queueRepository,
            RobotRepository robotRepository,
            DeadLetterQueueRepository deadLetterQueueRepository,
            TaskDependencyRepository taskDependencyRepository,
            RobotHealthRepository robotHealthRepository,
            RobotBackupRepository robotBackupRepository,
            AlertService alertService,
            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.queueRepository = queueRepository;
        this.robotRepository = robotRepository;
        this.dlqRepository = deadLetterQueueRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.robotHealthRepository = robotHealthRepository;
        this.robotBackupRepository = robotBackupRepository;
        this.alertService = alertService;
        this.objectMapper = objectMapper;

        loadBalanceStrategies.put("idle", new IdleLoadBalanceStrategy());
        loadBalanceStrategies.put("resource", new ResourceLoadBalanceStrategy());
        loadBalanceStrategies.put("success_rate", new SuccessRateLoadBalanceStrategy());
    }

    public Optional<Robot> scheduleTask(Task task) {
        TaskQueue queue = queueRepository.findById(task.getQueueId()).orElse(null);
        if (queue == null || !"active".equals(queue.getStatus())) {
            log.warn("任务 {} 无法调度：队列不存在或未启用", task.getName());
            return Optional.empty();
        }

        if (!canProcessTask(queue, task)) {
            log.warn("任务 {} 无法调度：队列 {} 不支持该任务类型", task.getName(), queue.getName());
            return Optional.empty();
        }

        List<Robot> availableRobots = getAvailableRobots(queue, task);

        if (availableRobots.isEmpty()) {
            log.warn("任务 {} 无法调度：没有可用的机器人", task.getName());
            return Optional.empty();
        }

        Robot selectedRobot = selectBestRobot(availableRobots, queue);

        if (selectedRobot != null) {
            assignTaskToRobot(task, selectedRobot, queue);
            log.info("任务 {} 已分配给机器人 {}", task.getName(), selectedRobot.getName());
            return Optional.of(selectedRobot);
        }

        return Optional.empty();
    }

    private boolean canProcessTask(TaskQueue queue, Task task) {
        if (queue.getRequiredCategories() != null && !queue.getRequiredCategories().isEmpty()) {
            try {
                List<String> requiredCategories = objectMapper.readValue(
                    queue.getRequiredCategories(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
                if (!requiredCategories.isEmpty() && task.getCategory() != null) {
                    return requiredCategories.contains(task.getCategory());
                }
            } catch (Exception e) {
                log.error("解析队列分类失败", e);
            }
        }
        return true;
    }

    private List<Robot> getAvailableRobots(TaskQueue queue, Task task) {
        List<Robot> allRobots = robotRepository.findAll();
        List<Robot> available = new ArrayList<>();

        for (Robot robot : allRobots) {
            if (!"idle".equals(robot.getStatus())) {
                continue;
            }

            RobotHealth health = robotHealthRepository
                .findTopByRobotIdOrderByCheckTimeDesc(robot.getId())
                .orElse(null);
            if (health != null && "offline".equals(health.getHealthStatus())) {
                continue;
            }

            if ("robot".equals(queue.getExclusivityMode())) {
                String exclusiveRobotIds = queue.getExclusiveRobotIds();
                if (exclusiveRobotIds != null && exclusiveRobotIds.contains(robot.getId().toString())) {
                    continue;
                }
            }

            if (robot.getCurrentTaskCount() != null && robot.getMaxConcurrentTasks() != null &&
                robot.getCurrentTaskCount() >= robot.getMaxConcurrentTasks()) {
                continue;
            }

            if (queue.getRequiredCategories() != null && !queue.getRequiredCategories().isEmpty()) {
                try {
                    List<String> requiredCategories = objectMapper.readValue(
                        queue.getRequiredCategories(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                    );
                    if (!requiredCategories.isEmpty()) {
                        String robotCategory = robot.getRobotCategory();
                        if (!requiredCategories.contains(robotCategory)) {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    log.error("解析机器人分类失败", e);
                }
            }

            available.add(robot);
        }

        available.sort((r1, r2) -> {
            RobotHealth h1 = robotHealthRepository.findTopByRobotIdOrderByCheckTimeDesc(r1.getId()).orElse(null);
            RobotHealth h2 = robotHealthRepository.findTopByRobotIdOrderByCheckTimeDesc(r2.getId()).orElse(null);
            int score1 = h1 != null && h1.getHealthScore() != null ? h1.getHealthScore() : 100;
            int score2 = h2 != null && h2.getHealthScore() != null ? h2.getHealthScore() : 100;
            return Integer.compare(score2, score1);
        });

        return available;
    }

    private Robot selectBestRobot(List<Robot> robots, TaskQueue queue) {
        if (robots.isEmpty()) return null;
        if (robots.size() == 1) return robots.get(0);
        LoadBalanceStrategy strategy = loadBalanceStrategies.get("resource");
        if (strategy != null) {
            return strategy.select(robots);
        }
        return robots.get(0);
    }

    private void assignTaskToRobot(Task task, Robot robot, TaskQueue queue) {
        task.setRobotId(robot.getId());
        task.setRobotName(robot.getName());
        task.setStatus("running");
        task.setStartTime(LocalDateTime.now());
        taskRepository.save(task);

        robot.setCurrentTaskCount(robot.getCurrentTaskCount() != null ? robot.getCurrentTaskCount() + 1 : 1);
        robot.setStatus("busy");
        robot.setLastExecutionTime(LocalDateTime.now());
        robotRepository.save(robot);

        queue.setCurrentRunningCount(queue.getCurrentRunningCount() != null ? queue.getCurrentRunningCount() + 1 : 1);
        queue.setCurrentPendingCount(Math.max(0, queue.getCurrentPendingCount() != null ? queue.getCurrentPendingCount() - 1 : 0));
        queueRepository.save(queue);
    }

    public void handleTaskFailure(Task task, String errorMessage, String errorStack) {
        TaskQueue queue = queueRepository.findById(task.getQueueId()).orElse(null);

        DeadLetterQueue deadLetter = new DeadLetterQueue();
        deadLetter.setOriginalQueueId(queue != null ? queue.getId() : null);
        deadLetter.setOriginalQueueName(queue != null ? queue.getName() : null);
        deadLetter.setTaskId(task.getId());
        deadLetter.setTaskName(task.getName());
        deadLetter.setProcessId(task.getProcessId());
        deadLetter.setProcessName(task.getProcessName());
        deadLetter.setRobotId(task.getRobotId());
        deadLetter.setRobotName(task.getRobotName());
        deadLetter.setErrorMessage(errorMessage);
        deadLetter.setErrorStack(errorStack);
        deadLetter.setRetryCount(task.getRetryCount() != null ? task.getRetryCount() : 0);
        deadLetter.setMaxRetry(queue != null && queue.getMaxRetryPerTask() != null ? queue.getMaxRetryPerTask() : 3);
        deadLetter.setStatus("pending");
        deadLetter.setTraceId(task.getTraceId());
        dlqRepository.save(deadLetter);

        task.setStatus("failed");
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());
        taskRepository.save(task);

        if (queue != null) {
            queue.setFailedCount(queue.getFailedCount() != null ? queue.getFailedCount() + 1 : 1L);
            queue.setCurrentRunningCount(Math.max(0, queue.getCurrentRunningCount() != null ? queue.getCurrentRunningCount() - 1 : 0));
            queueRepository.save(queue);
        }

        releaseRobot(task.getRobotId());
        alertService.sendTaskFailureAlert(task, errorMessage);
        log.warn("任务 {} 失败，已移入死信队列", task.getName());
    }

    public boolean processDeadLetter(Long deadLetterId, String resolutionType, String comment) {
        DeadLetterQueue deadLetter = dlqRepository.findById(deadLetterId).orElse(null);
        if (deadLetter == null) {
            return false;
        }

        deadLetter.setStatus("resolved");
        deadLetter.setResolutionType(resolutionType);
        deadLetter.setResolutionComment(comment);
        deadLetter.setResolvedAt(LocalDateTime.now());
        dlqRepository.save(deadLetter);

        if ("retry".equals(resolutionType)) {
            Task task = taskRepository.findById(deadLetter.getTaskId()).orElse(null);
            if (task != null) {
                task.setStatus("pending");
                task.setRetryCount(deadLetter.getRetryCount() + 1);
                task.setRobotId(null);
                task.setRobotName(null);
                task.setErrorMessage(null);
                taskRepository.save(task);
                scheduleTask(task);
            }
        }

        return true;
    }

    private void releaseRobot(Long robotId) {
        if (robotId == null) return;

        Robot robot = robotRepository.findById(robotId).orElse(null);
        if (robot != null) {
            robot.setCurrentTaskCount(Math.max(0, robot.getCurrentTaskCount() != null ? robot.getCurrentTaskCount() - 1 : 0));
            List<Task> runningTasks = taskRepository.findByRobotIdAndStatus(robotId, "running");
            if (runningTasks.isEmpty()) {
                robot.setStatus("idle");
            }
            robotRepository.save(robot);
        }
    }

    public void handleTaskCompletion(Task task, String status) {
        TaskQueue queue = queueRepository.findById(task.getQueueId()).orElse(null);

        task.setStatus(status);
        task.setEndTime(LocalDateTime.now());
        taskRepository.save(task);

        if (queue != null) {
            queue.setCurrentRunningCount(Math.max(0, queue.getCurrentRunningCount() != null ? queue.getCurrentRunningCount() - 1 : 0));
            if ("completed".equals(status) || "success".equals(status)) {
                queue.setCompletedCount(queue.getCompletedCount() != null ? queue.getCompletedCount() + 1 : 1L);
            } else {
                queue.setFailedCount(queue.getFailedCount() != null ? queue.getFailedCount() + 1 : 1L);
            }
            queueRepository.save(queue);
        }

        releaseRobot(task.getRobotId());
        processDependentTasks(task);
    }

    private void processDependentTasks(Task completedTask) {
        List<TaskDependency> dependencies = taskDependencyRepository.findByParentTaskId(completedTask.getId());
        for (TaskDependency dep : dependencies) {
            Task parent = taskRepository.findById(dep.getParentTaskId()).orElse(null);
            if (parent != null) {
                List<Task> parentTasks = Collections.singletonList(parent);
                boolean allParentsCompleted = parentTasks.stream()
                    .allMatch(t -> "completed".equals(t.getStatus()) || "success".equals(t.getStatus()));

                if (allParentsCompleted) {
                    Task currentTask = taskRepository.findById(dep.getId()).orElse(null);
                    if (currentTask != null && "pending".equals(currentTask.getStatus())) {
                        scheduleTask(currentTask);
                    }
                }
            }
        }
    }

    public void checkAndSwitchRobot(Long robotId) {
        Robot robot = robotRepository.findById(robotId).orElse(null);
        if (robot == null) return;

        RobotHealth health = robotHealthRepository
            .findTopByRobotIdOrderByCheckTimeDesc(robotId)
            .orElse(null);

        if (health == null) return;

        boolean needSwitch = false;
        String reason = "";

        if (Boolean.TRUE.equals(health.getHeartbeatTimeout())) {
            needSwitch = true;
            reason = "心跳超时";
        } else if (health.getHealthScore() != null && health.getHealthScore() < 60) {
            needSwitch = true;
            reason = "健康评分过低";
        }

        if (needSwitch) {
            List<RobotBackup> backups = robotBackupRepository.findByPrimaryRobotIdAndStatus(robotId, "active");
            for (RobotBackup backup : backups) {
                if (Boolean.TRUE.equals(backup.getAutoSwitch())) {
                    switchToBackup(backup, reason);
                }
            }
        }
    }

    private void switchToBackup(RobotBackup backup, String reason) {
        Robot primary = robotRepository.findById(backup.getPrimaryRobotId()).orElse(null);
        if (primary != null) {
            primary.setStatus("offline");
            robotRepository.save(primary);
        }

        Robot backupRobot = robotRepository.findById(backup.getBackupRobotId()).orElse(null);
        if (backupRobot != null) {
            backupRobot.setStatus("idle");
            robotRepository.save(backupRobot);
        }

        backup.setStatus("switched");
        backup.setLastSwitchTime(LocalDateTime.now());
        backup.setSwitchReason(reason);
        backup.setSwitchCount(backup.getSwitchCount() != null ? backup.getSwitchCount() + 1 : 1);
        robotBackupRepository.save(backup);

        log.warn("机器人 {} 已切换到备用机器人 {}，原因：{}", backup.getPrimaryRobotName(), backup.getBackupRobotName(), reason);
        alertService.sendRobotSwitchAlert(backup, reason);
    }

    interface LoadBalanceStrategy {
        Robot select(List<Robot> robots);
    }

    static class IdleLoadBalanceStrategy implements LoadBalanceStrategy {
        @Override
        public Robot select(List<Robot> robots) {
            return robots.stream()
                .min(Comparator.comparingInt(r -> r.getCurrentTaskCount() != null ? r.getCurrentTaskCount() : 0))
                .orElse(robots.get(0));
        }
    }

    static class ResourceLoadBalanceStrategy implements LoadBalanceStrategy {
        @Override
        public Robot select(List<Robot> robots) {
            return robots.get(0);
        }
    }

    static class SuccessRateLoadBalanceStrategy implements LoadBalanceStrategy {
        @Override
        public Robot select(List<Robot> robots) {
            return robots.stream()
                .max(Comparator.comparingDouble(r -> {
                    int total = r.getTotalExecutions() != null ? r.getTotalExecutions() : 0;
                    int success = r.getSuccessExecutions() != null ? r.getSuccessExecutions() : 0;
                    if (total == 0) return 1.0;
                    return (double) success / total;
                }))
                .orElse(robots.get(0));
        }
    }
}
