package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.ExecutionLog;
import rpa.repository.ExecutionLogRepository;
import java.util.List;

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

    public ExecutionLog create(Long taskId, Long processId, Long robotId, 
                               String action, String status, String message) {
        ExecutionLog log = new ExecutionLog();
        log.setTaskId(taskId);
        log.setProcessId(processId);
        log.setRobotId(robotId);
        log.setAction(action);
        log.setStatus(status);
        log.setMessage(message);
        return repository.save(log);
    }
}
