package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import rpa.service.ChatWebSocketService;

import java.util.Map;

/**
 * 聊天WebSocket控制器
 * 处理STOMP协议的WebSocket消息
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatWebSocketService chatWebSocketService;

    /**
     * 发送聊天消息
     */
    @MessageMapping("/chat/send")
    @SendToUser("/queue/messages")
    public Map<String, Object> sendMessage(@Payload Map<String, Object> messageData, SimpMessageHeaderAccessor headerAccessor) {
        log.info("收到聊天消息: {}", messageData);
        
        Map<String, Object> response = Map.of(
            "type", "message_sent",
            "success", true,
            "data", messageData
        );
        
        return response;
    }

    /**
     * 加入会话
     */
    @MessageMapping("/chat/join")
    public void joinConversation(@Payload Map<String, Object> data, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = Long.valueOf(data.get("userId").toString());
        Long conversationId = Long.valueOf(data.get("conversationId").toString());
        
        log.info("用户 {} 加入会话 {}", userId, conversationId);
        
        // 广播用户加入消息
        chatWebSocketService.sendSystemNotice(conversationId, "用户加入了会话");
    }

    /**
     * 离开会话
     */
    @MessageMapping("/chat/leave")
    public void leaveConversation(@Payload Map<String, Object> data, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = Long.valueOf(data.get("userId").toString());
        Long conversationId = Long.valueOf(data.get("conversationId").toString());
        
        log.info("用户 {} 离开会话 {}", userId, conversationId);
        
        // 广播用户离开消息
        chatWebSocketService.sendSystemNotice(conversationId, "用户离开了会话");
    }

    /**
     * 获取未读消息数
     */
    @MessageMapping("/chat/unread")
    @SendToUser("/queue/unread")
    public Map<String, Object> getUnreadCount(@Payload Map<String, Object> data) {
        Long userId = Long.valueOf(data.get("userId").toString());
        
        return Map.of(
            "type", "unread_response",
            "unreadCount", 0
        );
    }

    /**
     * WebSocket连接建立
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        log.info("WebSocket连接建立: sessionId={}", sessionId);
    }

    /**
     * WebSocket连接断开
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        log.info("WebSocket连接断开: sessionId={}", sessionId);
    }
}
