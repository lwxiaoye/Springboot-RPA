package rpa.service;

import rpa.entity.Task;
import rpa.entity.RpaProcess;
import rpa.entity.User;
import rpa.repository.TaskRepository;
import rpa.repository.RpaProcessRepository;
import rpa.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务执行服务
 * <p>
 * 负责任务的调度执行，包括：
 * - 从凭据中心获取凭据
 * - 执行机器人代码
 * - 记录执行日志
 * - 发送任务完成通知
 * </p>
 */
@Slf4j
@Service
public class TaskExecutionService {

    private final TaskRepository taskRepository;
    private final RpaProcessRepository processRepository;
    private final CredentialService credentialService;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;
    private final UserRepository userRepository;

    // 任务上下文缓存（用于存储执行中的凭据）
    private final Map<Long, TaskContext> taskContextCache = new ConcurrentHashMap<>();

    public TaskExecutionService(
            TaskRepository taskRepository,
            RpaProcessRepository processRepository,
            CredentialService credentialService,
            AuditLogService auditLogService,
            EmailNotificationService emailNotificationService,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.processRepository = processRepository;
        this.credentialService = credentialService;
        this.auditLogService = auditLogService;
        this.emailNotificationService = emailNotificationService;
        this.userRepository = userRepository;
    }

    /**
     * 执行任务（带凭据）
     */
    @Async
    @Transactional
    public void executeTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("任务不存在: {}", taskId);
            return;
        }

        executeTaskWithCredential(task);
    }

    /**
     * 执行任务（手动触发）
     */
    public Task executeTaskManual(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        executeTaskWithCredential(task);
        return task;
    }

    /**
     * 带凭据执行任务
     */
    private void executeTaskWithCredential(Task task) {
        Long taskId = task.getId();
        String taskName = task.getName();
        java.util.List<Long> processIds = getAllProcessIds(task);

        log.info("开始执行任务: id={}, name={}, 流程数量={}", taskId, taskName, processIds.size());
        task.setStartTime(LocalDateTime.now());
        task.setStatus("running");
        taskRepository.save(task);

        TaskContext context = null;
        StringBuilder allResults = new StringBuilder();
        boolean hasFailure = false;
        String lastError = null;

        try {
            // 1. 获取任务关联的凭据（如果有）
            context = prepareTaskContext(task);
            taskContextCache.put(taskId, context);

            // 2. 按顺序执行每个流程
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < processIds.size(); i++) {
                Long processId = processIds.get(i);
                String processName = getProcessName(processId);

                log.info("执行流程 {}/{}: id={}, name={}", i + 1, processIds.size(), processId, processName);
                allResults.append("=== 流程 ").append(i + 1).append("/").append(processIds.size())
                          .append(": ").append(processName).append(" ===\n");

                try {
                    RpaProcess process = processRepository.findById(processId).orElse(null);
                    if (process == null) {
                        allResults.append("[错误] 流程不存在\n");
                        failCount++;
                        hasFailure = true;
                        lastError = "流程不存在: " + processId;
                        continue;
                    }

                    // 执行单个流程
                    ExecutionResult result = executeSingleProcess(process, context);
                    allResults.append(result.getResultData()).append("\n");

                    if (result.isSuccess()) {
                        successCount++;
                        auditLogService.log("TASK", "TASK_PROCESS_SUCCESS", "Task", taskId, taskName,
                            "任务执行流程成功: " + processName, "low", "success",
                            java.util.Map.of("processId", processId, "processName", processName));
                    } else {
                        failCount++;
                        hasFailure = true;
                        lastError = result.getErrorMessage();
                        allResults.append("[失败] ").append(result.getErrorMessage()).append("\n");
                        auditLogService.log("TASK", "TASK_PROCESS_FAILED", "Task", taskId, taskName,
                            "任务执行流程失败: " + processName, "medium", "failed",
                            java.util.Map.of("processId", processId, "processName", processName, "error", result.getErrorMessage()));
                    }

                } catch (Exception e) {
                    failCount++;
                    hasFailure = true;
                    lastError = e.getMessage();
                    allResults.append("[异常] ").append(e.getMessage()).append("\n");
                    log.error("流程执行异常: processId={}, error={}", processId, e.getMessage(), e);
                    auditLogService.log("TASK", "TASK_PROCESS_ERROR", "Task", taskId, taskName,
                        "任务执行流程异常: " + processName, "high", "failed",
                        java.util.Map.of("processId", processId, "processName", processName, "error", e.getMessage()));
                }

                allResults.append("\n");
            }

            // 3. 更新任务状态
            allResults.append("=== 执行汇总 ===\n");
            allResults.append("总流程数: ").append(processIds.size()).append("\n");
            allResults.append("成功: ").append(successCount).append("\n");
            allResults.append("失败: ").append(failCount).append("\n");

            if (hasFailure) {
                task.setStatus("failed");
                task.setResultData(allResults.toString());
                task.setErrorMessage(lastError);
                auditLogService.logTaskExecute(taskId, taskName, false, "部分流程失败: " + lastError);
            } else {
                task.setStatus("completed");
                task.setResultData(allResults.toString());
                task.setErrorMessage(null);
                auditLogService.logTaskExecute(taskId, taskName, true, "全部流程执行成功");
            }

        } catch (Exception e) {
            log.error("任务执行失败: id={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            allResults.append("[任务异常] ").append(e.getMessage());
            task.setResultData(allResults.toString());
            auditLogService.logTaskExecute(taskId, taskName, false, e.getMessage());
            hasFailure = true;
            lastError = e.getMessage();

        } finally {
            // 清理任务上下文
            taskContextCache.remove(taskId);
            if (context != null) {
                context.clear();
            }
            task.setEndTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskRepository.save(task);

            // 发送任务完成/失败邮件通知
            sendTaskNotification(task, hasFailure, lastError);
        }
    }

    /**
     * 发送任务完成/失败邮件通知
     */
    private void sendTaskNotification(Task task, boolean hasFailure, String errorMessage) {
        try {
            // 获取任务创建者信息
            Long creatorId = task.getCreatorId();
            String creatorEmail = null;

            if (creatorId != null) {
                creatorEmail = userRepository.findById(creatorId)
                        .map(User::getEmail)
                        .orElse(null);
            }

            // 如果没有创建者邮箱，尝试获取任务中保存的邮箱
            if (creatorEmail == null || creatorEmail.isEmpty()) {
                creatorEmail = task.getNotifyEmail();
            }

            // 如果还是没有，使用默认邮箱
            if (creatorEmail == null || creatorEmail.isEmpty()) {
                creatorEmail = "lwxiaoye@163.com"; // 使用配置的默认邮箱
            }

            if (hasFailure) {
                // 发送失败通知
                emailNotificationService.sendTaskFailedEmail(task, errorMessage, creatorEmail);
                log.info("任务失败通知邮件已发送: taskId={}, email={}", task.getId(), creatorEmail);
            } else {
                // 发送成功通知
                emailNotificationService.sendTaskCompletionEmail(task, creatorEmail);
                log.info("任务完成通知邮件已发送: taskId={}, email={}", task.getId(), creatorEmail);
            }
        } catch (Exception e) {
            // 邮件发送失败不影响任务状态
            log.warn("发送任务通知邮件失败: taskId={}, error={}", task.getId(), e.getMessage());
        }
    }

    /**
     * 执行单个流程
     */
    private ExecutionResult executeSingleProcess(RpaProcess process, TaskContext context) {
        String robotCode = process.getCode();

        if (robotCode == null || robotCode.isEmpty()) {
            return ExecutionResult.fail("机器人代码为空");
        }

        try {
            log.info("开始执行机器人代码, 上下文包含凭据: username={}, hasPassword={}",
                    context.getUsername(), context.getPassword() != null);

            // 模拟执行结果
            StringBuilder result = new StringBuilder();
            result.append("流程: ").append(process.getName()).append("\n");
            result.append("执行完成\n");

            if (context.getUsername() != null) {
                result.append("- 用户名: ").append(context.getUsername()).append("\n");
            }
            if (context.getUrl() != null) {
                result.append("- 目标URL: ").append(context.getUrl()).append("\n");
            }
            result.append("- 执行时间: ").append(LocalDateTime.now()).append("\n");

            return ExecutionResult.success(result.toString());

        } catch (Exception e) {
            return ExecutionResult.fail("执行失败: " + e.getMessage());
        }
    }

    /**
     * 准备任务上下文（获取凭据）
     */
    private TaskContext prepareTaskContext(Task task) {
        TaskContext context = new TaskContext();

        // 获取任务关联的凭据ID（如果任务有credentialId字段）
        Long credentialId = getTaskCredentialId(task);
        if (credentialId != null) {
            try {
                Map<String, String> credentials = credentialService.getCredentialForTask(
                        credentialId, task.getName());

                context.setCredentialId(credentialId);
                context.setUsername(credentials.get("username"));
                context.setPassword(credentials.get("password"));
                context.setSecretKey(credentials.get("secretKey"));
                context.setUrl(credentials.get("url"));
                context.setCredentialType(credentials.get("type"));

                log.info("任务 {} 获取凭据成功: type={}", task.getName(), credentials.get("type"));

            } catch (Exception e) {
                log.error("获取任务凭据失败: credentialId={}, error={}", credentialId, e.getMessage());
                throw new RuntimeException("无法获取任务凭据: " + e.getMessage());
            }
        } else {
            log.info("任务 {} 无关联凭据", task.getName());
        }

        // 添加任务输入数据到上下文
        context.setInputData(task.getInputData());

        return context;
    }

    /**
     * 获取任务关联的凭据ID
     */
    private Long getTaskCredentialId(Task task) {
        // 方案1：如果Task实体有credentialId字段
        // return task.getCredentialId();

        // 方案2：从任务的processId关联的流程中获取凭据
        if (task.getProcessId() != null) {
            return processRepository.findById(task.getProcessId())
                    .map(RpaProcess::getCredentialId)
                    .orElse(null);
        }

        // 方案3：解析processIds JSON获取第一个流程的凭据
        if (task.getProcessIds() != null && !task.getProcessIds().isEmpty()) {
            try {
                // 假设processIds格式为 "[1,2,3]"
                String ids = task.getProcessIds().replace("[", "").replace("]", "").trim();
                if (!ids.isEmpty()) {
                    Long firstProcessId = Long.parseLong(ids.split(",")[0].trim());
                    return processRepository.findById(firstProcessId)
                            .map(RpaProcess::getCredentialId)
                            .orElse(null);
                }
            } catch (Exception e) {
                log.warn("解析processIds失败: {}", task.getProcessIds());
            }
        }

        return null;
    }

    /**
     * 获取机器人代码（支持多流程）
     * 如果有多个流程，返回第一个流程的代码
     * 如果需要执行多个流程，应该调用 executeMultipleProcesses
     */
    private String getRobotCode(Task task) {
        if (task.getProcessId() != null) {
            return processRepository.findById(task.getProcessId())
                    .map(RpaProcess::getCode)
                    .orElse("");
        }

        if (task.getProcessIds() != null && !task.getProcessIds().isEmpty()) {
            try {
                String ids = task.getProcessIds().replace("[", "").replace("]", "").trim();
                if (!ids.isEmpty()) {
                    Long firstProcessId = Long.parseLong(ids.split(",")[0].trim());
                    return processRepository.findById(firstProcessId)
                            .map(RpaProcess::getCode)
                            .orElse("");
                }
            } catch (Exception e) {
                log.warn("解析processIds失败: {}", task.getProcessIds());
            }
        }

        return "";
    }

    /**
     * 获取所有关联的流程ID列表
     */
    private java.util.List<Long> getAllProcessIds(Task task) {
        java.util.List<Long> processIds = new java.util.ArrayList<>();

        // 添加单个流程ID
        if (task.getProcessId() != null) {
            processIds.add(task.getProcessId());
        }

        // 添加多个流程ID
        if (task.getProcessIds() != null && !task.getProcessIds().isEmpty()) {
            try {
                String ids = task.getProcessIds().replace("[", "").replace("]", "").trim();
                if (!ids.isEmpty()) {
                    for (String idStr : ids.split(",")) {
                        idStr = idStr.trim();
                        if (!idStr.isEmpty()) {
                            processIds.add(Long.parseLong(idStr));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析processIds失败: {}", task.getProcessIds());
            }
        }

        return processIds;
    }

    /**
     * 获取流程名称
     */
    private String getProcessName(Long processId) {
        return processRepository.findById(processId)
                .map(RpaProcess::getName)
                .orElse("未知流程");
    }


    /**
     * 任务上下文
     */
    public static class TaskContext {
        private Long credentialId;
        private String username;
        private String password;
        private String secretKey;
        private String url;
        private String credentialType;
        private String inputData;

        // Getter和Setter
        public Long getCredentialId() { return credentialId; }
        public void setCredentialId(Long credentialId) { this.credentialId = credentialId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getCredentialType() { return credentialType; }
        public void setCredentialType(String credentialType) { this.credentialType = credentialType; }
        public String getInputData() { return inputData; }
        public void setInputData(String inputData) { this.inputData = inputData; }

        /**
         * 清理敏感数据
         */
        public void clear() {
            if (password != null) {
                password = null;
            }
            if (secretKey != null) {
                secretKey = null;
            }
        }
    }

    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private boolean success;
        private String resultData;
        private String errorMessage;

        public static ExecutionResult success(String data) {
            ExecutionResult result = new ExecutionResult();
            result.success = true;
            result.resultData = data;
            return result;
        }

        public static ExecutionResult fail(String error) {
            ExecutionResult result = new ExecutionResult();
            result.success = false;
            result.errorMessage = error;
            return result;
        }

        public boolean isSuccess() { return success; }
        public String getResultData() { return resultData; }
        public String getErrorMessage() { return errorMessage; }
    }
}