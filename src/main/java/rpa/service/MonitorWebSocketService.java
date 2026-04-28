package rpa.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * 监控WebSocket服务
 * 提供实时监控数据的WebSocket推送
 */
@Service
public class MonitorWebSocketService extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // 每5秒推送一次监控数据
        scheduler.scheduleAtFixedRate(this::broadcastMonitorData, 5, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("Monitor WebSocket connected: " + session.getId());
        
        // 发送欢迎消息
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "connected");
        welcome.put("message", "Monitor WebSocket connected");
        welcome.put("sessionId", session.getId());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            
            switch (type) {
                case "ping":
                    Map<String, Object> pong = new HashMap<>();
                    pong.put("type", "pong");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                    break;
                    
                case "subscribe":
                    System.out.println("Client subscribed: " + session.getId());
                    // 立即发送一次全量数据
                    sendFullData(session);
                    break;
                    
                case "unsubscribe":
                    System.out.println("Client unsubscribed: " + session.getId());
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("Monitor WebSocket disconnected: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
        sessions.remove(session);
    }

    /**
     * 广播监控数据到所有连接的客户端
     */
    private void broadcastMonitorData() {
        if (sessions.isEmpty()) return;
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "monitor_update");
            data.put("timestamp", System.currentTimeMillis());
            data.put("tasks", getTaskStats());
            data.put("robots", getRobotStats());
            
            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage(json);
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (Exception e) {
                        System.err.println("Error sending to session: " + session.getId());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting: " + e.getMessage());
        }
    }

    /**
     * 发送全量数据到指定会话
     */
    private void sendFullData(WebSocketSession session) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "full_sync");
            data.put("timestamp", System.currentTimeMillis());
            data.put("tasks", getTaskStats());
            data.put("robots", getRobotStats());
            data.put("queues", getQueueStats());
            data.put("logs", getLogStats());
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        } catch (Exception e) {
            System.err.println("Error sending full data: " + e.getMessage());
        }
    }

    /**
     * 获取任务统计（示例，实际应从数据库查询）
     */
    private List<Map<String, Object>> getTaskStats() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        // TODO: 从TaskService获取真实数据
        // 这里返回示例数据
        tasks.add(createTask(1L, "发票数据采集", "running", "Robot-01"));
        tasks.add(createTask(2L, "财务报表生成", "pending", null));
        tasks.add(createTask(3L, "客户信息同步", "success", "Robot-02"));
        tasks.add(createTask(4L, "订单自动处理", "failed", "Robot-03"));
        return tasks;
    }

    private Map<String, Object> createTask(Long id, String name, String status, String robotName) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", id);
        task.put("name", name);
        task.put("processName", name);
        task.put("status", status);
        task.put("robotName", robotName);
        task.put("startTime", java.time.LocalDateTime.now().toString());
        return task;
    }

    /**
     * 获取机器人统计（示例）
     */
    private List<Map<String, Object>> getRobotStats() {
        List<Map<String, Object>> robots = new ArrayList<>();
        // TODO: 从RobotService获取真实数据
        robots.add(createRobot(1L, "Robot-01", "idle", 95, 128));
        robots.add(createRobot(2L, "Robot-02", "busy", 92, 256));
        robots.add(createRobot(3L, "Robot-03", "offline", 88, 64));
        robots.add(createRobot(4L, "Robot-04", "idle", 97, 192));
        robots.add(createRobot(5L, "Robot-05", "busy", 91, 320));
        return robots;
    }

    private Map<String, Object> createRobot(Long id, String name, String status, int successRate, int executions) {
        Map<String, Object> robot = new HashMap<>();
        robot.put("id", id);
        robot.put("name", name);
        robot.put("status", status);
        robot.put("successRate", successRate);
        robot.put("todayExecutions", executions);
        return robot;
    }

    /**
     * 获取队列统计（示例）
     */
    private List<Map<String, Object>> getQueueStats() {
        List<Map<String, Object>> queues = new ArrayList<>();
        queues.add(createQueue(1L, "高优先级队列", 45, 100));
        queues.add(createQueue(2L, "普通队列", 128, 200));
        queues.add(createQueue(3L, "低优先级队列", 23, 150));
        return queues;
    }

    private Map<String, Object> createQueue(Long id, String name, int itemCount, int capacity) {
        Map<String, Object> queue = new HashMap<>();
        queue.put("id", id);
        queue.put("name", name);
        queue.put("itemCount", itemCount);
        queue.put("capacity", capacity);
        return queue;
    }

    /**
     * 获取日志统计（示例）
     */
    private List<Map<String, Object>> getLogStats() {
        List<Map<String, Object>> logs = new ArrayList<>();
        // TODO: 从ExecutionLogService获取真实数据
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", (long) i);
            log.put("status", random.nextInt(10) > 1 ? "success" : "failed");
            log.put("startTime", java.time.LocalDateTime.now().minusHours(random.nextInt(24)).toString());
            logs.add(log);
        }
        return logs;
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}
