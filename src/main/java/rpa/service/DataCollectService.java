package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.DataCollect;
import rpa.repository.DataCollectRepository;
import rpa.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * 数据采集配置服务类
 * <p>
 * 提供数据采集配置相关的业务逻辑处理，包括配置CRUD和执行采集任务。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectService {

    private final DataCollectRepository repository;
    private final WebScraperService scraperService;
    private final NotificationService notificationService;

    /**
     * 查询所有采集配置
     */
    public List<DataCollect> findAll() {
        return repository.findAll();
    }

    /**
     * 根据ID查询采集配置
     */
    public Optional<DataCollect> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建采集配置
     *
     * @param request 配置信息
     * @return 创建的配置
     */
    public DataCollect create(Map<String, Object> request) {
        DataCollect collect = new DataCollect();
        collect.setName((String) request.get("name"));
        collect.setSourceUrl((String) request.get("sourceUrl"));
        collect.setSourceType((String) request.getOrDefault("sourceType", "web"));
        collect.setSelectorRules((String) request.get("selectorRules"));
        collect.setHeaders((String) request.get("headers"));
        collect.setCookies((String) request.get("cookies"));
        collect.setCronExpression((String) request.get("cronExpression"));
        collect.setStatus(0);
        collect.setDataCount(0);
        collect.setCreateTime(LocalDateTime.now());
        collect.setUpdateTime(LocalDateTime.now());

        Object creatorIdObj = request.get("creatorId");
        if (creatorIdObj != null) {
            collect.setCreatorId(Long.valueOf(creatorIdObj.toString()));
        }
        collect.setCreatorName((String) request.get("creatorName"));

        return repository.save(collect);
    }

    /**
     * 更新采集配置
     *
     * @param id 配置ID
     * @param request 更新信息
     * @return 更新后的配置
     */
    public DataCollect update(Long id, Map<String, Object> request) {
        return repository.findById(id).map(collect -> {
            if (request.containsKey("name")) collect.setName((String) request.get("name"));
            if (request.containsKey("sourceUrl")) collect.setSourceUrl((String) request.get("sourceUrl"));
            if (request.containsKey("sourceType")) collect.setSourceType((String) request.get("sourceType"));
            if (request.containsKey("selectorRules")) collect.setSelectorRules((String) request.get("selectorRules"));
            if (request.containsKey("headers")) collect.setHeaders((String) request.get("headers"));
            if (request.containsKey("cookies")) collect.setCookies((String) request.get("cookies"));
            if (request.containsKey("cronExpression")) collect.setCronExpression((String) request.get("cronExpression"));
            if (request.containsKey("status")) collect.setStatus((Integer) request.get("status"));
            collect.setUpdateTime(LocalDateTime.now());
            return repository.save(collect);
        }).orElseThrow(() -> new RuntimeException("采集配置不存在"));
    }

    /**
     * 删除采集配置
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 执行采集任务
     * <p>
     * 调用WebScraperService执行实际采集，
     * 更新配置状态和数据计数，
     * 发送采集结果通知。
     * </p>
     *
     * @param id 配置ID
     * @return 执行结果
     */
    public Map<String, Object> executeCollect(Long id) {
        return repository.findById(id).map(collect -> {
            Map<String, Object> result = scraperService.executeCollect(collect);
            boolean success = (boolean) result.getOrDefault("success", false);
            String message = (String) result.getOrDefault("message", "");

            if (success) {
                collect.setStatus(1);
                collect.setLastCollectTime(System.currentTimeMillis());
                collect.setDataCount(collect.getDataCount() + (Integer) result.getOrDefault("count", 0));
            } else {
                collect.setStatus(2);
            }
            repository.save(collect);

            // 发送采集通知
            try {
                notificationService.sendCollectNotification(
                    collect.getId(),
                    collect.getName(),
                    success,
                    message,
                    collect.getCreatorId(),
                    collect.getCreatorName()
                );
            } catch (Exception e) {
                log.warn("发送采集通知失败: {}", e.getMessage());
            }

            return result;
        }).orElseThrow(() -> new RuntimeException("采集配置不存在"));
    }
}
