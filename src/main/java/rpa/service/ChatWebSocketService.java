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
        // 发送到会话订阅者
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, message);
        log.debug("消息发送到会话 {}: {}", conversationId, message.getId());
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
        
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/unread", payload);
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
