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

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository repository;
    private final RobotRepository robotRepository;

    public List<Task> findAll() {
        return repository.findAll();
    }

    public List<Task> findByAssigneeId(Long assigneeId) {
        return repository.findByAssigneeId(assigneeId);
    }

    public List<Task> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    public Optional<Task> findById(Long id) {
        return repository.findById(id);
    }

    public Task create(String name, String category, String priority, 
                       Long processId, String processName, Long assigneeId, String assigneeName) {
        Task task = new Task();
        task.setName(name);
        task.setCategory(category);
        task.setPriority(priority);
        task.setProcessId(processId);
        task.setProcessName(processName);
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(assigneeName);
        return repository.save(task);
    }

    public Task assignRobot(Long taskId, Long robotId) {
        return repository.findById(taskId).map(task -> {
            task.setRobotId(robotId);
            Robot robot = robotRepository.findById(robotId)
                    .orElseThrow(() -> new RuntimeException("机器人不存在"));
            task.setRobotName(robot.getName());
            return repository.save(task);
        }).orElseThrow(() -> new RuntimeException("任务不存在"));
    }

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

    public Task update(Long taskId, String name, String category, String priority) {
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
            return repository.save(task);
        }).orElseThrow(() -> new RuntimeException("任务不存在"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
