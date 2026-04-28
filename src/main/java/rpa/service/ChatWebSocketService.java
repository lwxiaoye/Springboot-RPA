package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import rpa.entity.ChatMessage;
import rpa.repository.ChatMessageRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天WebSocket服务
 * 处理实时消息推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository messageRepository;

    // 在线用户映射: userId -> sessionId
    private final Map<Long, String> onlineUsers = new ConcurrentHashMap<>();

    /**
     * 用户上线
     */
    public void userOnline(Long userId, String sessionId) {
        onlineUsers.put(userId, sessionId);
        log.info("用户 {} 上线, session: {}", userId, sessionId);
        
        // 广播用户状态变化
        broadcastUserStatus(userId, true);
    }

    /**
     * 用户下线
     */
    public void userOffline(Long userId) {
        onlineUsers.remove(userId);
        log.info("用户 {} 下线", userId);
        
        // 广播用户状态变化
        broadcastUserStatus(userId, false);
    }

    /**
     * 发送消息到指定会话
     */
    public void sendMessageToConversation(Long conversationId, ChatMessage message) {
        // 构建统一的消息格式，确保前端能正确解析
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", "new_message");
        payload.put("message", message);
        payload.put("conversationId", conversationId);
        
        // 发送到会话订阅者
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);
        log.debug("消息发送到会话 {}: {}", conversationId, message.getId());
        
        // 同时广播会话列表更新通知给所有参与者
        broadcastConversationUpdate(conversationId, message);
    }
    
    /**
     * 广播会话列表更新
     */
    private void broadcastConversationUpdate(Long conversationId, ChatMessage message) {
        // 获取会话的所有参与者
        // 这里发送一个轻量级的更新通知，前端收到后会刷新会话列表
        Map<String, Object> notification = new ConcurrentHashMap<>();
        notification.put("type", "conversation_update");
        notification.put("conversationId", conversationId);
        notification.put("action", "message_sent");
        notification.put("lastMessage", message.getContent());
        notification.put("lastMessageTime", message.getCreatedAt().toString());
        notification.put("senderId", message.getSenderId());
        notification.put("senderName", message.getSenderName());
        
        // 广播给所有相关用户（这里用topic，实际应该发送给具体的用户）
        messagingTemplate.convertAndSend("/topic/conversations/updates", notification);
    }

    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(Long userId, Object payload) {
        // 检查用户是否在线
        if (onlineUsers.containsKey(userId)) {
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/messages", payload);
            log.debug("消息发送给用户 {}: {}", userId, payload);
        } else {
            log.debug("用户 {} 不在线，消息将存储到数据库", userId);
        }
    }

    /**
     * 发送未读消息数更新
     */
    public void sendUnreadCountUpdate(Long userId, int unreadCount) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", "unread_update");
        payload.put("unreadCount", unreadCount);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/unread", payload);
        log.debug("未读数更新发送给用户 {}: {}", userId, unreadCount);
    }

    /**
     * 广播用户状态变化
     */
    private void broadcastUserStatus(Long userId, boolean online) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", "user_status");
        payload.put("userId", userId);
        payload.put("online", online);
        
        messagingTemplate.convertAndSend("/topic/users/status", payload);
    }

    /**
     * 发送系统通知
     */
    public void sendSystemNotice(Long conversationId, String content) {
        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("type", "system_notice");
        payload.put("content", content);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, payload);
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }
}
