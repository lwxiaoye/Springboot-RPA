package rpa.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_queue")
public class TaskQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(length = 500)
    private String description;

    private String status = "active";

    @Column(name = "priority_level")
    private Integer priorityLevel = 2;

    @Column(name = "max_concurrent_tasks")
    private Integer maxConcurrentTasks = 5;

    @Column(name = "current_pending_count")
    private Integer currentPendingCount = 0;

    @Column(name = "current_running_count")
    private Integer currentRunningCount = 0;

    @Column(name = "completed_count")
    private Long completedCount = 0L;

    @Column(name = "failed_count")
    private Long failedCount = 0L;

    @Column(length = 1000)
    private String processIds;

    @Column(length = 2000)
    private String processNames;

    @Column(length = 500)
    private String requiredCategories;

    @Column(length = 100)
    private String department;

    private String creator;

    private LocalDateTime createTime = LocalDateTime.now();

    private LocalDateTime updateTime = LocalDateTime.now();

    private Boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String remark;

    // 新增字段
    @Column(name = "priority_weight")
    private Integer priorityWeight = 10;

    @Column(name = "exclusivity_mode", length = 20)
    private String exclusivityMode = "none";

    @Column(name = "exclusive_robot_ids", columnDefinition = "TEXT")
    private String exclusiveRobotIds;

    @Column(name = "max_retry_per_task")
    private Integer maxRetryPerTask = 3;

    @Column(name = "retry_interval")
    private Integer retryInterval = 60;

    @Column(name = "timeout_per_task")
    private Integer timeoutPerTask = 3600;

    @Column(name = "dead_letter_enabled")
    private Boolean deadLetterEnabled = true;

    public TaskQueue() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(Integer priorityLevel) { this.priorityLevel = priorityLevel; }
    public Integer getMaxConcurrentTasks() { return maxConcurrentTasks; }
    public void setMaxConcurrentTasks(Integer maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }
    public Integer getCurrentPendingCount() { return currentPendingCount; }
    public void setCurrentPendingCount(Integer currentPendingCount) { this.currentPendingCount = currentPendingCount; }
    public Integer getCurrentRunningCount() { return currentRunningCount; }
    public void setCurrentRunningCount(Integer currentRunningCount) { this.currentRunningCount = currentRunningCount; }
    public Long getCompletedCount() { return completedCount; }
    public void setCompletedCount(Long completedCount) { this.completedCount = completedCount; }
    public Long getFailedCount() { return failedCount; }
    public void setFailedCount(Long failedCount) { this.failedCount = failedCount; }
    public String getProcessIds() { return processIds; }
    public void setProcessIds(String processIds) { this.processIds = processIds; }
    public String getProcessNames() { return processNames; }
    public void setProcessNames(String processNames) { this.processNames = processNames; }
    public String getRequiredCategories() { return requiredCategories; }
    public void setRequiredCategories(String requiredCategories) { this.requiredCategories = requiredCategories; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getPriorityWeight() { return priorityWeight; }
    public void setPriorityWeight(Integer priorityWeight) { this.priorityWeight = priorityWeight; }
    public String getExclusivityMode() { return exclusivityMode; }
    public void setExclusivityMode(String exclusivityMode) { this.exclusivityMode = exclusivityMode; }
    public String getExclusiveRobotIds() { return exclusiveRobotIds; }
    public void setExclusiveRobotIds(String exclusiveRobotIds) { this.exclusiveRobotIds = exclusiveRobotIds; }
    public Integer getMaxRetryPerTask() { return maxRetryPerTask; }
    public void setMaxRetryPerTask(Integer maxRetryPerTask) { this.maxRetryPerTask = maxRetryPerTask; }
    public Integer getRetryInterval() { return retryInterval; }
    public void setRetryInterval(Integer retryInterval) { this.retryInterval = retryInterval; }
    public Integer getTimeoutPerTask() { return timeoutPerTask; }
    public void setTimeoutPerTask(Integer timeoutPerTask) { this.timeoutPerTask = timeoutPerTask; }
    public Boolean getDeadLetterEnabled() { return deadLetterEnabled; }
    public void setDeadLetterEnabled(Boolean deadLetterEnabled) { this.deadLetterEnabled = deadLetterEnabled; }
}
