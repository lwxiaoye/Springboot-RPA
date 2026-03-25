package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.ExecutionLog;
import rpa.repository.ExecutionLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExecutionLogService {

    private final ExecutionLogRepository repository;

    public List<ExecutionLog> findAll() {
        return repository.findAll();
    }

    public List<ExecutionLog> findByTaskId(Long taskId) {
        return repository.findByTaskId(taskId);
    }

    public List<ExecutionLog> findByProcessId(Long processId) {
        return repository.findByProcessId(processId);
    }

    public List<ExecutionLog> findByRobotId(Long robotId) {
        return repository.findByRobotId(robotId);
    }

    public Optional<ExecutionLog> findById(Long id) {
        return repository.findById(id);
    }

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
}
