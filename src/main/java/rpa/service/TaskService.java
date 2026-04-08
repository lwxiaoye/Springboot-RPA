package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.Task;
import rpa.entity.Robot;
import rpa.repository.TaskRepository;
import rpa.repository.RobotRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务服务类
 * <p>
 * 提供任务相关的业务逻辑处理，包括任务CRUD、分配和状态管理。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository repository;
    private final RobotRepository robotRepository;

    /**
     * 查询所有任务
     */
    public List<Task> findAll() {
        return repository.findAll();
    }

    /**
     * 根据接收人查询任务
     */
    public List<Task> findByAssigneeId(Long assigneeId) {
        return repository.findByAssigneeId(assigneeId);
    }

    /**
     * 根据状态查询任务
     */
    public List<Task> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    /**
     * 根据ID查询任务
     */
    public Optional<Task> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建任务
     *
     * @param name 任务名称
     * @param category 分类
     * @param priority 优先级
     * @param processId 流程ID（单个）
     * @param processIds 多个流程ID（JSON格式数组）
     * @param processName 流程名称（单个）
     * @param processNames 多个流程名称（JSON格式数组）
     * @param assigneeId 接收人ID
     * @param assigneeName 接收人名称
     * @return 创建的任务
     */
    public Task create(String name, String category, String priority,
                       Long processId, String processIds, String processName, String processNames,
                       Long assigneeId, String assigneeName) {
        Task task = new Task();
        task.setName(name);
        task.setCategory(category);
        task.setPriority(priority);
        task.setProcessId(processId);
        task.setProcessIds(processIds);
        task.setProcessName(processName);
        task.setProcessNames(processNames);
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(assigneeName);
        return repository.save(task);
    }

    /**
     * 分配机器人到任务
     *
     * @param taskId 任务ID
     * @param robotId 机器人ID
     * @return 更新后的任务
     */
    public Task assignRobot(Long taskId, Long robotId) {
        return repository.findById(taskId).map(task -> {
            task.setRobotId(robotId);
            Robot robot = robotRepository.findById(robotId)
                    .orElseThrow(() -> new RuntimeException("机器人不存在"));
            task.setRobotName(robot.getName());
            return repository.save(task);
        }).orElseThrow(() -> new RuntimeException("任务不存在"));
    }

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 状态
     * @param resultData 结果数据
     * @param errorMessage 错误信息
     * @return 更新后的任务
     */
    public Task updateStatus(Long taskId, String status, String resultData, String errorMessage) {
        return repository.findById(taskId).map(task -> {
            task.setStatus(status);
            task.setResultData(resultData);
            task.setErrorMessage(errorMessage);
            if ("running".equals(status)) {
                task.setStartTime(LocalDateTime.now());
            } else if ("completed".equals(status) || "failed".equals(status)) {
                task.setEndTime(LocalDateTime.now());
            }
            return repository.save(task);
        }).orElseThrow(() -> new RuntimeException("任务不存在"));
    }

    /**
     * 更新任务信息
     *
     * @param taskId 任务ID
     * @param name 名称
     * @param category 分类
     * @param priority 优先级
     * @param processId 流程ID（单个）
     * @param processIds 多个流程ID（JSON格式数组）
     * @param processName 流程名称（单个）
     * @param processNames 多个流程名称（JSON格式数组）
     * @return 更新后的任务
     */
    public Task update(Long taskId, String name, String category, String priority,
                      Long processId, String processIds, String processName, String processNames) {
        return repository.findById(taskId).map(task -> {
            if (name != null) {
                task.setName(name);
            }
            if (category != null) {
                task.setCategory(category);
            }
            if (priority != null) {
                task.setPriority(priority);
            }
            if (processId != null) {
                task.setProcessId(processId);
            }
            if (processIds != null) {
                task.setProcessIds(processIds);
            }
            if (processName != null) {
                task.setProcessName(processName);
            }
            if (processNames != null) {
                task.setProcessNames(processNames);
            }
            return repository.save(task);
        }).orElseThrow(() -> new RuntimeException("任务不存在"));
    }

    /**
     * 删除任务
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
