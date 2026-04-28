package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.ChatMessage;
import rpa.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息摘要服务
 * 
 * 功能：
 * 1. 群聊消息摘要生成
 * 2. 重点消息提取
 * 3. 关键信息汇总
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSummaryService {

    private final ChatMessageRepository messageRepository;

    /**
     * 生成会话消息摘要
     */
    public ConversationSummary generateSummary(Long conversationId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ChatMessage> messages = messageRepository.findByConversationIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                conversationId, startTime, endTime);

        if (messages.isEmpty()) {
            return new ConversationSummary("该时间段内无消息", 0, new ArrayList<>(), new ArrayList<>());
        }

        ConversationSummary summary = new ConversationSummary();
        summary.setConversationId(conversationId);
        summary.setStartTime(startTime);
        summary.setEndTime(endTime);
        summary.setMessageCount(messages.size());

        // 提取关键信息
        Set<String> participants = messages.stream()
                .map(ChatMessage::getSenderName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        summary.setParticipants(new ArrayList<>(participants));

        // 提取RPA卡片
        List<RPACardSummary> cards = extractRPACards(messages);
        summary.setRpaCards(cards);

        // 提取提及
        List<String> mentions = extractMentions(messages);
        summary.setMentions(mentions);

        // 生成摘要文本
        summary.setSummaryText(generateSummaryText(messages, participants, cards));

        return summary;
    }

    /**
     * 提取RPA卡片信息
     */
    private List<RPACardSummary> extractRPACards(List<ChatMessage> messages) {
        return messages.stream()
                .filter(m -> "rpa_card".equals(m.getType()) && m.getCardType() != null)
                .map(m -> {
                    RPACardSummary card = new RPACardSummary();
                    card.setCardType(m.getCardType());
                    card.setSenderName(m.getSenderName());
                    card.setCreatedAt(m.getCreatedAt());
                    card.setMessageId(m.getId());

                    // 解析卡片数据
                    if (m.getCardData() != null) {
                        try {
                            // 简单解析关键信息
                            if (m.getCardData().contains("taskName")) {
                                card.setTitle("任务卡片");
                            } else if (m.getCardData().contains("robotName")) {
                                card.setTitle("机器人卡片");
                            } else if (m.getCardData().contains("alertTitle")) {
                                card.setTitle("告警卡片");
                            }
                        } catch (Exception e) {
                            log.warn("解析卡片数据失败", e);
                        }
                    }

                    return card;
                })
                .collect(Collectors.toList());
    }

    /**
     * 提取@提及
     */
    private List<String> extractMentions(List<ChatMessage> messages) {
        return messages.stream()
                .filter(m -> m.getMentionedUsers() != null && !m.getMentionedUsers().isEmpty())
                .map(m -> m.getSenderName() + " @了 " + m.getMentionedUsers())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 生成摘要文本
     */
    private String generateSummaryText(List<ChatMessage> messages, Set<String> participants, 
                                       List<RPACardSummary> cards) {
        StringBuilder sb = new StringBuilder();

        sb.append("📊 **群聊摘要**\n\n");
        sb.append(String.format("• 消息总数: %d 条\n", messages.size()));
        sb.append(String.format("• 参与人数: %d 人\n", participants.size()));
        sb.append(String.format("• 参与人员: %s\n", String.join(", ", participants)));

        if (!cards.isEmpty()) {
            sb.append(String.format("\n📋 **RPA卡片** (%d个):\n", cards.size()));
            for (RPACardSummary card : cards) {
                sb.append(String.format("  • [%s] %s - %s\n", 
                        card.getCardType(), 
                        card.getSenderName(),
                        card.getTitle()));
            }
        }

        // 提取关键消息
        List<String> keyMessages = messages.stream()
                .filter(m -> m.getContent() != null && m.getContent().length() > 10)
                .sorted((m1, m2) -> {
                    int score1 = calculateImportance(m1);
                    int score2 = calculateImportance(m2);
                    return Integer.compare(score2, score1);
                })
                .limit(3)
                .map(m -> String.format("「%s」: %s", m.getSenderName(), 
                        m.getContent().length() > 50 ? m.getContent().substring(0, 50) + "..." : m.getContent()))
                .collect(Collectors.toList());

        if (!keyMessages.isEmpty()) {
            sb.append("\n💬 **重要消息**:\n");
            keyMessages.forEach(msg -> sb.append("  • ").append(msg).append("\n"));
        }

        return sb.toString();
    }

    /**
     * 计算消息重要性
     */
    private int calculateImportance(ChatMessage message) {
        int score = 0;
        String content = message.getContent() != null ? message.getContent().toLowerCase() : "";

        // RPA相关消息权重更高
        if (content.contains("任务") || content.contains("卡片")) score += 10;
        if (content.contains("告警") || content.contains("失败")) score += 15;
        if (content.contains("完成") || content.contains("成功")) score += 5;
        if (message.getMentionedUsers() != null) score += 10;

        return score;
    }

    // ==================== 快捷指令解析服务 ====================

    /**
     * 快捷指令类型
     */
    public enum CommandType {
        TASK, ROBOT, FLOW, LOG, HELP, NAVIGATE, UNKNOWN
    }

    /**
     * 解析并执行快捷指令
     */
    public CommandExecutionResult executeCommand(String command, Long userId) {
        CommandExecutionResult result = new CommandExecutionResult();
        result.setCommand(command);
        result.setUserId(userId);
        result.setTimestamp(LocalDateTime.now());

        String trimmed = command.trim();
        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        result.setCommand(cmd);
        result.setArgs(args);

        try {
            switch (cmd) {
                case "/run":
                    result = executeTaskCommand(cmd, args, userId, result);
                    break;
                case "/tasks":
                    result = new CommandExecutionResult(cmd, args, true, "正在查询任务列表...", "/tasks", "TASK");
                    break;
                case "/robots":
                    result = new CommandExecutionResult(cmd, args, true, "正在查询机器人状态...", "/robots", "ROBOT");
                    break;
                case "/flows":
                    result = new CommandExecutionResult(cmd, args, true, "正在打开流程仓库...", "/flows", "FLOW");
                    break;
                case "/logs":
                    result = new CommandExecutionResult(cmd, args, true, "正在查询执行日志...", "/logs", "LOG");
                    break;
                case "/help":
                    result = new CommandExecutionResult(cmd, args, true, generateHelpText(), null, "HELP");
                    break;
                case "/status":
                    result = executeStatusCommand(args, result);
                    break;
                default:
                    result.setSuccess(false);
                    result.setResult("未知命令: " + cmd);
                    result.setCommandType("UNKNOWN");
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}", command, e);
            result.setSuccess(false);
            result.setResult("执行失败: " + e.getMessage());
        }

        return result;
    }

    private CommandExecutionResult executeTaskCommand(String cmd, String[] args, Long userId, CommandExecutionResult result) {
        if (args.length == 0) {
            result.setSuccess(true);
            result.setResult("请提供任务名称或ID");
            result.setCommandType("TASK");
            return result;
        }

        String taskRef = args[0];
        String params = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;

        result.setSuccess(true);
        result.setResult(String.format("正在执行任务: %s", taskRef));
        result.setRedirectUrl("/task/execute/" + taskRef);
        result.setCommandType("TASK");

        // TODO: 调用任务执行服务
        log.info("用户 {} 请求执行任务: {}", userId, taskRef);

        return result;
    }

    private CommandExecutionResult executeStatusCommand(String[] args, CommandExecutionResult result) {
        if (args.length == 0) {
            result.setSuccess(true);
            result.setResult("请提供查询对象，如: /status robot 1");
            result.setCommandType("UNKNOWN");
            return result;
        }

        String target = args[0];
        result.setSuccess(true);

        switch (target) {
            case "robot":
                result.setResult("正在查询机器人状态...");
                result.setRedirectUrl("/robots");
                result.setCommandType("ROBOT");
                break;
            case "task":
                result.setResult("正在查询任务状态...");
                result.setRedirectUrl("/tasks");
                result.setCommandType("TASK");
                break;
            default:
                result.setResult("未知查询对象: " + target);
                result.setCommandType("UNKNOWN");
        }

        return result;
    }

    private String generateHelpText() {
        return """
            📖 **快捷指令帮助**
            
            **任务指令:**
            `/run <流程名> [参数]` - 执行流程
            `/tasks` - 查看任务列表
            `/task status <id>` - 查看任务状态
            
            **机器人指令:**
            `/robots` - 查看机器人状态
            `/status robot <id>` - 查看特定机器人
            
            **流程指令:**
            `/flows` - 浏览流程仓库
            `/flows <关键词>` - 搜索流程
            
            **日志指令:**
            `/logs` - 查看执行日志
            `/logs failed` - 查看失败日志
            
            **其他指令:**
            `/help` - 显示帮助
            """;
    }

    // ==================== 内部类 ====================

    public static class ConversationSummary {
        private Long conversationId;
        private String summaryText;
        private int messageCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<String> participants;
        private List<RPACardSummary> rpaCards;
        private List<String> mentions;

        // Getters and Setters
        public ConversationSummary() {}
        public ConversationSummary(String summaryText, int messageCount, List<RPACardSummary> rpaCards, List<String> mentions) {
            this.summaryText = summaryText;
            this.messageCount = messageCount;
            this.rpaCards = rpaCards;
            this.mentions = mentions;
        }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public String getSummaryText() { return summaryText; }
        public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public List<String> getParticipants() { return participants; }
        public void setParticipants(List<String> participants) { this.participants = participants; }
        public List<RPACardSummary> getRpaCards() { return rpaCards; }
        public void setRpaCards(List<RPACardSummary> rpaCards) { this.rpaCards = rpaCards; }
        public List<String> getMentions() { return mentions; }
        public void setMentions(List<String> mentions) { this.mentions = mentions; }
    }

    public static class RPACardSummary {
        private Long messageId;
        private String cardType;
        private String title;
        private String senderName;
        private LocalDateTime createdAt;

        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        public String getCardType() { return cardType; }
        public void setCardType(String cardType) { this.cardType = cardType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class CommandExecutionResult {
        private String command;
        private String[] args;
        private boolean success;
        private String result;
        private String redirectUrl;
        private String commandType;
        private Long userId;
        private LocalDateTime timestamp;

        public CommandExecutionResult() {}

        public CommandExecutionResult(String command, String[] args, boolean success, String result, String redirectUrl, String commandType) {
            this.command = command;
            this.args = args;
            this.success = success;
            this.result = result;
            this.redirectUrl = redirectUrl;
            this.commandType = commandType;
        }

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
        public String getCommandType() { return commandType; }
        public void setCommandType(String commandType) { this.commandType = commandType; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
