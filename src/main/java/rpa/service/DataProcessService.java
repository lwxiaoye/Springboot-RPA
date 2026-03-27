package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.DataProcess;
import rpa.entity.CollectedData;
import rpa.entity.ProcessedData;
import rpa.repository.DataProcessRepository;
import rpa.repository.CollectedDataRepository;
import rpa.repository.ProcessedDataRepository;
import rpa.service.NotificationService;
import com.alibaba.fastjson.JSON;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 数据加工服务类
 * <p>
 * 提供数据加工相关的业务逻辑处理，包括配置CRUD和执行加工任务。
 * </p>
 * <p>
 * 支持的加工规则：
 * <ul>
 *   <li>filter - 过滤规则，不满足条件的数据被过滤</li>
 *   <li>transform - 转换规则，支持{value}占位符替换</li>
 *   <li>mapping - 字段映射，重命名字段</li>
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
public class DataProcessService {

    private final DataProcessRepository repository;
    private final CollectedDataRepository collectedDataRepository;
    private final ProcessedDataRepository processedDataRepository;
    private final NotificationService notificationService;

    /**
     * 查询所有加工配置
     */
    public List<DataProcess> findAll() {
        return repository.findAll();
    }

    /**
     * 根据ID查询加工配置
     */
    public Optional<DataProcess> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建加工配置
     *
     * @param request 配置信息
     * @return 创建的配置
     */
    public DataProcess create(Map<String, Object> request) {
        DataProcess process = new DataProcess();
        process.setName((String) request.get("name"));
        process.setSourceIds((String) request.get("sourceIds"));
        process.setProcessType((String) request.getOrDefault("processType", "transform"));
        process.setProcessRules((String) request.get("processRules"));
        process.setOutputTable((String) request.get("outputTable"));
        process.setStatus(0);
        process.setProcessedCount(0);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());
        return repository.save(process);
    }

    /**
     * 更新加工配置
     *
     * @param id 配置ID
     * @param request 更新信息
     * @return 更新后的配置
     */
    public DataProcess update(Long id, Map<String, Object> request) {
        return repository.findById(id).map(process -> {
            if (request.containsKey("name")) process.setName((String) request.get("name"));
            if (request.containsKey("sourceIds")) process.setSourceIds((String) request.get("sourceIds"));
            if (request.containsKey("processType")) process.setProcessType((String) request.get("processType"));
            if (request.containsKey("processRules")) process.setProcessRules((String) request.get("processRules"));
            if (request.containsKey("outputTable")) process.setOutputTable((String) request.get("outputTable"));
            if (request.containsKey("status")) process.setStatus((Integer) request.get("status"));
            process.setUpdateTime(LocalDateTime.now());
            return repository.save(process);
        }).orElseThrow(() -> new RuntimeException("加工配置不存在"));
    }

    /**
     * 删除加工配置
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 执行数据加工
     * <p>
     * 从多个采集数据源获取原始数据，
     * 应用加工规则（filter/transform/mapping），
     * 保存加工结果到ProcessedData表，
     * 发送加工完成通知。
     * </p>
     *
     * @param id 配置ID
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeProcess(Long id) {
        return repository.findById(id).map(process -> {
            Map<String, Object> result = doProcess(process);
            boolean success = (boolean) result.getOrDefault("success", false);
            String message = (String) result.getOrDefault("message", "");

            if (success) {
                process.setStatus(1);
                process.setLastProcessTime(System.currentTimeMillis());
            } else {
                process.setStatus(2);
            }
            repository.save(process);

            // 发送加工通知
            try {
                notificationService.sendProcessNotification(
                    process.getId(),
                    process.getName(),
                    success,
                    message,
                    null,
                    null
                );
            } catch (Exception e) {
                log.warn("发送加工通知失败: {}", e.getMessage());
            }

            return result;
        }).orElseThrow(() -> new RuntimeException("加工配置不存在"));
    }

    /**
     * 执行加工逻辑
     */
    private Map<String, Object> doProcess(DataProcess process) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始数据加工: {}", process.getName());

            List<CollectedData> dataList = getSourceData(process.getSourceIds());
            List<Map<String, String>> processedDataList = new ArrayList<>();

            int savedCount = 0;

            for (CollectedData data : dataList) {
                Map<String, String> item = new HashMap<>();

                // 获取原始数据
                if (data.getRawData() != null) {
                    try {
                        com.alibaba.fastjson.JSONObject raw = JSON.parseObject(data.getRawData());
                        for (String key : raw.keySet()) {
                            item.put(key, raw.getString(key));
                        }
                    } catch (Exception e) {
                        log.warn("解析原始数据失败: {}", e.getMessage());
                    }
                }

                // 获取解析后数据（优先）
                if (data.getParsedData() != null && !data.getParsedData().isEmpty()) {
                    try {
                        com.alibaba.fastjson.JSONObject parsed = JSON.parseObject(data.getParsedData());
                        for (String key : parsed.keySet()) {
                            item.put(key, parsed.getString(key));
                        }
                    } catch (Exception e) {
                        log.warn("解析数据失败: {}", e.getMessage());
                    }
                }

                item.put("_id", String.valueOf(data.getId()));
                item.put("_collectTime", data.getCollectTime().toString());
                processedDataList.add(item);

                // 应用加工规则
                Map<String, String> processedItem = applyRulesToItem(item, process.getProcessRules());

                // 保存到数据库
                ProcessedData savedData = saveProcessedData(process, data, processedItem);
                if (savedData != null) {
                    savedCount++;
                }
            }

            process.setProcessedCount(process.getProcessedCount() + savedCount);

            result.put("success", true);
            result.put("message", "加工完成");
            result.put("count", savedCount);
            result.put("totalProcessed", processedDataList.size());

            log.info("数据加工完成: {}, 处理数量: {}", process.getName(), savedCount);

        } catch (Exception e) {
            log.error("数据加工失败: {}", process.getName(), e);
            result.put("success", false);
            result.put("message", "加工失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 保存加工后的数据到数据库
     */
    private ProcessedData saveProcessedData(DataProcess process, CollectedData sourceData, Map<String, String> processedItem) {
        try {
            ProcessedData processed = new ProcessedData();
            processed.setName(sourceData.getCollectName() + "_" + process.getName());
            processed.setProcessId(process.getId());
            processed.setProcessName(process.getName());
            processed.setCollectId(sourceData.getCollectId());
            processed.setCollectName(sourceData.getCollectName());
            processed.setSourceData(sourceData.getRawData());
            processed.setProcessedData(JSON.toJSONString(processedItem));
            processed.setSourceUrl(sourceData.getSourceUrl());
            processed.setSourceType(sourceData.getDataType());
            processed.setDataCategory(process.getProcessType());
            processed.setStatus(1);
            processed.setProcessTime(LocalDateTime.now());
            processed.setCreateTime(LocalDateTime.now());

            return processedDataRepository.save(processed);
        } catch (Exception e) {
            log.error("保存加工数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对单条数据应用加工规则
     * <p>
     * 支持的规则类型：
     * <ul>
     *   <li>filter - 过滤，满足条件返回_filtered=true</li>
     *   <li>transform - 转换，{value}占位符替换</li>
     *   <li>mapping - 映射，重命名字段</li>
     * </ul>
     * </p>
     */
    private Map<String, String> applyRulesToItem(Map<String, String> item, String rulesJson) {
        Map<String, String> result = new HashMap<>(item);

        if (rulesJson == null || rulesJson.isEmpty()) {
            return result;
        }

        try {
            com.alibaba.fastjson.JSONObject rules = JSON.parseObject(rulesJson);

            // 应用过滤规则
            if (rules.containsKey("filter")) {
                com.alibaba.fastjson.JSONObject filter = rules.getJSONObject("filter");
                for (String key : filter.keySet()) {
                    String expected = String.valueOf(filter.get(key));
                    String actual = result.get(key);
                    if (actual == null || !actual.equals(expected)) {
                        result.put("_filtered", "true");
                        return result;
                    }
                }
            }

            // 应用转换规则
            if (rules.containsKey("transform")) {
                com.alibaba.fastjson.JSONObject transform = rules.getJSONObject("transform");
                for (String key : transform.keySet()) {
                    String value = result.get(key);
                    if (value != null) {
                        String transformRule = String.valueOf(transform.get(key));
                        result.put(key, transformRule.replace("{value}", value));
                    }
                }
            }

            // 应用映射规则
            if (rules.containsKey("mapping")) {
                com.alibaba.fastjson.JSONObject mapping = rules.getJSONObject("mapping");
                Map<String, String> mapped = new HashMap<>();
                for (String newKey : mapping.keySet()) {
                    String oldKey = String.valueOf(mapping.get(newKey));
                    String value = result.get(oldKey);
                    if (value != null) {
                        mapped.put(newKey, value);
                    }
                }
                result.putAll(mapped);
            }

        } catch (Exception e) {
            log.error("应用加工规则失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取数据源
     */
    private List<CollectedData> getSourceData(String sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return collectedDataRepository.findAll();
        }

        List<Long> ids = Arrays.stream(sourceIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<CollectedData> result = new ArrayList<>();
        for (Long id : ids) {
            collectedDataRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    /**
     * 应用加工规则（批量）
     */
    private List<Map<String, String>> applyProcessRules(List<Map<String, String>> data, String rulesJson) {
        if (rulesJson == null || rulesJson.isEmpty()) {
            return data;
        }

        try {
            com.alibaba.fastjson.JSONObject rules = JSON.parseObject(rulesJson);
            List<Map<String, String>> result = new ArrayList<>();

            for (Map<String, String> item : data) {
                Map<String, String> processed = new HashMap<>(item);

                if (rules.containsKey("filter")) {
                    com.alibaba.fastjson.JSONObject filter = rules.getJSONObject("filter");
                    boolean shouldInclude = true;

                    for (String key : filter.keySet()) {
                        String expected = String.valueOf(filter.get(key));
                        String actual = processed.get(key);
                        if (actual == null || !actual.equals(expected)) {
                            shouldInclude = false;
                            break;
                        }
                    }

                    if (!shouldInclude) {
                        continue;
                    }
                }

                if (rules.containsKey("transform")) {
                    com.alibaba.fastjson.JSONObject transform = rules.getJSONObject("transform");
                    for (String key : transform.keySet()) {
                        String value = processed.get(key);
                        if (value != null) {
                            String transformRule = String.valueOf(transform.get(key));
                            processed.put(key, transformRule.replace("{value}", value));
                        }
                    }
                }

                result.add(processed);
            }

            return result;

        } catch (Exception e) {
            log.error("应用加工规则失败", e);
            return data;
        }
    }

    // ========== 加工数据查询 ==========

    /**
     * 根据加工任务ID查询加工数据
     */
    public List<ProcessedData> findProcessedDataByProcessId(Long processId) {
        return processedDataRepository.findByProcessId(processId);
    }

    /**
     * 根据采集来源ID查询加工数据
     */
    public List<ProcessedData> findProcessedDataByCollectId(Long collectId) {
        return processedDataRepository.findByCollectId(collectId);
    }

    /**
     * 分页查询加工数据
     */
    public Page<ProcessedData> findProcessedDataPage(Long processId, int page, int size) {
        return processedDataRepository.findByProcessId(processId, org.springframework.data.domain.PageRequest.of(page, size));
    }

    /**
     * 搜索加工数据
     */
    public List<ProcessedData> searchProcessedData(String keyword) {
        return processedDataRepository.searchByKeyword(keyword);
    }
}
