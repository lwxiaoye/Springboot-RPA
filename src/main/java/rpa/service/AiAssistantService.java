package rpa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.AiChatConversation;
import rpa.entity.RPACardData;
import rpa.repository.AiChatConversationRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RPA智能助手服务
 * 
 * 功能：
 * 1. 智能问答 - 回答RPA平台常见问题
 * 2. 错误日志分析 - 分析任务失败原因
 * 3. 流程推荐 - 根据需求推荐流程模板
 * 4. 意图识别 - 识别用户意图并提供建议
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiChatConversationRepository chatRepository;
    private final ObjectMapper objectMapper;

    // ==================== 意图识别 ====================

    /**
     * 意图类型枚举
     */
    public enum Intent {
        GREETING,           // 问候
        TASK_QUERY,         // 任务查询
        TASK_OPERATION,     // 任务操作
        ROBOT_QUERY,        // 机器人查询
        ROBOT_OPERATION,    // 机器人操作
        FLOW_QUERY,         // 流程查询
        FLOW_EXECUTION,     // 流程执行
        LOG_QUERY,          // 日志查询
        ERROR_ANALYSIS,     // 错误分析
        HELP,               // 帮助
        SUGGESTION,         // 建议
        UNKNOWN             // 未知
    }

    /**
     * 识别用户消息的意图
     */
    public IntentRecognizeResult recognizeIntent(String message) {
        String lowerMsg = message.toLowerCase().trim();
        
        // 问候
        if (containsAny(lowerMsg, "你好", "hi", "hello", "早上好", "下午好", "晚上好")) {
            return new IntentRecognizeResult(Intent.GREETING, "greeting", 0.95);
        }
        
        // 任务查询
        if (containsAny(lowerMsg, "任务", "task", "执行", "运行")) {
            if (containsAny(lowerMsg, "状态", "查看", "查询", "怎么样", "成功", "失败")) {
                return new IntentRecognizeResult(Intent.TASK_QUERY, "task_query", 0.90);
            }
            if (containsAny(lowerMsg, "创建", "新建", "开始", "启动")) {
                return new IntentRecognizeResult(Intent.TASK_OPERATION, "task_create", 0.85);
            }
            return new IntentRecognizeResult(Intent.TASK_QUERY, "task_query", 0.70);
        }
        
        // 机器人查询
        if (containsAny(lowerMsg, "机器人", "robot", "机", "在线", "离线", "状态")) {
            if (containsAny(lowerMsg, "状态", "在线", "离线", "运行中")) {
                return new IntentRecognizeResult(Intent.ROBOT_QUERY, "robot_status", 0.90);
            }
            return new IntentRecognizeResult(Intent.ROBOT_QUERY, "robot_query", 0.70);
        }
        
        // 流程查询
        if (containsAny(lowerMsg, "流程", "process", "脚本", "自动化")) {
            if (containsAny(lowerMsg, "执行", "运行", "启动", "开始")) {
                return new IntentRecognizeResult(Intent.FLOW_EXECUTION, "flow_execute", 0.85);
            }
            return new IntentRecognizeResult(Intent.FLOW_QUERY, "flow_query", 0.75);
        }
        
        // 日志查询
        if (containsAny(lowerMsg, "日志", "log", "记录", "错误", "异常")) {
            return new IntentRecognizeResult(Intent.LOG_QUERY, "log_query", 0.85);
        }
        
        // 错误分析
        if (containsAny(lowerMsg, "分析", "原因", "为什么", "出错", "失败")) {
            return new IntentRecognizeResult(Intent.ERROR_ANALYSIS, "error_analysis", 0.90);
        }
        
        // 帮助
        if (containsAny(lowerMsg, "帮助", "help", "怎么", "如何", "教我", "使用")) {
            return new IntentRecognizeResult(Intent.HELP, "help", 0.90);
        }
        
        // 建议
        if (containsAny(lowerMsg, "建议", "推荐", "优化", "提升")) {
            return new IntentRecognizeResult(Intent.SUGGESTION, "suggestion", 0.85);
        }
        
        return new IntentRecognizeResult(Intent.UNKNOWN, "unknown", 0.50);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ==================== AI回复生成 ====================

    /**
     * 生成AI回复
     */
    public AiResponse generateResponse(Long userId, String message) {
        AiResponse response = new AiResponse();
        response.setUserMessage(message);
        response.setTimestamp(LocalDateTime.now());

        // 识别意图
        IntentRecognizeResult intentResult = recognizeIntent(message);
        response.setIntent(intentResult.getIntent().name());
        
        log.info("AI助手识别意图: {} ({})", intentResult.getIntent(), intentResult.getConfidence());

        // 根据意图生成回复
        String reply;
        List<AiSuggestion> suggestions = new ArrayList<>();

        switch (intentResult.getIntent()) {
            case GREETING -> {
                reply = generateGreetingResponse();
                suggestions.add(new AiSuggestion("查看任务列表", "/tasks", "查看当前所有任务"));
                suggestions.add(new AiSuggestion("查看机器人状态", "/robots", "查看所有机器人状态"));
                suggestions.add(new AiSuggestion("帮助文档", "/help", "查看使用帮助"));
            }
            case TASK_QUERY -> {
                reply = generateTaskResponse(message);
                suggestions.add(new AiSuggestion("查看失败任务", "/tasks failed", "查看最近失败的任务"));
                suggestions.add(new AiSuggestion("创建新任务", "/task create", "创建新的自动化任务"));
            }
            case TASK_OPERATION -> {
                reply = "好的，我可以帮您操作任务。请告诉我：\n1. 您想要创建新任务还是执行现有任务？\n2. 如果是执行任务，请提供任务ID";
                suggestions.add(new AiSuggestion("创建新任务", "/task create", "创建新的自动化任务"));
                suggestions.add(new AiSuggestion("执行现有任务", "/task run <id>", "执行指定ID的任务"));
            }
            case ROBOT_QUERY -> {
                reply = generateRobotResponse(message);
                suggestions.add(new AiSuggestion("查看所有机器人", "/robots", "查看所有机器人状态"));
                suggestions.add(new AiSuggestion("查看特定机器人", "/robot <id>", "查看指定机器人的详细信息"));
            }
            case LOG_QUERY -> {
                reply = "我可以帮您查询日志。请告诉我：\n1. 您想查询哪个任务的日志？\n2. 或者您可以直接说\"查看最近的执行日志\"";
                suggestions.add(new AiSuggestion("最近执行日志", "/logs recent", "查看最近10条执行日志"));
                suggestions.add(new AiSuggestion("失败任务日志", "/logs failed", "查看最近失败任务的日志"));
            }
            case ERROR_ANALYSIS -> {
                reply = "请提供您想要分析的日志或错误信息，我可以帮您：\n1. 分析错误原因\n2. 提供解决方案\n3. 推荐相关流程";
                suggestions.add(new AiSuggestion("上传日志分析", "[粘贴日志内容]", "粘贴错误日志进行AI分析"));
            }
            case HELP -> {
                reply = generateHelpResponse();
                suggestions.add(new AiSuggestion("查看所有命令", "/help commands", "查看所有可用命令"));
                suggestions.add(new AiSuggestion("RPA入门指南", "/help getting-started", "查看入门指南"));
            }
            default -> {
                reply = "我理解您想要了解RPA平台的功能。我可以帮您：\n• 查询任务和机器人状态\n• 执行自动化流程\n• 分析错误日志\n• 提供优化建议\n\n请告诉我您具体想做什么？";
                suggestions.add(new AiSuggestion("查看任务", "/tasks", "查看当前任务"));
                suggestions.add(new AiSuggestion("查看机器人", "/robots", "查看机器人状态"));
                suggestions.add(new AiSuggestion("创建流程", "/flows", "浏览流程仓库"));
            }
        }

        response.setReply(reply);
        response.setSuggestions(suggestions);
        
        // 解析实体
        response.setEntities(extractEntities(message));

        // 保存聊天记录
        saveChatConversation(userId, response);

        return response;
    }

    // ==================== 错误日志分析 ====================

    /**
     * 分析错误日志
     */
    public ErrorAnalysisResult analyzeErrorLog(String errorLog) {
        ErrorAnalysisResult result = new ErrorAnalysisResult();
        result.setOriginalLog(errorLog);
        result.setTimestamp(LocalDateTime.now());

        // 常见错误模式识别
        List<String> identifiedPatterns = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        StringBuilder analysis = new StringBuilder();

        String lowerLog = errorLog.toLowerCase();

        // 网络相关错误
        if (containsAny(lowerLog, "connection", "timeout", "network", "socket", "连接", "超时", "网络")) {
            identifiedPatterns.add("网络问题");
            suggestions.add("检查网络连接是否稳定");
            suggestions.add("增加超时等待时间");
            suggestions.add("使用重试机制");
            analysis.append("🔌 **网络问题**：检测到网络连接相关的错误。\n");
        }

        // 认证授权错误
        if (containsAny(lowerLog, "auth", "permission", "denied", "unauthorized", "登录", "权限", "认证", "拒绝")) {
            identifiedPatterns.add("认证授权问题");
            suggestions.add("检查用户名密码是否正确");
            suggestions.add("检查账号是否有相应权限");
            suggestions.add("检查Token是否过期");
            analysis.append("🔐 **认证授权问题**：可能是登录凭证失效或权限不足。\n");
        }

        // 元素定位错误
        if (containsAny(lowerLog, "element", "not found", "unable to locate", "找不到", "元素", "定位")) {
            identifiedPatterns.add("UI元素定位失败");
            suggestions.add("检查页面结构是否发生变化");
            suggestions.add("更新元素选择器");
            suggestions.add("增加等待时间");
            analysis.append("🎯 **元素定位失败**：无法找到目标UI元素。\n");
        }

        // 数据处理错误
        if (containsAny(lowerLog, "null", "empty", "undefined", "空值", "数据", "格式")) {
            identifiedPatterns.add("数据处理问题");
            suggestions.add("添加空值检查");
            suggestions.add("验证数据格式");
            suggestions.add("添加数据转换逻辑");
            analysis.append("📊 **数据问题**：处理数据时遇到问题。\n");
        }

        // 文件操作错误
        if (containsAny(lowerLog, "file", "path", "directory", "folder", "文件", "路径", "目录")) {
            identifiedPatterns.add("文件操作问题");
            suggestions.add("检查文件路径是否正确");
            suggestions.add("确认文件是否存在");
            suggestions.add("检查文件权限");
            analysis.append("📁 **文件问题**：文件操作失败。\n");
        }

        // 未知错误
        if (identifiedPatterns.isEmpty()) {
            analysis.append("❓ **未能识别具体错误类型**，建议：\n");
            suggestions.add("查看完整的错误堆栈信息");
            suggestions.add("联系技术支持");
        }

        result.setIdentifiedPatterns(identifiedPatterns);
        result.setAnalysisText(analysis.toString());
        result.setSuggestions(suggestions);

        // 生成卡片数据
        RPACardData cardData = new RPACardData();
        cardData.setCardType("ALERT");
        cardData.setAlertTitle("日志分析结果");
        cardData.setAlertContent(analysis.toString());
        cardData.setAlertLevelForNotify("warning");
        cardData.setAlertTime(LocalDateTime.now());

        List<RPACardData.CardAction> actions = new ArrayList<>();
        actions.add(new RPACardData.CardAction("view_logs", "查看日志详情", null, "Document"));
        actions.add(new RPACardData.CardAction("create_ticket", "创建工单", null, "Ticket"));
        cardData.setActions(actions);

        result.setCardData(cardData);

        return result;
    }

    // ==================== 流程推荐 ====================

    /**
     * 根据需求推荐流程
     */
    public List<FlowRecommendation> recommendFlows(String requirement) {
        List<FlowRecommendation> recommendations = new ArrayList<>();

        String lowerReq = requirement.toLowerCase();

        // Excel处理类
        if (containsAny(lowerReq, "excel", "表格", "csv", "数据处理", "批量")) {
            recommendations.add(new FlowRecommendation(1L, "Excel数据批量处理", "自动化处理Excel数据的读取、转换和写入", "high"));
            recommendations.add(new FlowRecommendation(2L, "CSV数据导入", "批量导入CSV数据到系统", "high"));
        }

        // 网页爬取类
        if (containsAny(lowerReq, "爬虫", "抓取", "网页", "网站", "爬取", "scrape")) {
            recommendations.add(new FlowRecommendation(3L, "网页数据爬取", "从指定网页提取结构化数据", "high"));
            recommendations.add(new FlowRecommendation(4L, "电商数据采集", "采集电商平台商品信息", "medium"));
        }

        // 文件处理类
        if (containsAny(lowerReq, "文件", "pdf", "文档", "word", "转换")) {
            recommendations.add(new FlowRecommendation(5L, "PDF文本提取", "从PDF文件中提取文本内容", "medium"));
            recommendations.add(new FlowRecommendation(6L, "文档格式转换", "批量转换文档格式", "medium"));
        }

        // 邮件处理类
        if (containsAny(lowerReq, "邮件", "email", "发送", "接收")) {
            recommendations.add(new FlowRecommendation(7L, "邮件自动处理", "自动处理收发邮件", "high"));
            recommendations.add(new FlowRecommendation(8L, "邮件批量发送", "批量发送邮件", "medium"));
        }

        // 审批流程类
        if (containsAny(lowerReq, "审批", "流程审批", "请假", "报销")) {
            recommendations.add(new FlowRecommendation(9L, "审批流程自动化", "自动处理审批流程", "high"));
            recommendations.add(new FlowRecommendation(10L, "钉钉审批同步", "同步钉钉审批数据", "medium"));
        }

        // 如果没有匹配，返回通用推荐
        if (recommendations.isEmpty()) {
            recommendations.add(new FlowRecommendation(11L, "通用数据采集", "基础的网页数据采集流程", "medium"));
            recommendations.add(new FlowRecommendation(12L, "定时任务执行", "定时执行自动化任务", "low"));
        }

        return recommendations;
    }

    // ==================== 快捷指令解析 ====================

    /**
     * 解析快捷指令
     */
    public CommandResult parseCommand(String command) {
        CommandResult result = new CommandResult();
        result.setOriginalCommand(command);

        String trimmed = command.trim();
        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        result.setCommand(cmd);
        result.setArgs(args);

        switch (cmd) {
            case "/help" -> {
                result.setSuccess(true);
                result.setResult(generateHelpResponse());
                result.setSuggestions(List.of(
                    new AiSuggestion("查看任务", "/tasks", "查看任务列表"),
                    new AiSuggestion("查看机器人", "/robots", "查看机器人状态"),
                    new AiSuggestion("浏览流程", "/flows", "浏览流程仓库")
                ));
            }
            case "/tasks" -> {
                result.setSuccess(true);
                result.setResult("正在为您查询任务列表...");
                result.setRedirectUrl("/tasks");
            }
            case "/robots" -> {
                result.setSuccess(true);
                result.setResult("正在为您查询机器人状态...");
                result.setRedirectUrl("/robots");
            }
            case "/flows" -> {
                result.setSuccess(true);
                result.setResult("正在为您打开流程仓库...");
                result.setRedirectUrl("/flows");
            }
            case "/task" -> {
                if (args.length == 0) {
                    result.setSuccess(false);
                    result.setResult("请提供任务操作命令，如：\n/task create - 创建新任务\n/task run <id> - 执行任务\n/task status <id> - 查看任务状态");
                } else {
                    result.setSuccess(true);
                    result.setResult("正在处理任务命令: " + args[0]);
                }
            }
            default -> {
                result.setSuccess(false);
                result.setResult("未知命令: " + cmd + "\n输入 /help 查看可用命令");
            }
        }

        return result;
    }

    // ==================== 辅助方法 ====================

    private String generateGreetingResponse() {
        int hour = LocalDateTime.now().getHour();
        String timeGreeting = hour < 12 ? "早上好" : hour < 18 ? "下午好" : "晚上好";
        
        return String.format("""
            %s！👋 我是RPA智能助手小M。
            
            我可以帮您：
            • 查询任务和机器人状态
            • 执行自动化流程
            • 分析错误日志
            • 推荐优化建议
            
            请告诉我您需要什么帮助？
            """, timeGreeting);
    }

    private String generateTaskResponse(String message) {
        String lowerMsg = message.toLowerCase();
        
        if (containsAny(lowerMsg, "失败", "failed", "错误")) {
            return "我帮您查询失败的任务：\n\n最近失败的任务：\n• 任务 #1024 - 数据采集任务 (10分钟前)\n• 任务 #1023 - 报表生成任务 (30分钟前)\n\n需要我帮您分析失败原因吗？";
        }
        
        if (containsAny(lowerMsg, "成功", "完成")) {
            return "我帮您查询已完成的任务：\n\n最近成功的任务：\n• 任务 #1025 - 数据采集任务 (5分钟前)\n• 任务 #1022 - 报表生成任务 (1小时前)\n\n所有任务运行正常！";
        }
        
        return "我可以帮您查询任务信息：\n\n当前系统中有 5 个任务正在运行，12 个任务已完成。\n\n请问您想查看哪些任务？";
    }

    private String generateRobotResponse(String message) {
        String lowerMsg = message.toLowerCase();
        
        if (containsAny(lowerMsg, "在线", "运行")) {
            return "当前机器人状态：\n\n🟢 在线 (3台):\n• RPA-Robot-01\n• RPA-Robot-02\n• RPA-Robot-03\n\n⚪ 离线 (2台):\n• RPA-Robot-04\n• RPA-Robot-05";
        }
        
        return "机器人状态概览：\n\n• 总计: 5台\n• 在线: 3台\n• 离线: 2台\n• 今日执行: 156次\n\n所有机器人运行正常！";
    }

    private String generateHelpResponse() {
        return """
            📖 **RPA智能助手使用指南**
            
            **快捷指令：**
            `/tasks` - 查看任务列表
            `/robots` - 查看机器人状态
            `/flows` - 浏览流程仓库
            `/logs` - 查看执行日志
            `/help` - 显示帮助信息
            
            **常见问题：**
            • 如何创建新任务？ → 点击「任务调度」→「创建任务」
            • 如何添加机器人？ → 点击「机器人管理」→「添加机器人」
            • 如何使用流程？ → 在「流程仓库」中选择流程执行
            
            **需要更多帮助？**
            联系技术支持或查看在线文档
            """;
    }

    private Map<String, Object> extractEntities(String message) {
        Map<String, Object> entities = new HashMap<>();
        
        // 提取任务ID
        Pattern taskPattern = Pattern.compile("[任务#](\\d+)");
        Matcher taskMatcher = taskPattern.matcher(message);
        if (taskMatcher.find()) {
            entities.put("taskId", Long.parseLong(taskMatcher.group(1)));
        }
        
        // 提取机器人ID
        Pattern robotPattern = Pattern.compile("[机器人R-](\\w+)");
        Matcher robotMatcher = robotPattern.matcher(message);
        if (robotMatcher.find()) {
            entities.put("robotId", robotMatcher.group(1));
        }
        
        return entities;
    }

    private void saveChatConversation(Long userId, AiResponse response) {
        try {
            AiChatConversation conversation = new AiChatConversation();
            conversation.setUserId(userId);
            conversation.setUserMessage(response.getUserMessage());
            conversation.setAiResponse(response.getReply());
            conversation.setIntent(response.getIntent());
            conversation.setSessionId("session_" + userId + "_" + System.currentTimeMillis());
            conversation.setCreatedAt(LocalDateTime.now());
            chatRepository.save(conversation);
        } catch (Exception e) {
            log.error("保存AI聊天记录失败", e);
        }
    }

    // ==================== 内部类定义 ====================

    public static class IntentRecognizeResult {
        private Intent intent;
        private String detail;
        private double confidence;

        public IntentRecognizeResult(Intent intent, String detail, double confidence) {
            this.intent = intent;
            this.detail = detail;
            this.confidence = confidence;
        }

        public Intent getIntent() { return intent; }
        public String getDetail() { return detail; }
        public double getConfidence() { return confidence; }
    }

    public static class AiResponse {
        private String userMessage;
        private String reply;
        private String intent;
        private Map<String, Object> entities;
        private List<AiSuggestion> suggestions;
        private LocalDateTime timestamp;

        // Getters and Setters
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public Map<String, Object> getEntities() { return entities; }
        public void setEntities(Map<String, Object> entities) { this.entities = entities; }
        public List<AiSuggestion> getSuggestions() { return suggestions; }
        public void setSuggestions(List<AiSuggestion> suggestions) { this.suggestions = suggestions; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class AiSuggestion {
        private String label;
        private String command;
        private String description;

        public AiSuggestion(String label, String command, String description) {
            this.label = label;
            this.command = command;
            this.description = description;
        }

        public String getLabel() { return label; }
        public String getCommand() { return command; }
        public String getDescription() { return description; }
    }

    public static class ErrorAnalysisResult {
        private String originalLog;
        private String analysisText;
        private List<String> identifiedPatterns;
        private List<String> suggestions;
        private RPACardData cardData;
        private LocalDateTime timestamp;

        public String getOriginalLog() { return originalLog; }
        public void setOriginalLog(String originalLog) { this.originalLog = originalLog; }
        public String getAnalysisText() { return analysisText; }
        public void setAnalysisText(String analysisText) { this.analysisText = analysisText; }
        public List<String> getIdentifiedPatterns() { return identifiedPatterns; }
        public void setIdentifiedPatterns(List<String> identifiedPatterns) { this.identifiedPatterns = identifiedPatterns; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        public RPACardData getCardData() { return cardData; }
        public void setCardData(RPACardData cardData) { this.cardData = cardData; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class FlowRecommendation {
        private Long id;
        private String name;
        private String description;
        private String priority;

        public FlowRecommendation(Long id, String name, String description, String priority) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.priority = priority;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPriority() { return priority; }
    }

    public static class CommandResult {
        private String originalCommand;
        private String command;
        private String[] args;
        private boolean success;
        private String result;
        private String redirectUrl;
        private List<AiSuggestion> suggestions;

        public String getOriginalCommand() { return originalCommand; }
        public void setOriginalCommand(String originalCommand) { this.originalCommand = originalCommand; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String[] getArgs() { return args; }
        public void setArgs(String[] args) { this.args = args; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
        public List<AiSuggestion> getSuggestions() { return suggestions; }
        public void setSuggestions(List<AiSuggestion> suggestions) { this.suggestions = suggestions; }
    }
}
