package rpa.controller;

/**
 * 通知控制器
 * <p>
 * 提供通知相关的RESTful API接口，包括：
 * <ul>
 *   <li>通知管理：CRUD操作，支持分页和筛选</li>
 *   <li>状态管理：标记已读、全部已读</li>
 *   <li>统计分析：各类型数量、未读数、趋势图表</li>
 *   <li>快捷发送：采集结果通知、临时通知、用户操作通知</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Notification;
import rpa.service.NotificationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@CrossOrigin
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Notification> all;
            if (type != null && !type.isEmpty()) {
                if ("read".equals(status) || "unread".equals(status)) {
                    all = service.findByTypeAndStatus(type, status);
                } else {
                    all = service.findByType(type);
                }
            } else {
                all = service.findAll();
            }

            // 分页
            int total = all.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            List<Notification> paged = start < total ? all.subList(start, end) : List.of();

            response.put("code", 0);
            response.put("data", paged);
            response.put("total", total);
            response.put("page", page);
            response.put("pageSize", size);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return service.findById(id).map(n -> {
            response.put("code", 0);
            response.put("data", n);
            return response;
        }).orElseGet(() -> {
            response.put("code", -1);
            response.put("message", "通知不存在");
            return response;
        });
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Notification n = new Notification();
            n.setType((String) request.getOrDefault("type", "temp"));
            n.setTitle((String) request.get("title"));
            n.setContent((String) request.get("content"));
            Object creatorId = request.get("creatorId");
            if (creatorId != null) n.setCreatorId(Long.valueOf(creatorId.toString()));
            n.setCreatorName((String) request.get("creatorName"));
            Object receiverId = request.get("receiverId");
            if (receiverId != null) n.setReceiverId(Long.valueOf(receiverId.toString()));
            Object relatedId = request.get("relatedId");
            if (relatedId != null) n.setRelatedId(Long.valueOf(relatedId.toString()));
            n.setRelatedType((String) request.get("relatedType"));

            Notification saved = service.create(n);
            response.put("code", 0);
            response.put("message", "通知发送成功");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Notification n = service.markAsRead(id);
        if (n != null) {
            response.put("code", 0);
            response.put("message", "已标记为已读");
        } else {
            response.put("code", -1);
            response.put("message", "通知不存在");
        }
        return response;
    }

    @PutMapping("/readAll")
    public Map<String, Object> markAllRead(@RequestParam(required = false) String type) {
        Map<String, Object> response = new HashMap<>();
        if (type != null && !type.isEmpty()) {
            service.markAllAsRead(type);
        } else {
            service.markAllAsRead("collect");
            service.markAllAsRead("temp");
            service.markAllAsRead("user");
        }
        response.put("code", 0);
        response.put("message", "全部已标记为已读");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.delete(id);
            response.put("code", 0);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /** 统计数据 */
    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam(defaultValue = "7") int days) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> counts = service.getCounts();
            Map<String, Long> unread = service.getUnreadCounts();
            List<Map<String, Object>> chartData = service.getDailyStats(days);
            response.put("code", 0);
            response.put("data", Map.of(
                "totals", counts,
                "unreads", unread,
                "chartData", chartData
            ));
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /** 发送采集结果通知 */
    @PostMapping("/collect")
    public Map<String, Object> sendCollectNotification(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long collectId = request.get("collectId") != null ? Long.valueOf(request.get("collectId").toString()) : null;
            String collectName = (String) request.getOrDefault("collectName", "未知采集");
            boolean success = Boolean.TRUE.equals(request.get("success"));
            String message = (String) request.getOrDefault("message", "");
            Long creatorId = request.get("creatorId") != null ? Long.valueOf(request.get("creatorId").toString()) : null;
            String creatorName = (String) request.getOrDefault("creatorName", "");

            Notification n = service.sendCollectNotification(
                collectId, collectName, success, message, creatorId, creatorName);
            response.put("code", 0);
            response.put("message", "通知已发送");
            response.put("data", n);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /** 发送临时通知 */
    @PostMapping("/temp")
    public Map<String, Object> sendTempNotification(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String title = (String) request.getOrDefault("title", "临时通知");
            String content = (String) request.getOrDefault("content", "");
            Long creatorId = request.get("creatorId") != null ? Long.valueOf(request.get("creatorId").toString()) : null;
            String creatorName = (String) request.getOrDefault("creatorName", "");

            Notification n = service.sendTempNotification(title, content, creatorId, creatorName);
            response.put("code", 0);
            response.put("message", "通知已发送");
            response.put("data", n);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /** 发送用户操作通知 */
    @PostMapping("/user")
    public Map<String, Object> sendUserNotification(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
            String userName = (String) request.getOrDefault("userName", "");
            String title = (String) request.getOrDefault("title", "操作通知");
            String content = (String) request.getOrDefault("content", "");

            Notification n = service.sendUserNotification(userId, userName, title, content);
            response.put("code", 0);
            response.put("message", "通知已发送");
            response.put("data", n);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
