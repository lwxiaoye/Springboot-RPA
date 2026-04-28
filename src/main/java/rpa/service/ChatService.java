package rpa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.*;
import rpa.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RPA协作中枢 - 聊天核心服务
 * 
 * 功能定位：
 * 1. 基础聊天：单聊、群聊、消息收发
 * 2. RPA深度集成：任务卡片、机器人卡片、流程分享
 * 3. 金融合规：敏感词过滤、审计日志、消息归档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private ChatWebSocketService chatWebSocketService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setChatWebSocketService(ChatWebSocketService chatWebSocketService) {
        this.chatWebSocketService = chatWebSocketService;
    }

    // ==================== 会话管理 ====================

    /**
     * 创建或获取单聊会话
     */
    @Transactional
    public ChatConversation getOrCreatePrivateChat(Long userId1, Long userId2) {
        Optional<ChatConversation> existing = conversationRepository.findPrivateConversation(userId1, userId2);
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setType("private");
        conversation.setCreatedBy(userId1);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation.getId(), userId1, "member");
        addParticipant(conversation.getId(), userId2, "member");

        logAudit(userId1, "CREATE_CONVERSATION", "创建单聊会话", null, conversation.getId(), null);

        return conversation;
    }

    /**
     * 创建群聊
     */
    @Transactional
    public ChatConversation createGroup(Long ownerId, String name, String description, List<Long> memberIds) {
        ChatConversation conversation = new ChatConversation();
        conversation.setType("group");
        conversation.setName(name);
        conversation.setDescription(description);
        conversation.setOwnerId(ownerId);
        conversation.setCreatedBy(ownerId);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation.getId(), ownerId, "owner");
        for (Long memberId : memberIds) {
            if (!memberId.equals(ownerId)) {
                addParticipant(conversation.getId(), memberId, "member");
            }
        }

        conversation.setMemberCount(memberIds.size() + 1);
        conversationRepository.save(conversation);

        logAudit(ownerId, "CREATE_GROUP", "创建群聊: " + name, null, conversation.getId(), null);

        return conversation;
    }

    /**
     * 创建RPA临时讨论组
     */
    @Transactional
    public ChatConversation createTemporaryGroup(Long creatorId, String taskName, Long taskId, List<Long> memberIds) {
        ChatConversation conversation = new ChatConversation();
        conversation.setType("temporary");
        conversation.setName("【任务排查】" + taskName);
        conversation.setDescription("任务ID: " + taskId + " 异常排查临时群");
        conversation.setOwnerId(creatorId);
        conversation.setCreatedBy(creatorId);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setRelatedType("task");
        conversation.setRelatedId(taskId);
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation.getId(), creatorId, "owner");
        for (Long memberId : memberIds) {
            if (!memberId.equals(creatorId)) {
                addParticipant(conversation.getId(), memberId, "member");
            }
        }

        sendSystemMessage(conversation.getId(), "系统自动创建的临时讨论组，用于排查任务异常。任务处理完成后可归档。");

        logAudit(creatorId, "CREATE_TEMP_GROUP", "创建临时讨论组: " + taskName, null, conversation.getId(), "task");

        return conversation;
    }

    /**
     * 添加会话参与者
     */
    @Transactional
    public void addParticipant(Long conversationId, Long userId, String role) {
        if (participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            return;
        }

        ChatParticipant participant = new ChatParticipant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setJoinedAt(LocalDateTime.now());
        participantRepository.save(participant);

        ChatConversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setMemberCount((int) participantRepository.countByConversationIdAndLeftAtIsNull(conversationId));
            conversationRepository.save(conv);
        }
    }

    /**
     * 获取用户会话列表
     */
    public List<Map<String, Object>> getUserConversations(Long userId) {
        List<ChatConversation> conversations = conversationRepository.findUserConversations(userId);
        return conversations.stream().map(conv -> {
            Map<String, Object> result = new HashMap<>();
            result.put("conversation", conv);

            Optional<ChatParticipant> participant = participantRepository.findByConversationIdAndUserId(conv.getId(), userId);
            result.put("unreadCount", participant.map(ChatParticipant::getUnreadCount).orElse(0));
            result.put("isPinned", participant.map(ChatParticipant::getIsPinned).orElse(0));

            if ("private".equals(conv.getType())) {
                List<ChatParticipant> participants = participantRepository.findByConversationId(conv.getId());
                for (ChatParticipant p : participants) {
                    if (!p.getUserId().equals(userId)) {
                        result.put("otherUserId", p.getUserId());
                        // 优先使用群昵称，如果没有则查询用户真实姓名
                        String displayName = p.getNickname();
                        if (displayName == null || displayName.isEmpty()) {
                            Optional<User> user = userRepository.findById(p.getUserId());
                            if (user.isPresent()) {
                                displayName = user.get().getRealName() != null ? user.get().getRealName() : user.get().getUsername();
                                // 添加头像
                                result.put("otherUserAvatar", user.get().getAvatar());
                            }
                        }
                        result.put("otherUserName", displayName != null ? displayName : "未知用户");
                    }
                }
            }

            return result;
        }).collect(Collectors.toList());
    }

    // ==================== 消息管理 ====================

    /**
     * 发送消息
     */
    @Transactional
    public ChatMessage sendMessage(Long conversationId, Long senderId, String senderName, String content, String type) {
        Optional<ChatParticipant> participant = participantRepository.findByConversationIdAndUserId(conversationId, senderId);
        if (participant.isEmpty()) {
            throw new RuntimeException("您不在此会话中");
        }
        if (participant.get().getIsMuted() == 1) {
            throw new RuntimeException("您已被禁言");
        }

        String filteredContent = filterSensitiveWords(content);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setType(type != null ? type : "text");
        message.setContent(filteredContent);
        message.setCreatedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        updateConversationLastMessage(conversationId, message);
        participantRepository.incrementUnreadCount(conversationId, senderId);
        logAudit(senderId, "SEND_MESSAGE", "发送消息", message.getId(), conversationId, null);

        // 通过WebSocket推送消息到会话
        if (chatWebSocketService != null) {
            chatWebSocketService.sendMessageToConversation(conversationId, message);
            // 给其他参与者发送未读更新
            List<ChatParticipant> participants = participantRepository.findByConversationId(conversationId);
            for (ChatParticipant p : participants) {
                if (!p.getUserId().equals(senderId)) {
                    chatWebSocketService.sendUnreadCountUpdate(p.getUserId(), p.getUnreadCount() + 1);
                }
            }
        }

        // 发布消息事件
        eventPublisher.publishEvent(new ChatMessageEvent(this, message, "NEW_MESSAGE"));

        return message;
    }

    /**
     * 发送RPA卡片消息
     */
    @Transactional
    public ChatMessage sendRPACard(Long conversationId, Long senderId, String senderName, RPACardData cardData, List<RPACardData.CardAction> actions) {
        try {
            cardData.setActions(actions);
            String cardDataJson = objectMapper.writeValueAsString(cardData);

            ChatMessage message = new ChatMessage();
            message.setConversationId(conversationId);
            message.setSenderId(senderId);
            message.setSenderName(senderName);
            message.setType("rpa_card");
            message.setCardType(cardData.getCardType());
            message.setCardData(cardDataJson);
            message.setContent("[" + getCardTypeName(cardData.getCardType()) + "卡片]");
            message.setCreatedAt(LocalDateTime.now());
            message = messageRepository.save(message);

            updateConversationLastMessage(conversationId, message);
            participantRepository.incrementUnreadCount(conversationId, senderId);
            logAudit(senderId, "SEND_RPA_CARD", "发送RPA卡片: " + cardData.getCardType(), 
                    message.getId(), conversationId, cardData.getCardType());

            eventPublisher.publishEvent(new ChatMessageEvent(this, message, "NEW_CARD"));

            return message;

        } catch (Exception e) {
            log.error("发送RPA卡片失败", e);
            throw new RuntimeException("发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送系统消息
     */
    @Transactional
    public ChatMessage sendSystemMessage(Long conversationId, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(0L);
        message.setSenderName("系统");
        message.setType("notice");
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        updateConversationLastMessage(conversationId, message);
        eventPublisher.publishEvent(new ChatMessageEvent(this, message, "NEW_NOTICE"));

        return message;
    }

    /**
     * 获取历史消息
     */
    public List<ChatMessage> getHistory(Long conversationId, int page, int size) {
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, org.springframework.data.domain.PageRequest.of(page, size)
        ).getContent().stream().sorted(Comparator.comparing(ChatMessage::getCreatedAt)).collect(Collectors.toList());
    }

    /**
     * 撤回消息
     */
    @Transactional
    public void recallMessage(Long messageId, Long userId) {
        ChatMessage message = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("消息不存在"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("只能撤回自己的消息");
        }

        LocalDateTime now = LocalDateTime.now();
        if (message.getCreatedAt().plusHours(24).isBefore(now)) {
            throw new RuntimeException("超过24小时的消息不能撤回");
        }

        messageRepository.recallMessage(messageId, now, userId);
        logAudit(userId, "RECALL_MESSAGE", "撤回消息", messageId, message.getConversationId(), null);

        eventPublisher.publishEvent(new ChatMessageEvent(this, message, "MESSAGE_RECALLED"));
    }

    /**
     * 标记会话已读
     * @return 更新后的未读消息数量
     */
    @Transactional
    public int markAsRead(Long conversationId, Long userId) {
        // 先获取当前的 participant
        ChatParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId).orElse(null);
        if (participant == null) {
            return 0;
        }

        // 获取最后一条已读时间（如果没有，则使用当前时间之前的某个时间点）
        LocalDateTime lastReadTime = participant.getLastReadTime();
        LocalDateTime now = LocalDateTime.now();

        // 如果没有已读时间，说明之前没有任何阅读记录
        // 我们需要获取该会话的第一条消息的时间作为基准
        if (lastReadTime == null) {
            // 获取该会话最早的消息时间
            lastReadTime = messageRepository.findFirstByConversationIdOrderByCreatedAtAsc(conversationId)
                    .map(ChatMessage::getCreatedAt)
                    .orElse(now);
        }

        // 计算从最后阅读时间到现在有多少条未读消息（排除自己发的消息）
        long unreadCount = messageRepository.countUnreadMessages(conversationId, lastReadTime, userId);

        // 更新已读时间和未读数
        participant.setLastReadTime(now);
        participant.setUnreadCount((int) unreadCount);
        participantRepository.save(participant);

        logAudit(userId, "MARK_READ", "标记会话已读", null, conversationId, null);

        return (int) unreadCount;
    }

    /**
     * 获取用户所有会话的未读消息总数
     */
    public int getTotalUnreadCount(Long userId) {
        List<ChatConversation> conversations = conversationRepository.findUserConversations(userId);
        int totalUnread = 0;
        for (ChatConversation conv : conversations) {
            Optional<ChatParticipant> participant = participantRepository.findByConversationIdAndUserId(conv.getId(), userId);
            if (participant.isPresent()) {
                totalUnread += participant.get().getUnreadCount();
            }
        }
        return totalUnread;
    }

    // ==================== RPA深度集成 ====================

    /**
     * 发送任务状态卡片
     */
    public ChatMessage sendTaskCard(Long conversationId, Long senderId, String senderName, Task task) {
        RPACardData cardData = new RPACardData();
        cardData.setCardType("TASK");
        cardData.setTaskId(task.getId());
        cardData.setTaskName(task.getName());
        cardData.setTaskStatus(task.getStatus());
        cardData.setStartTime(task.getStartTime());
        cardData.setEndTime(task.getEndTime());
        cardData.setErrorMessage(task.getErrorMessage());

        List<RPACardData.CardAction> actions = new ArrayList<>();
        if ("failed".equals(task.getStatus())) {
            actions.add(new RPACardData.CardAction("retry", "重试", "确定重试此任务?", "Refresh"));
            actions.add(new RPACardData.CardAction("assign", "转派", null, "User"));
        }
        if ("running".equals(task.getStatus())) {
            actions.add(new RPACardData.CardAction("pause", "暂停", "确定暂停此任务?", "VideoPause"));
            actions.add(new RPACardData.CardAction("stop", "终止", "确定终止此任务?", "Close"));
        }
        actions.add(new RPACardData.CardAction("view", "查看详情", null, "View"));

        return sendRPACard(conversationId, senderId, senderName, cardData, actions);
    }

    /**
     * 发送机器人状态卡片
     */
    public ChatMessage sendRobotCard(Long conversationId, Long senderId, String senderName, Robot robot) {
        RPACardData cardData = new RPACardData();
        cardData.setCardType("ROBOT");
        cardData.setRobotId(robot.getId());
        cardData.setRobotName(robot.getName());
        cardData.setRobotStatus(robot.getStatus());

        List<RPACardData.CardAction> actions = new ArrayList<>();
        actions.add(new RPACardData.CardAction("restart", "重启", "确定重启机器人?", "Refresh"));
        actions.add(new RPACardData.CardAction("view_logs", "查看日志", null, "Document"));

        return sendRPACard(conversationId, senderId, senderName, cardData, actions);
    }

    /**
     * 分享执行日志
     */
    public ChatMessage shareExecutionLog(Long conversationId, Long senderId, String senderName, 
                                        Long logId, String logSummary, String logUrl) {
        RPACardData cardData = new RPACardData();
        cardData.setCardType("LOG");
        cardData.setLogId(logId);
        cardData.setLogSummary(logSummary);
        cardData.setLogTime(LocalDateTime.now());

        List<RPACardData.CardAction> actions = new ArrayList<>();
        actions.add(new RPACardData.CardAction("view_detail", "查看详情", null, "View"));

        return sendRPACard(conversationId, senderId, senderName, cardData, actions);
    }

    // ==================== 金融合规功能 ====================

    /**
     * 敏感词过滤
     */
    private String filterSensitiveWords(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        Map<String, String> sensitivePatterns = new HashMap<>();
        sensitivePatterns.put("\\d{16,19}", "****");  // 卡号
        sensitivePatterns.put("\\d{15}|\\d{18}", "***");  // 身份证
        sensitivePatterns.put("1[3-9]\\d{9}", "***");  // 手机号

        String filtered = content;
        for (Map.Entry<String, String> entry : sensitivePatterns.entrySet()) {
            filtered = filtered.replaceAll(entry.getKey(), entry.getValue());
        }

        return filtered;
    }

    /**
     * 记录审计日志
     */
    private void logAudit(Long userId, String action, String detail, Long messageId, Long conversationId, String relatedType) {
        ChatAuditLog auditLog = new ChatAuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setActionDetail(detail);
        auditLog.setMessageId(messageId);
        auditLog.setConversationId(conversationId);
        auditLog.setRelatedType(relatedType);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    // ==================== 辅助方法 ====================

    private void updateConversationLastMessage(Long conversationId, ChatMessage message) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setLastMessageTime(message.getCreatedAt());
            conv.setLastMessageContent(message.getContent());
            conv.setMessageCount(conv.getMessageCount() + 1);
            conversationRepository.save(conv);
        });
    }

    private String getCardTypeName(String cardType) {
        return switch (cardType) {
            case "TASK" -> "任务";
            case "ROBOT" -> "机器人";
            case "FLOW" -> "流程";
            case "LOG" -> "日志";
            case "APPROVAL" -> "审批";
            case "ALERT" -> "告警";
            default -> "RPA";
        };
    }

    /**
     * 聊天消息事件
     */
    public static class ChatMessageEvent extends org.springframework.context.ApplicationEvent {
        private final ChatMessage message;
        private final String eventType;

        public ChatMessageEvent(Object source, ChatMessage message, String eventType) {
            super(source);
            this.message = message;
            this.eventType = eventType;
        }

        public ChatMessage getMessage() { return message; }
        public String getEventType() { return eventType; }
    }
}
