package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.Notification;
import rpa.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public static final String TYPE_COLLECT = "collect";
    public static final String TYPE_TEMP = "temp";
    public static final String TYPE_USER = "user";

    public List<Notification> findAll() {
        return repository.findAll();
    }

    public List<Notification> findByType(String type) {
        return repository.findByTypeOrderByCreateTimeDesc(type);
    }

    public List<Notification> findByTypeAndStatus(String type, String status) {
        return repository.findByTypeAndStatusOrderByCreateTimeDesc(type, status);
    }

    public Optional<Notification> findById(Long id) {
        return repository.findById(id);
    }

    public Notification create(Notification notification) {
        notification.setCreateTime(LocalDateTime.now());
        notification.setStatus("unread");
        return repository.save(notification);
    }

    public Notification sendCollectNotification(Long collectId, String collectName,
                                              boolean success, String message,
                                              Long creatorId, String creatorName) {
        Notification n = new Notification();
        n.setType(TYPE_COLLECT);
        n.setRelatedId(collectId);
        n.setRelatedType("collect");
        n.setCreatorId(creatorId);
        n.setCreatorName(creatorName);
        n.setTitle(success ? "采集任务完成" : "采集任务失败");
        n.setContent(String.format("「%s」%s。%s", collectName, success ? "采集成功" : "采集失败", message));
        return repository.save(n);
    }

    public Notification sendTempNotification(String title, String content,
                                           Long creatorId, String creatorName) {
        Notification n = new Notification();
        n.setType(TYPE_TEMP);
        n.setTitle(title);
        n.setContent(content);
        n.setCreatorId(creatorId);
        n.setCreatorName(creatorName);
        return repository.save(n);
    }

    public Notification sendUserNotification(Long userId, String userName,
                                           String title, String content) {
        Notification n = new Notification();
        n.setType(TYPE_USER);
        n.setReceiverId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setCreatorId(userId);
        n.setCreatorName(userName);
        return repository.save(n);
    }

    public Notification sendProcessNotification(Long processId, String processName,
                                               boolean success, String message,
                                               Long creatorId, String creatorName) {
        Notification n = new Notification();
        n.setType(TYPE_COLLECT);
        n.setRelatedId(processId);
        n.setRelatedType("process");
        n.setCreatorId(creatorId);
        n.setCreatorName(creatorName);
        n.setTitle(success ? "数据加工完成" : "数据加工失败");
        n.setContent(String.format("「%s」%s。%s", processName, success ? "加工成功" : "加工失败", message));
        return repository.save(n);
    }

    public Notification markAsRead(Long id) {
        return repository.findById(id).map(n -> {
            n.setStatus("read");
            n.setReadTime(LocalDateTime.now());
            return repository.save(n);
        }).orElse(null);
    }

    public void markAllAsRead(String type) {
        List<Notification> list = repository.findByTypeAndStatusOrderByCreateTimeDesc(type, "unread");
        for (Notification n : list) {
            n.setStatus("read");
            n.setReadTime(LocalDateTime.now());
            repository.save(n);
        }
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Map<String, Object> getCounts() {
        Map<String, Object> result = new HashMap<>();
        result.put("collect", repository.countByTypeAndTimeRange(TYPE_COLLECT,
            LocalDateTime.now().minusDays(30), LocalDateTime.now()));
        result.put("temp", repository.countByTypeAndTimeRange(TYPE_TEMP,
            LocalDateTime.now().minusDays(30), LocalDateTime.now()));
        result.put("user", repository.countByTypeAndTimeRange(TYPE_USER,
            LocalDateTime.now().minusDays(30), LocalDateTime.now()));
        return result;
    }

    public Map<String, Long> getUnreadCounts() {
        Map<String, Long> result = new HashMap<>();
        result.put("collect", (long) repository.findByTypeAndStatusOrderByCreateTimeDesc(TYPE_COLLECT, "unread").size());
        result.put("temp", (long) repository.findByTypeAndStatusOrderByCreateTimeDesc(TYPE_TEMP, "unread").size());
        result.put("user", (long) repository.findByTypeAndStatusOrderByCreateTimeDesc(TYPE_USER, "unread").size());
        return result;
    }

    public List<Map<String, Object>> getDailyStats(int days) {
        List<Map<String, Object>> chartData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = days - 1; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", now.minusDays(i).toLocalDate().toString());
            item.put("collect", 0);
            item.put("temp", 0);
            item.put("user", 0);
            chartData.add(item);
        }
        return chartData;
    }
}
