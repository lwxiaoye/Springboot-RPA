package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.Notification;
import rpa.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务类
 * <p>
 * 提供通知相关的业务逻辑处理，包括通知CRUD和发送各类通知。
 * </p>
 * <p>
 * 通知类型：
 * <ul>
 *   <li>collect - 采集任务通知</li>
 *   <li>temp - 临时通知</li>
 *   <li>user - 用户操作通知</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /** 通知类型常量：采集任务通知 */
    public static final String TYPE_COLLECT = "collect";
    
    /** 通知类型常量：临时通知 */
    public static final String TYPE_TEMP = "temp";
    
    /** 通知类型常量：用户操作通知 */
    public static final String TYPE_USER = "user";

    // ========== CRUD ==========

    /**
     * 查询所有通知
     */
    public List<Notification> findAll() {
        return repository.findAll();
    }

    /**
     * 根据类型查询通知
     */
    public List<Notification> findByType(String type) {
        return repository.findByTypeOrderByCreateTimeDesc(type);
    }

    /**
     * 根据类型和状态查询通知
     */
    public List<Notification> findByTypeAndStatus(String type, String status) {
        return repository.findByTypeAndStatusOrderByCreateTimeDesc(type, status);
    }

    /**
     * 根据ID查询通知
     */
    public Optional<Notification> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建通知
     *
     * @param notification 通知信息
     * @return 创建的通知
     */
    public Notification create(Notification notification) {
        notification.setCreateTime(LocalDateTime.now());
        notification.setStatus("unread");
        return repository.save(notification);
    }

    /**
     * 发送采集结果通知
     *
     * @param collectId 采集任务ID
     * @param collectName 采集任务名称
     * @param success 是否成功
     * @param message 结果消息
     * @param creatorId 创建者ID
     * @param creatorName 创建者名称
     * @return 创建的通知
     */
    public Notification sendCollectNotification(Long collectId, String collectName,
                                              boolean success, String message,
                                              Long creatorId, String creatorName) {
        Notification n = new Notification();
        n.setType(TYPE_COLLECT);
        n.setRelatedId(collectId);
        n.setRelatedType("collect");
        n.setCreatorId(creatorId);
        n.setCreatorName(creatorName);

        if (success) {
            n.setTitle("采集任务完成");
            n.setContent(String.format("「%s」采集成功。%s", collectName, message));
        } else {
            n.setTitle("采集任务失败");
            n.setContent(String.format("「%s」采集失败。原因：%s", collectName, message));
        }

        return repository.save(n);
    }

    /**
     * 发送临时通知
     *
     * @param title 标题
     * @param content 内容
     * @param creatorId 创建者ID
     * @param creatorName 创建者名称
     * @return 创建的通知
     */
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

    /**
     * 发送用户操作通知
     *
     * @param userId 用户ID
     * @param userName 用户名称
     * @param title 标题
     * @param content 内容
     * @return 创建的通知
     */
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

    /**
     * 发送数据加工完成通知
     *
     * @param processId 加工任务ID
     * @param processName 加工任务名称
     * @param success 是否成功
     * @param message 结果消息
     * @param creatorId 创建者ID
     * @param creatorName 创建者名称
     * @return 创建的通知
     */
    public Notification sendProcessNotification(Long processId, String processName,
                                               boolean success, String message,
                                               Long creatorId, String creatorName) {
        Notification n = new Notification();
        n.setType(TYPE_COLLECT);
        n.setRelatedId(processId);
        n.setRelatedType("process");
        n.setCreatorId(creatorId);
        n.setCreatorName(creatorName);

        if (success) {
            n.setTitle("数据加工完成");
            n.setContent(String.format("「%s」加工成功。%s", processName, message));
        } else {
            n.setTitle("数据加工失败");
            n.setContent(String.format("「%s」加工失败。原因：%s", processName, message));
        }

        return repository.save(n);
    }

    /**
     * 标记单条通知为已读
     *
     * @param id 通知ID
     * @return 更新后的通知
     */
    public Notification markAsRead(Long id) {
        return repository.findById(id).map(n -> {
            n.setStatus("read");
            n.setReadTime(LocalDateTime.now());
            return repository.save(n);
        }).orElse(null);
    }

    /**
     * 批量标记指定类型通知为已读
     *
     * @param type 通知类型
     */
    public void markAllAsRead(String type) {
        List<Notification> list = repository.findByTypeAndStatusOrderByCreateTimeDesc(type, "unread");
        for (Notification n : list) {
            n.setStatus("read");
            n.setReadTime(LocalDateTime.now());
            repository.save(n);
        }
    }

    /**
     * 删除通知
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ========== 统计 ==========

    /**
     * 获取各类型通知总数（最近30天）
     *
     * @return 各类型数量统计
     */
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

    /**
     * 获取各类型未读数统计
     *
     * @return 各类型未读数
     */
    public Map<String, Long> getUnreadCounts() {
        Map<String, Long> result = new HashMap<>();
        result.put("collect", (long) repository.findByTypeAndStatusOrderByCreateTimeDesc(TYPE_COLLECT, "unread").size());
        result.put("temp", (long) repository.findByTypeAndStatusOrderByCreateTimeDesc(TYPE_TEMP, "unread").size());
        result.put("user", (long) repository.findByTypeAndStatusOrderByCreateTimeDesc(TYPE_USER, "unread").size());
        return result;
    }

    /**
     * 按日期统计三类通知数量（用于图表展示）
     *
     * @param days 统计天数
     * @return 每日统计数据列表
     */
    public List<Map<String, Object>> getDailyStats(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        List<Object[]> raw = repository.countByDateAndType(start);

        // 按日期分组
        Map<String, Map<String, Integer>> dateMap = new LinkedHashMap<>();
        for (Object[] row : raw) {
            String dateStr = row[0] != null ? row[0].toString() : "";
            String type = (String) row[1];
            Long count = (Long) row[2];

            dateMap.computeIfAbsent(dateStr, k -> new HashMap<>());
            dateMap.get(dateStr).put(type, count.intValue());
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : dateMap.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", entry.getKey());
            item.put("collect", entry.getValue().getOrDefault(TYPE_COLLECT, 0));
            item.put("temp", entry.getValue().getOrDefault(TYPE_TEMP, 0));
            item.put("user", entry.getValue().getOrDefault(TYPE_USER, 0));
            chartData.add(item);
        }

        return chartData;
    }
}
