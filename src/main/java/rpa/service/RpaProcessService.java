package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.RpaProcess;
import rpa.entity.Robot;
import rpa.entity.ExecutionLog;
import rpa.repository.RpaProcessRepository;
import rpa.repository.RobotRepository;
import rpa.repository.ExecutionLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * RPA流程服务类
 * <p>
 * 提供RPA流程的创建、执行、监控等核心功能。
 * 支持步骤执行、机器人调度、执行日志记录等。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RpaProcessService {

    private final RpaProcessRepository repository;
    private final RobotRepository robotRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final RobotCodeExecutor robotCodeExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // 存储正在执行的任务
    private final Map<Long, Future<?>> executingTasks = new ConcurrentHashMap<>();

    /**
     * 查询所有流程
     */
    public List<RpaProcess> findAll() {
        return repository.findAll();
    }

    /**
     * 根据创建者查询流程
     */
    public List<RpaProcess> findByCreatorId(Long creatorId) {
        return repository.findByCreatorId(creatorId);
    }

    /**
     * 根据ID查询流程
     */
    public Optional<RpaProcess> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建流程
     *
     * @param name        名称
     * @param code        编码
     * @param description 描述
     * @param version     版本
     * @param status      状态
     * @param creatorId   创建者ID
     * @param creatorName 创建者名称
     * @return 创建的流程
     */
    public RpaProcess create(String name, String code, String description, String version, String status, Long creatorId, String creatorName) {
        RpaProcess process = new RpaProcess();
        process.setName(name);
        process.setCode(code);
        process.setDescription(description);
        process.setVersion(version != null ? version : "1.0.0");
        process.setStatus(status != null ? status : "draft");
        process.setCreatorId(creatorId);
        process.setCreatorName(creatorName);
        process.setTaskCount(0);
        return repository.save(process);
    }

    /**
     * 更新流程
     *
     * @param id          流程ID
     * @param name        名称
     * @param code        编码
     * @param description 描述
     * @param version     版本
     * @param status      状态
     * @return 更新后的流程
     */
    public RpaProcess update(Long id, String name, String code, String description, String version, String status) {
        return repository.findById(id).map(process -> {
            if (name != null) {
                process.setName(name);
            }
            if (code != null) {
                process.setCode(code);
            }
            if (description != null) {
                process.setDescription(description);
            }
            if (version != null) {
                process.setVersion(version);
            }
            if (status != null) {
                process.setStatus(status);
            }
            return repository.save(process);
        }).orElseThrow(() -> new RuntimeException("流程不存在"));
    }

    /**
     * 删除流程
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 保存流程设计
     *
     * @param id    流程 ID
     * @param steps 流程步骤（JSON 格式）
     * @return 更新后的流程
     */
    public RpaProcess saveDesign(Long id, String steps) {
        return repository.findById(id).map(process -> {
            process.setSteps(steps);
            return repository.save(process);
        }).orElseThrow(() -> new RuntimeException("流程不存在"));
    }

    /**
     * 执行流程（真实执行）
     *
     * @param processId 流程ID
     * @return 执行结果
     */
    public Map<String, Object> execute(Long processId) {
        final Long finalProcessId = processId;
        RpaProcess process = repository.findById(processId)
                .orElseThrow(() -> new RuntimeException("流程不存在"));

        // 解析步骤
        List<Map<String, Object>> steps = new ArrayList<>();
        if (process.getSteps() != null && !process.getSteps().isEmpty()) {
            try {
                // 尝试解析为JSON对象，判断是否为画布格式
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(process.getSteps());
                
                if (jsonNode.isObject() && jsonNode.has("nodes") && jsonNode.has("edges")) {
                    // 画布格式：{nodes:[...], edges:[...]}
                    // 需要转换为步骤数组格式
                    steps = convertCanvasToSteps(jsonNode);
                } else if (jsonNode.isArray()) {
                    // 旧步骤格式：[{name, type, robotId, config}, ...]
                    steps = objectMapper.readValue(process.getSteps(), new TypeReference<List<Map<String, Object>>>() {});
                } else {
                    throw new RuntimeException("流程步骤格式错误");
                }
            } catch (Exception e) {
                throw new RuntimeException("流程步骤解析失败: " + e.getMessage());
            }
        }
        
        final List<Map<String, Object>> finalSteps = steps;

        if (steps.isEmpty()) {
            throw new RuntimeException("流程没有设计步骤，请先设计流程");
        }

        // 检查每个步骤是否绑定了机器人
        for (int i = 0; i < finalSteps.size(); i++) {
            final int stepIndex = i;
            Map<String, Object> step = finalSteps.get(i);
            Object robotIdObj = step.get("robotId");
            if (robotIdObj == null) {
                throw new RuntimeException("步骤 " + (stepIndex + 1) + " 未绑定机器人");
            }
            Long robotId = robotIdObj instanceof Integer ? ((Integer) robotIdObj).longValue() : (Long) robotIdObj;
            Robot robot = robotRepository.findById(robotId)
                    .orElseThrow(() -> new RuntimeException("步骤 " + (stepIndex + 1) + " 绑定的机器人不存在"));

            if ("offline".equals(robot.getStatus())) {
                throw new RuntimeException("机器人 " + robot.getName() + " 处于离线状态，无法执行");
            }
        }

        // 生成执行ID
        String executionId = "EXEC-" + System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();

        // 创建执行日志记录
        ExecutionLog log = new ExecutionLog();
        log.setProcessId(finalProcessId);
        log.setTaskName(process.getName());
        log.setAction("流程执行");
        log.setStatus("running");
        log.setStartTime(startTime);
        log.setSteps(process.getSteps());
        log.setCreateTime(LocalDateTime.now());

        // 初始日志
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("=== 流程开始执行 ===\n");
        logMessage.append("流程名称: ").append(process.getName()).append("\n");
        logMessage.append("执行ID: ").append(executionId).append("\n");
        logMessage.append("开始时间: ").append(startTime).append("\n");
        logMessage.append("步骤数: ").append(finalSteps.size()).append("\n");
        logMessage.append("\n");

        ExecutionLog savedLog = executionLogRepository.save(log);
        final Long finalLogId = savedLog.getId();
        final String finalExecutionId = executionId;
        final LocalDateTime finalStartTime = startTime;

        // 在后台线程执行流程
        Future<?> taskFuture = executorService.submit(() -> {
            executeSteps(finalProcessId, process.getName(), finalSteps, finalLogId, finalExecutionId, finalStartTime);
        });

        executingTasks.put(processId, taskFuture);

        // 返回执行结果
        Map<String, Object> result = new HashMap<>();
        result.put("executionId", finalExecutionId);
        result.put("processId", finalProcessId);
        result.put("processName", process.getName());
        result.put("stepsCount", finalSteps.size());
        result.put("startTime", finalStartTime.toString());
        result.put("logId", finalLogId);

        return result;
    }

    /**
     * 真正执行流程步骤
     */
    private void executeSteps(Long processId, String processName, List<Map<String, Object>> steps,
                              Long logId, String executionId, LocalDateTime startTime) {
        StringBuilder fullLog = new StringBuilder();
        fullLog.append("=== 流程开始执行 ===\n");
        fullLog.append("流程名称: ").append(processName).append("\n");
        fullLog.append("执行ID: ").append(executionId).append("\n");
        fullLog.append("开始时间: ").append(startTime).append("\n");
        fullLog.append("步骤数: ").append(steps.size()).append("\n\n");

        boolean allSuccess = true;
        List<Map<String, Object>> stepResults = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            String stepName = (String) step.getOrDefault("name", "步骤" + (i + 1));
            String stepType = (String) step.getOrDefault("type", "");
            Object robotIdObj = step.get("robotId");
            Long robotId = robotIdObj instanceof Integer ? ((Integer) robotIdObj).longValue() : (Long) robotIdObj;
            Robot robot = robotRepository.findById(robotId).orElse(null);

            fullLog.append(">>> 开始执行步骤 ").append(i + 1).append(": ").append(stepName).append("\n");

            if (robot == null) {
                fullLog.append("[错误] 机器人不存在\n");
                allSuccess = false;
                continue;
            }

            fullLog.append("机器人: ").append(robot.getName()).append("\n");
            fullLog.append("分类: ").append(robot.getRobotCategory()).append("\n");

            // 更新机器人状态为忙碌
            robot.setStatus("busy");
            robotRepository.save(robot);

            try {
                // 获取步骤配置
                Map<String, Object> config = step.get("config") instanceof Map ?
                        (Map<String, Object>) step.get("config") : new HashMap<>();
                
                // 传递流程执行ID到机器人，用于Redis上下文共享
                config.put("processId", String.valueOf(processId));
                config.put("processName", processName);

                // 真正执行机器人的代码
                Map<String, Object> stepResult = executeRobotCode(robot, step, config);

                Map<String, Object> resultItem = new HashMap<>();
                resultItem.put("stepIndex", i + 1);
                resultItem.put("stepName", stepName);
                resultItem.put("stepType", stepType);
                resultItem.put("robotId", robotId);
                resultItem.put("robotName", robot.getName());
                resultItem.put("status", "success");
                resultItem.put("result", stepResult);
                stepResults.add(resultItem);

                fullLog.append("[成功] ");
                fullLog.append("步骤类型: ").append(getStepTypeName(stepType)).append("\n");

                if (stepResult.containsKey("output")) {
                    fullLog.append("执行输出: ").append(stepResult.get("output").toString()).append("\n");
                }
                if (stepResult.containsKey("dataCount")) {
                    fullLog.append("处理数据: ").append(stepResult.get("dataCount").toString()).append(" 条\n");
                }
                if (stepResult.containsKey("message")) {
                    fullLog.append("消息: ").append(stepResult.get("message").toString()).append("\n");
                }
                // 添加浏览器自动化日志
                if (stepResult.containsKey("logs")) {
                    Object logsObj = stepResult.get("logs");
                    if (logsObj instanceof List) {
                        for (Object logEntry : (List<?>) logsObj) {
                            fullLog.append("  > ").append(logEntry.toString()).append("\n");
                        }
                    }
                }

            } catch (Exception e) {
                allSuccess = false;
                Map<String, Object> resultItem = new HashMap<>();
                resultItem.put("stepIndex", i + 1);
                resultItem.put("stepName", stepName);
                resultItem.put("stepType", stepType);
                resultItem.put("robotId", robotId);
                resultItem.put("robotName", robot.getName());
                resultItem.put("status", "failed");
                resultItem.put("error", e.getMessage());
                stepResults.add(resultItem);

                fullLog.append("[失败] ").append(e.getMessage()).append("\n");
            }

            // 将机器人状态恢复为空闲
            robot.setStatus("idle");
            robotRepository.save(robot);

            fullLog.append("\n");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = Duration.between(startTime, endTime).getSeconds();
        String duration = String.format("%02d:%02d:%02d", durationSeconds / 3600, (durationSeconds % 3600) / 60, durationSeconds % 60);

        // 统计采集到的数据总量
        int totalDataCount = 0;
        boolean hasDataCollected = false;
        for (Map<String, Object> result : stepResults) {
            Map<String, Object> stepResultData = (Map<String, Object>) result.get("result");
            if (stepResultData != null && stepResultData.containsKey("dataCount")) {
                Object dataCountObj = stepResultData.get("dataCount");
                if (dataCountObj instanceof Number) {
                    int dataCount = ((Number) dataCountObj).intValue();
                    totalDataCount += dataCount;
                    if (dataCount > 0) {
                        hasDataCollected = true;
                    }
                }
            }
        }

        fullLog.append("=== 流程执行完成 ===\n");
        fullLog.append("结束时间: ").append(endTime).append("\n");
        fullLog.append("总耗时: ").append(duration).append("\n");
        fullLog.append("成功步骤: ").append(stepResults.stream().filter(r -> "success".equals(r.get("status"))).count()).append("/").append(steps.size()).append("\n");
        fullLog.append("采集数据: ").append(totalDataCount).append(" 条\n");

        // 根据数据采集结果判断最终状态
        // - 如果有步骤失败：failed
        // - 如果没有数据采集：abnormal（异常）
        // - 如果全部成功且有数据：completed（正常）
        // - 如果全部成功但没有数据采集：completed_with_errors 或 abnormal
        String finalStatus;
        if (!allSuccess) {
            finalStatus = "failed";
            fullLog.append("状态判定: 执行失败（有步骤出错）\n");
        } else if (totalDataCount > 0) {
            finalStatus = "completed"; // 正常 - 有数据采集
            fullLog.append("状态判定: 正常（采集到数据）\n");
        } else {
            finalStatus = "abnormal"; // 异常 - 没有数据采集
            fullLog.append("状态判定: 异常（未采集到数据）\n");
        }

        final String finalMessage = fullLog.toString();
        final LocalDateTime finalEndTime = endTime;
        final String finalDuration = duration;
        final String finalResultData = toJson(stepResults);
        final int finalDataCount = totalDataCount;

        executionLogRepository.findById(logId).ifPresent(execLog -> {
            execLog.setStatus(finalStatus);
            execLog.setMessage(finalMessage);
            execLog.setEndTime(finalEndTime);
            execLog.setDuration(finalDuration);
            execLog.setResultData(finalResultData);
            execLog.setDataCount(finalDataCount);
            executionLogRepository.save(execLog);
        });
    }

    /**
     * 真正执行机器人代码
     */
    private Map<String, Object> executeRobotCode(Robot robot, Map<String, Object> step, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        String robotCode = robot.getRobotCode();
        String stepType = (String) step.getOrDefault("type", "");
        String stepName = (String) step.getOrDefault("name", "未命名步骤");
        String robotCategory = robot.getRobotCategory();

        result.put("stepName", stepName);
        result.put("stepType", stepType);
        result.put("robotId", robot.getId());
        result.put("robotName", robot.getName());

        log.info("=== 执行机器人代码 ===");
        log.info("机器人ID: {}, 名称: {}", robot.getId(), robot.getName());
        log.info("机器人分类: {}", robotCategory);
        log.info("步骤类型: {}", stepType);
        log.info("robotCode是否为空: {}", robotCode == null ? "null" : (robotCode.isEmpty() ? "空字符串" : "有值"));
        log.info("robotCode长度: {}", robotCode != null ? robotCode.length() : 0);
        if (robotCode != null && !robotCode.isEmpty()) {
            log.info("robotCode内容: {}", robotCode.substring(0, Math.min(100, robotCode.length())));
        }

        try {
            // 如果机器人有自定义代码，执行自定义代码
            if (robotCode != null && !robotCode.trim().isEmpty()) {
                log.info(">>> 调用 RobotCodeExecutor.execute()");
                // 使用命令执行器执行代码
                Map<String, Object> executorResult = robotCodeExecutor.execute(robot, config);

                // 收集日志
                Object logs = executorResult.get("logs");
                if (logs instanceof List) {
                    for (Object logEntry : (List<?>) logs) {
                        // 日志会被收集到步骤日志中
                    }
                }

                result.putAll(executorResult);
                result.put("executedBy", "robot_code");
            } else {
                // 否则根据步骤类型执行对应的模拟逻辑
                log.info(">>> 调用 executeStepByType() (模拟逻辑)");
                result.putAll(executeStepByType(stepType, config));
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("status", "failed");
            throw new RuntimeException("执行机器人代码失败: " + e.getMessage());
        }

        result.put("status", "success");
        return result;
    }

    /**
     * 执行浏览器自动化
     */
    private Map<String, Object> executeBrowserAutomation(String robotCode, Map<String, Object> step, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        List<String> logs = new ArrayList<>();
        
        try {
            // 使用 Playwright 执行浏览器自动化
            logs.add(">>> 启动 Playwright 浏览器引擎...");
            
            // 创建 Playwright 实例
            com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create();
            
            try {
                // 启动浏览器（优先使用 Edge，如果失败则使用 Chromium）
                logs.add(">>> 正在启动浏览器...");
                com.microsoft.playwright.Browser browser;
                try {
                    browser = playwright.chromium().launch(
                        new com.microsoft.playwright.BrowserType.LaunchOptions()
                            .setChannel("msedge")
                            .setHeadless(true)
                    );
                } catch (Exception e) {
                    logs.add("Edge 启动失败，尝试使用 Chromium...");
                    browser = playwright.chromium().launch(
                        new com.microsoft.playwright.BrowserType.LaunchOptions()
                            .setHeadless(true)
                    );
                }
                logs.add(">>> 浏览器启动成功");
                
                // 创建新页面
                logs.add(">>> 正在创建页面...");
                com.microsoft.playwright.Page page = browser.newPage();
                
                // 设置默认超时
                page.setDefaultTimeout(30000);
                logs.add(">>> 页面创建成功");
                
                // 解析并执行命令
                String[] lines = robotCode.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("//") || line.startsWith("var ")) continue;
                    
                    // 打开网址
                    if (line.contains("打开") || line.contains("navigate") || line.contains("goto") || line.contains("导航")) {
                        String url = extractUrl(line);
                        if (url != null) {
                            logs.add(">>> 正在导航到: " + url);
                            page.navigate(url);
                            logs.add(">>> 页面加载完成，标题: " + page.title());
                        }
                    }
                    
                    // 搜索
                    else if (line.contains("搜索") || line.contains("Search") || line.contains("search")) {
                        String keyword = extractSearchKeyword(line);
                        if (keyword != null && !keyword.isEmpty()) {
                            logs.add(">>> 正在搜索: " + keyword);
                            // 输入关键词
                            page.fill("input[name='wd'], input[id='kw']", keyword);
                            page.waitForTimeout(500);
                            // 点击搜索按钮
                            page.click("input[type='submit'], button[type='submit'], #su");
                            page.waitForTimeout(2000);
                            logs.add(">>> 搜索完成，当前页面: " + page.title());
                        }
                    }
                    
                    // 点击元素
                    else if (line.contains("点击") || line.contains("click")) {
                        String selector = extractSelector(line);
                        if (selector != null) {
                            logs.add(">>> 正在点击: " + selector);
                            page.click(selector);
                            page.waitForTimeout(1000);
                            logs.add(">>> 点击成功");
                        }
                    }
                    
                    // 等待
                    else if (line.contains("等待") || line.contains("wait")) {
                        String timeStr = line.replaceAll("[^0-9]", "");
                        int waitTime = timeStr.isEmpty() ? 1000 : Integer.parseInt(timeStr);
                        logs.add(">>> 等待 " + waitTime + " 毫秒...");
                        page.waitForTimeout(waitTime);
                    }
                    
                    // 获取页面内容
                    else if (line.contains("获取") || line.contains("getText")) {
                        String content = page.content();
                        logs.add(">>> 页面内容获取成功，长度: " + content.length() + " 字符");
                    }
                    
                    // 截图
                    else if (line.contains("截图") || line.contains("screenshot")) {
                        byte[] screenshot = page.screenshot();
                        logs.add(">>> 截图已保存，大小: " + screenshot.length + " 字节");
                    }
                }
                
                // 关闭浏览器
                browser.close();
                logs.add(">>> 浏览器已关闭");
                
                result.put("output", "浏览器自动化执行成功");
                result.put("logs", logs);
                result.put("dataCount", logs.size());
                result.put("message", "执行了 " + logs.size() + " 个操作");
                
            } finally {
                playwright.close();
            }
            
        } catch (Exception e) {
            logs.add(">>> 执行失败: " + e.getMessage());
            result.put("error", e.getMessage());
            result.put("logs", logs);
            throw new RuntimeException("浏览器自动化失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 从代码中提取URL
     */
    private String extractUrl(String line) {
        // 匹配 http:// 或 https:// 开头的 URL
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://[^\\s'\"),;\\]\\}]+");
        java.util.regex.Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group();
        }
        
        // 匹配常见网站（不区分大小写）
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("baidu") || lowerLine.contains("百度")) return "https://www.baidu.com";
        if (lowerLine.contains("google")) return "https://www.google.com";
        if (lowerLine.contains("taobao") || lowerLine.contains("淘宝")) return "https://www.taobao.com";
        if (lowerLine.contains("jd") || lowerLine.contains("京东")) return "https://www.jd.com";
        if (lowerLine.contains("github")) return "https://github.com";
        
        return null;
    }
    
    /**
     * 从代码中提取搜索关键词
     */
    private String extractSearchKeyword(String line) {
        // 移除搜索相关字符后获取剩余文本
        String keyword = line
            .replaceAll("(?i)(搜索|Search|search)['\"\\(：:\\s]*", "")
            .replaceAll("['\"\\)]", "")
            .replaceAll("//.*", "")
            .trim();
        return keyword.isEmpty() ? null : keyword;
    }
    
    /**
     * 从代码中提取选择器
     */
    private String extractSelector(String line) {
        String selector = line
            .replaceAll("(?i)(点击|click)['\"\\(：:\\s]*", "")
            .replaceAll("['\"\\)]", "")
            .trim();
        return selector.isEmpty() ? null : selector;
    }

    /**
     * 根据步骤类型执行对应的逻辑
     * 注意：此方法仅在机器人代码为空时作为后备逻辑使用
     * 真实数据采集应该使用 RobotCodeExecutor
     */
    private Map<String, Object> executeStepByType(String stepType, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();

        log.warn(">>> 警告: 机器人代码为空，使用模拟逻辑执行!");
        log.warn(">>> 这意味着采集/解析/加工/落库的数据不是真实采集的!");
        log.warn(">>> 请在机器人详情页编辑并保存代码，然后重新执行流程。");

        switch (stepType != null ? stepType : "") {
            case "collect":
                result.put("output", "【模拟】数据采集完成 - 警告: 代码为空!");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，请先保存代码!");
                break;
            case "parse":
                result.put("output", "【模拟】数据解析完成");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，数据无效!");
                break;
            case "process":
                result.put("output", "【模拟】数据加工完成");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，数据无效!");
                break;
            case "query":
                result.put("output", "【模拟】数据查询完成");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，数据无效!");
                break;
            case "transform":
                result.put("output", "【模拟】数据转换完成");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，数据无效!");
                break;
            case "output":
                result.put("output", "【模拟】数据输出完成");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，数据无效!");
                break;
            case "validate":
                result.put("output", "【模拟】数据校验完成");
                result.put("dataCount", 0);
                result.put("validCount", 0);
                result.put("invalidCount", 0);
                result.put("message", "【警告】机器人代码为空，校验无效!");
                break;
            default:
                result.put("output", "【模拟】步骤执行完成");
                result.put("dataCount", 0);
                result.put("message", "【警告】机器人代码为空，步骤无效!");
                break;
        }

        return result;
    }

    /**
     * 执行自定义代码
     */
    private String executeCustomCode(String code, Map<String, Object> config) throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        
        // 设置配置参数
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
        }
        
        // 设置一些常用的Java对象引用
        engine.put("Runtime", java.lang.Runtime.getRuntime());

        // 执行代码
        Object result = engine.eval(code);
        return result != null ? result.toString() : "执行完成";
    }

    /**
     * 获取步骤类型名称
     */
    private String getStepTypeName(String type) {
        if (type == null || type.isEmpty()) return "通用步骤";
        switch (type) {
            case "collect":
                return "数据采集";
            case "parse":
                return "数据解析";
            case "process":
                return "数据加工";
            case "query":
                return "数据查询";
            case "transform":
                return "数据转换";
            case "output":
                return "数据输出";
            case "validate":
                return "数据校验";
            default:
                return "通用步骤";
        }
    }

    /**
     * 转换为JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 查询执行状态
     */
    public Map<String, Object> getExecutionStatus(Long processId) {
        Map<String, Object> result = new HashMap<>();
        Future<?> task = executingTasks.get(processId);

        if (task == null) {
            result.put("status", "not_running");
            result.put("message", "该流程当前没有执行任务");
        } else if (task.isDone()) {
            result.put("status", "completed");
            result.put("message", "执行已完成");
        } else if (task.isCancelled()) {
            result.put("status", "cancelled");
            result.put("message", "执行已取消");
        } else {
            result.put("status", "running");
            result.put("message", "执行中");
        }

        return result;
    }

    /**
     * 取消执行
     */
    public boolean cancelExecution(Long processId) {
        Future<?> task = executingTasks.get(processId);
        if (task != null && !task.isDone()) {
            return task.cancel(true);
        }
        return false;
    }

    /**
     * 将画布格式转换为步骤数组格式
     * 画布格式：{nodes:[{id, type, name, stepType, robotId, ...}], edges:[{source, target}]}
     * 步骤格式：[{name, type, robotId, config, ...}, ...]
     */
    private List<Map<String, Object>> convertCanvasToSteps(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        List<Map<String, Object>> steps = new ArrayList<>();
        
        com.fasterxml.jackson.databind.JsonNode nodes = jsonNode.get("nodes");
        com.fasterxml.jackson.databind.JsonNode edges = jsonNode.get("edges");
        
        if (nodes == null || !nodes.isArray()) {
            return steps;
        }
        
        // 只保留流程节点（type='process'），过滤掉条件节点等其他类型
        List<com.fasterxml.jackson.databind.JsonNode> processNodes = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
            String nodeType = node.path("type").asText();
            if ("process".equals(nodeType)) {
                processNodes.add(node);
            }
        }
        
        // 如果edges存在，按连接顺序排序（拓扑排序）
        if (edges != null && edges.isArray() && !edges.isEmpty()) {
            // 构建邻接表
            Map<String, List<String>> adjacency = new HashMap<>();
            for (com.fasterxml.jackson.databind.JsonNode edge : edges) {
                String source = edge.path("source").asText();
                String target = edge.path("target").asText();
                adjacency.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
            }
            
            // 找到所有被指向的节点ID
            Set<String> targetIds = new HashSet<>();
            for (com.fasterxml.jackson.databind.JsonNode edge : edges) {
                targetIds.add(edge.path("target").asText());
            }
            
            // 找到起始节点（没有被其他节点指向的节点）
            List<String> startIds = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode node : processNodes) {
                if (!targetIds.contains(node.path("id").asText())) {
                    startIds.add(node.path("id").asText());
                }
            }
            
            // BFS遍历
            Set<String> visited = new HashSet<>();
            Queue<String> queue = new LinkedList<>();
            
            if (!startIds.isEmpty()) {
                for (String id : startIds) {
                    queue.offer(id);
                }
            } else if (!processNodes.isEmpty()) {
                // 如果没有明显的起始节点，从第一个节点开始
                queue.offer(processNodes.get(0).path("id").asText());
            }
            
            while (!queue.isEmpty()) {
                String nodeId = queue.poll();
                if (visited.contains(nodeId)) continue;
                visited.add(nodeId);
                
                // 找到对应的节点
                for (com.fasterxml.jackson.databind.JsonNode node : processNodes) {
                    if (nodeId.equals(node.path("id").asText())) {
                        steps.add(convertNodeToStep(node));
                        break;
                    }
                }
                
                // 将后继节点加入队列
                List<String> nextIds = adjacency.getOrDefault(nodeId, new ArrayList<>());
                for (String nextId : nextIds) {
                    if (!visited.contains(nextId)) {
                        queue.offer(nextId);
                    }
                }
            }
            
            // 处理未访问到的节点（可能没有边的节点）
            for (com.fasterxml.jackson.databind.JsonNode node : processNodes) {
                String nodeId = node.path("id").asText();
                if (!visited.contains(nodeId)) {
                    steps.add(convertNodeToStep(node));
                }
            }
        } else {
            // 没有边的情况，按节点在数组中的顺序排列
            for (com.fasterxml.jackson.databind.JsonNode node : processNodes) {
                steps.add(convertNodeToStep(node));
            }
        }
        
        return steps;
    }
    
    /**
     * 将画布节点转换为步骤格式
     */
    private Map<String, Object> convertNodeToStep(com.fasterxml.jackson.databind.JsonNode node) {
        Map<String, Object> step = new HashMap<>();
        step.put("name", node.path("name").asText("未命名步骤"));
        step.put("type", node.path("stepType").asText(""));
        step.put("category", node.path("category").asText(""));
        
        // 处理robotId
        com.fasterxml.jackson.databind.JsonNode robotIdNode = node.get("robotId");
        if (robotIdNode != null && !robotIdNode.isNull()) {
            if (robotIdNode.isInt()) {
                step.put("robotId", robotIdNode.asInt());
            } else if (robotIdNode.isLong()) {
                step.put("robotId", robotIdNode.asLong());
            } else {
                try {
                    step.put("robotId", Long.parseLong(robotIdNode.asText()));
                } catch (NumberFormatException e) {
                    // 忽略无效的robotId
                }
            }
        }
        
        step.put("robotName", node.path("robotName").asText(""));
        step.put("config", new HashMap<String, Object>());
        
        return step;
    }
}
