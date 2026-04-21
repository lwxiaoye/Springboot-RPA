package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.ExecutionLog;
import rpa.repository.ExecutionLogRepository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行日志服务类
 * <p>
 * 提供执行日志相关的业务逻辑处理。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class ExecutionLogService {

    private final ExecutionLogRepository repository;

    /**
     * 查询所有执行日志
     */
    public List<ExecutionLog> findAll() {
        return repository.findAll();
    }

    /**
     * 根据任务ID查询执行日志
     */
    public List<ExecutionLog> findByTaskId(Long taskId) {
        return repository.findByTaskId(taskId);
    }

    /**
     * 根据流程ID查询执行日志
     */
    public List<ExecutionLog> findByProcessId(Long processId) {
        return repository.findByProcessId(processId);
    }

    /**
     * 根据机器人ID查询执行日志
     */
    public List<ExecutionLog> findByRobotId(Long robotId) {
        return repository.findByRobotId(robotId);
    }

    /**
     * 根据ID查询执行日志
     */
    public Optional<ExecutionLog> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 删除执行日志
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 创建执行日志（简化版）
     *
     * @param taskId 任务ID
     * @param processId 流程ID
     * @param robotId 机器人ID
     * @param action 执行动作
     * @param status 状态
     * @param message 消息
     * @return 创建的日志
     */
    public ExecutionLog create(Long taskId, Long processId, Long robotId,
                              String action, String status, String message) {
        ExecutionLog log = new ExecutionLog();
        log.setTaskId(taskId);
        log.setProcessId(processId);
        log.setRobotId(robotId);
        log.setAction(action);
        log.setStatus(status);
        log.setMessage(message);
        log.setCreateTime(java.time.LocalDateTime.now());
        return repository.save(log);
    }

    /**
     * 创建执行日志（完整版）
     *
     * @param taskId 任务ID
     * @param processId 流程ID
     * @param robotId 机器人ID
     * @param taskName 任务名称
     * @param robotName 机器人名称
     * @param action 执行动作
     * @param status 状态
     * @param message 消息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param duration 执行耗时
     * @param steps 执行步骤
     * @return 创建的日志
     */
    public ExecutionLog create(Long taskId, Long processId, Long robotId,
                              String taskName, String robotName,
                              String action, String status, String message,
                              LocalDateTime startTime, LocalDateTime endTime,
                              String duration, String steps) {
        ExecutionLog log = new ExecutionLog();
        log.setTaskId(taskId);
        log.setProcessId(processId);
        log.setRobotId(robotId);
        log.setTaskName(taskName);
        log.setRobotName(robotName);
        log.setAction(action);
        log.setStatus(status);
        log.setMessage(message);
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        log.setDuration(duration);
        log.setSteps(steps);
        log.setCreateTime(java.time.LocalDateTime.now());
        return repository.save(log);
    }

    /**
     * 查询最近的执行日志
     *
     * @param limit 返回数量限制
     * @return List<ExecutionLog> 日志列表
     */
    public List<ExecutionLog> findRecentLogs(int limit) {
        return repository.findAllOrderByCreateTimeDesc().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 统计今日执行次数
     */
    public long countTodayExecutions() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return repository.findAllOrderByCreateTimeDesc().stream()
                .filter(log -> log.getCreateTime() != null && log.getCreateTime().isAfter(startOfDay))
                .count();
    }

    /**
     * 获取今日成功率
     */
    public double getTodaySuccessRate() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<ExecutionLog> todayLogs = repository.findAllOrderByCreateTimeDesc().stream()
                .filter(log -> log.getCreateTime() != null && log.getCreateTime().isAfter(startOfDay))
                .collect(Collectors.toList());

        if (todayLogs.isEmpty()) {
            return 100.0;
        }

        long successCount = todayLogs.stream()
                .filter(log -> "completed".equals(log.getStatus()))
                .count();

        return Math.round((double) successCount / todayLogs.size() * 100 * 100) / 100.0;
    }

    /**
     * 获取每周执行趋势数据
     */
    public List<Map<String, Object>> getWeeklyExecutionTrend() {
        List<Map<String, Object>> trend = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<ExecutionLog> dayLogs = repository.findAllOrderByCreateTimeDesc().stream()
                    .filter(log -> log.getCreateTime() != null
                            && log.getCreateTime().isAfter(dayStart)
                            && log.getCreateTime().isBefore(dayEnd))
                    .collect(Collectors.toList());

            long successCount = dayLogs.stream()
                    .filter(log -> "completed".equals(log.getStatus()))
                    .count();

            Map<String, Object> dayData = new java.util.LinkedHashMap<>();
            dayData.put("date", date.toString());
            dayData.put("executions", dayLogs.size());
            dayData.put("success", successCount);
            dayData.put("failed", dayLogs.size() - successCount);
            trend.add(dayData);
        }

        return trend;
    }
}
