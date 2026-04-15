package rpa.service;

import rpa.entity.Task;
import rpa.entity.RpaProcess;
import rpa.repository.TaskRepository;
import rpa.repository.RpaProcessRepository;
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
 * </p>
 */
@Slf4j
@Service
public class TaskExecutionService {

    private final TaskRepository taskRepository;
    private final RpaProcessRepository processRepository;
    private final CredentialService credentialService;
    private final AuditLogService auditLogService;

    // 任务上下文缓存（用于存储执行中的凭据）
    private final Map<Long, TaskContext> taskContextCache = new ConcurrentHashMap<>();

    public TaskExecutionService(
            TaskRepository taskRepository,
            RpaProcessRepository processRepository,
            CredentialService credentialService,
            AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.processRepository = processRepository;
        this.credentialService = credentialService;
        this.auditLogService = auditLogService;
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

        log.info("开始执行任务: id={}, name={}", taskId, taskName);
        task.setStartTime(LocalDateTime.now());
        task.setStatus("running");
        taskRepository.save(task);

        TaskContext context = null;

        try {
            // 1. 获取任务关联的凭据（如果有）
            context = prepareTaskContext(task);
            taskContextCache.put(taskId, context);

            // 2. 获取关联的流程/机器人代码
            String robotCode = getRobotCode(task);

            // 3. 执行机器人代码（使用凭据）
            ExecutionResult result = executeRobotCode(robotCode, context);

            // 4. 更新任务状态
            if (result.isSuccess()) {
                task.setStatus("completed");
                task.setResultData(result.getResultData());
                task.setErrorMessage(null);
                auditLogService.logTaskExecute(taskId, taskName, true, null);
            } else {
                task.setStatus("failed");
                task.setErrorMessage(result.getErrorMessage());
                auditLogService.logTaskExecute(taskId, taskName, false, result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("任务执行失败: id={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            auditLogService.logTaskExecute(taskId, taskName, false, e.getMessage());

        } finally {
            // 5. 清理任务上下文
            taskContextCache.remove(taskId);
            if (context != null) {
                context.clear(); // 清理敏感数据
            }
            task.setEndTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskRepository.save(task);
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
     * 获取机器人代码
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
     * 执行机器人代码
     */
    private ExecutionResult executeRobotCode(String robotCode, TaskContext context) {
        if (robotCode == null || robotCode.isEmpty()) {
            return ExecutionResult.fail("机器人代码为空");
        }

        try {
            log.info("开始执行机器人代码, 上下文包含凭据: username={}, hasPassword={}",
                    context.getUsername(), context.getPassword() != null);

            // TODO: 调用实际的机器人执行器
            // 这里可以调用 RobotCodeExecutor 或其他执行服务

            // 模拟执行结果
            StringBuilder result = new StringBuilder();
            result.append("// 执行完成\n");
            result.append("// 任务上下文信息:\n");

            if (context.getUsername() != null) {
                result.append("// - 用户名: ").append(context.getUsername()).append("\n");
            }
            if (context.getUrl() != null) {
                result.append("// - 目标URL: ").append(context.getUrl()).append("\n");
            }

            result.append("// - 执行时间: ").append(LocalDateTime.now()).append("\n");

            return ExecutionResult.success(result.toString());

        } catch (Exception e) {
            return ExecutionResult.fail("执行失败: " + e.getMessage());
        }
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