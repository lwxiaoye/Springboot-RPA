package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.TaskQueue;
import rpa.repository.TaskQueueRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务队列服务类
 * <p>
 * 提供任务队列的CRUD操作和队列统计功能。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskQueueService {

    private final TaskQueueRepository repository;

    public List<TaskQueue> findAll() {
        return repository.findAll();
    }

    public Optional<TaskQueue> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<TaskQueue> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<TaskQueue> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    public List<TaskQueue> findActiveQueues() {
        return repository.findActiveQueues();
    }

    @Transactional
    public TaskQueue create(String name, String code, String description, String status,
                            Integer priorityLevel, Integer maxConcurrentTasks,
                            String processIds, String processNames, String requiredCategories,
                            String department, String creator, String remark) {
        TaskQueue queue = new TaskQueue();
        queue.setName(name);
        queue.setCode(code);
        queue.setDescription(description);
        queue.setStatus(status != null ? status : "active");
        queue.setPriorityLevel(priorityLevel != null ? priorityLevel : 2);
        queue.setMaxConcurrentTasks(maxConcurrentTasks != null ? maxConcurrentTasks : 5);
        queue.setProcessIds(processIds);
        queue.setProcessNames(processNames);
        queue.setRequiredCategories(requiredCategories);
        queue.setDepartment(department);
        queue.setCreator(creator);
        queue.setRemark(remark);
        queue.setEnabled(true);
        queue.setCreateTime(LocalDateTime.now());
        queue.setUpdateTime(LocalDateTime.now());
        queue.setCurrentPendingCount(0);
        queue.setCurrentRunningCount(0);
        queue.setCompletedCount(0L);
        queue.setFailedCount(0L);
        return repository.save(queue);
    }

    @Transactional
    public TaskQueue update(Long id, String name, String code, String description, String status,
                           Integer priorityLevel, Integer maxConcurrentTasks,
                           String processIds, String processNames, String requiredCategories,
                           String department, String remark, Boolean enabled) {
        return repository.findById(id).map(queue -> {
            if (name != null) queue.setName(name);
            if (code != null) queue.setCode(code);
            if (description != null) queue.setDescription(description);
            if (status != null) queue.setStatus(status);
            if (priorityLevel != null) queue.setPriorityLevel(priorityLevel);
            if (maxConcurrentTasks != null) queue.setMaxConcurrentTasks(maxConcurrentTasks);
            if (processIds != null) queue.setProcessIds(processIds);
            if (processNames != null) queue.setProcessNames(processNames);
            if (requiredCategories != null) queue.setRequiredCategories(requiredCategories);
            if (department != null) queue.setDepartment(department);
            if (remark != null) queue.setRemark(remark);
            if (enabled != null) queue.setEnabled(enabled);
            queue.setUpdateTime(LocalDateTime.now());
            return repository.save(queue);
        }).orElseThrow(() -> new RuntimeException("队列不存在"));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public TaskQueue updateStatus(Long id, String status) {
        return repository.findById(id).map(queue -> {
            queue.setStatus(status);
            queue.setUpdateTime(LocalDateTime.now());
            return repository.save(queue);
        }).orElseThrow(() -> new RuntimeException("队列不存在"));
    }

    @Transactional
    public void incrementPendingCount(Long id) {
        repository.incrementPendingCount(id);
    }

    @Transactional
    public void decrementPendingCount(Long id) {
        repository.decrementPendingCount(id);
    }

    @Transactional
    public void incrementRunningCount(Long id) {
        repository.incrementRunningCount(id);
    }

    @Transactional
    public void decrementRunningCount(Long id) {
        repository.decrementRunningCount(id);
    }

    @Transactional
    public void incrementCompletedCount(Long id) {
        repository.incrementCompletedCount(id);
    }

    @Transactional
    public void incrementFailedCount(Long id) {
        repository.incrementFailedCount(id);
    }

    public boolean canAcceptTask(Long queueId) {
        return repository.findById(queueId).map(queue -> {
            return "active".equals(queue.getStatus())
                && queue.getEnabled()
                && queue.getCurrentRunningCount() < queue.getMaxConcurrentTasks();
        }).orElse(false);
    }
}
