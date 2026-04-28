package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rpa.entity.ChatConversation;
import rpa.entity.ChatMessage;
import rpa.service.ChatService;
import rpa.service.AiAssistantService;
import rpa.service.MessageSummaryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final AiAssistantService aiAssistantService;
    private final MessageSummaryService messageSummaryService;

    // ==================== 会话管理 ====================

    /**
     * 获取用户会话列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestParam(defaultValue = "1") Long userId) {
        try {
            List<Map<String, Object>> conversations = chatService.getUserConversations(userId);
            return ResponseEntity.ok(Map.of("code", 0, "data", conversations));
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 创建单聊会话
     */
    @PostMapping("/conversation/private")
    public ResponseEntity<?> createPrivateChat(@RequestBody Map<String, Long> params) {
        try {
            ChatConversation conv = chatService.getOrCreatePrivateChat(params.get("userId1"), params.get("userId2"));
            return ResponseEntity.ok(Map.of("code", 0, "data", conv));
        } catch (Exception e) {
            log.error("创建单聊失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 创建群聊
     */
    @PostMapping("/conversation/group")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> params) {
        try {
            Long ownerId = Long.valueOf(params.get("ownerId").toString());
            String name = params.get("name").toString();
            String description = params.getOrDefault("description", "").toString();
            List<Long> memberIds = ((List<?>) params.get("memberIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .toList();

            ChatConversation conv = chatService.createGroup(ownerId, name, description, memberIds);
            return ResponseEntity.ok(Map.of("code", 0, "data", conv));
        } catch (Exception e) {
            log.error("创建群聊失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 创建RPA临时讨论组
     */
    @PostMapping("/conversation/temporary")
    public ResponseEntity<?> createTemporaryGroup(@RequestBody Map<String, Object> params) {
        try {
            Long creatorId = Long.valueOf(params.get("creatorId").toString());
            String taskName = params.get("taskName").toString();
            Long taskId = Long.valueOf(params.get("taskId").toString());
            List<Long> memberIds = ((List<?>) params.get("memberIds")).stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .toList();

            ChatConversation conv = chatService.createTemporaryGroup(creatorId, taskName, taskId, memberIds);
            return ResponseEntity.ok(Map.of("code", 0, "data", conv));
        } catch (Exception e) {
            log.error("创建临时讨论组失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    // ==================== 消息管理 ====================

    /**
     * 获取会话消息
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ChatMessage> messages = chatService.getHistory(conversationId, page, size);
            return ResponseEntity.ok(Map.of("code", 0, "data", messages));
        } catch (Exception e) {
            log.error("获取消息失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> params) {
        try {
            Long conversationId = Long.valueOf(params.get("conversationId").toString());
            Long senderId = Long.valueOf(params.get("senderId").toString());
            String senderName = params.getOrDefault("senderName", "用户").toString();
            String content = params.get("content").toString();
            String type = params.getOrDefault("type", "text").toString();

            ChatMessage message = chatService.sendMessage(conversationId, senderId, senderName, content, type);
            return ResponseEntity.ok(Map.of("code", 0, "data", message));
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 撤回消息
     */
    @PostMapping("/recall")
    public ResponseEntity<?> recallMessage(@RequestBody Map<String, Long> params) {
        try {
            chatService.recallMessage(params.get("messageId"), params.get("userId"));
            return ResponseEntity.ok(Map.of("code", 0, "message", "撤回成功"));
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    // ==================== AI助手 ====================

    /**
     * AI智能对话
     */
    @PostMapping("/ai")
    public ResponseEntity<?> aiChat(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.getOrDefault("userId", 1L).toString());
            String message = params.get("message").toString();

            // 检查是否是快捷指令
            if (message.startsWith("/")) {
                AiAssistantService.CommandResult result = aiAssistantService.parseCommand(message);
                Map<String, Object> response = new HashMap<>();
                response.put("code", 0);
                response.put("data", Map.of(
                        "reply", result.getResult(),
                        "intent", "COMMAND",
                        "suggestions", result.getSuggestions() != null ? result.getSuggestions() : List.of(),
                        "redirectUrl", result.getRedirectUrl() != null ? result.getRedirectUrl() : ""
                ));
                return ResponseEntity.ok(response);
            }

            // AI对话
            AiAssistantService.AiResponse response = aiAssistantService.generateResponse(userId, message);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("data", Map.of(
                    "reply", response.getReply(),
                    "intent", response.getIntent(),
                    "suggestions", response.getSuggestions() != null ? response.getSuggestions() : List.of()
            ));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    /**
     * 执行快捷指令
     */
    @PostMapping("/command")
    public ResponseEntity<?> executeCommand(@RequestBody Map<String, Object> params) {
        try {
            String command = params.get("command").toString();
            Long userId = Long.valueOf(params.getOrDefault("userId", 1L).toString());

            MessageSummaryService.CommandExecutionResult result = messageSummaryService.executeCommand(command, userId);
            return ResponseEntity.ok(Map.of("code", 0, "data", result));
        } catch (Exception e) {
            log.error("执行命令失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    // ==================== 消息摘要 ====================

    /**
     * 生成会话摘要
     */
    @PostMapping("/summary")
    public ResponseEntity<?> generateSummary(@RequestBody Map<String, Object> params) {
        try {
            Long conversationId = Long.valueOf(params.get("conversationId").toString());
            MessageSummaryService.ConversationSummary summary = messageSummaryService.generateSummary(
                    conversationId,
                    java.time.LocalDateTime.now().minusHours(24),
                    java.time.LocalDateTime.now()
            );
            return ResponseEntity.ok(Map.of("code", 0, "data", summary));
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage()));
        }
    }

    // ==================== 健康检查 ====================

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("code", 0, "message", "Chat service is running"));
    }
}
