package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.DataQuery;
import rpa.entity.CollectedData;
import rpa.repository.DataQueryRepository;
import rpa.repository.CollectedDataRepository;
import com.alibaba.fastjson.JSON;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * 数据查询服务类
 * <p>
 * 提供数据查询相关的业务逻辑处理，包括查询配置CRUD和执行查询任务。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQueryService {

    private final DataQueryRepository repository;
    private final CollectedDataRepository collectedDataRepository;

    /**
     * 查询所有查询配置
     */
    public List<DataQuery> findAll() {
        return repository.findAll();
    }

    /**
     * 根据ID查询查询配置
     */
    public Optional<DataQuery> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建查询配置
     *
     * @param request 配置信息
     * @return 创建的配置
     */
    public DataQuery create(Map<String, Object> request) {
        DataQuery query = new DataQuery();
        query.setName((String) request.get("name"));
        query.setSourceTable((String) request.getOrDefault("sourceTable", "collected_data"));
        query.setQueryCondition((String) request.get("queryCondition"));
        query.setQueryColumns((String) request.get("queryColumns"));
        query.setResultCount(0);
        query.setStatus(0);
        query.setCreateTime(LocalDateTime.now());
        query.setUpdateTime(LocalDateTime.now());
        return repository.save(query);
    }

    /**
     * 更新查询配置
     *
     * @param id 配置ID
     * @param request 更新信息
     * @return 更新后的配置
     */
    public DataQuery update(Long id, Map<String, Object> request) {
        return repository.findById(id).map(query -> {
            if (request.containsKey("name")) query.setName((String) request.get("name"));
            if (request.containsKey("sourceTable")) query.setSourceTable((String) request.get("sourceTable"));
            if (request.containsKey("queryCondition")) query.setQueryCondition((String) request.get("queryCondition"));
            if (request.containsKey("queryColumns")) query.setQueryColumns((String) request.get("queryColumns"));
            if (request.containsKey("status")) query.setStatus((Integer) request.get("status"));
            query.setUpdateTime(LocalDateTime.now());
            return repository.save(query);
        }).orElseThrow(() -> new RuntimeException("查询配置不存在"));
    }

    /**
     * 删除查询配置
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * 执行查询任务
     *
     * @param id 配置ID
     * @return 执行结果
     */
    public Map<String, Object> executeQuery(Long id) {
        return repository.findById(id).map(query -> {
            Map<String, Object> result = doQuery(query);
            if ((boolean) result.getOrDefault("success", false)) {
                query.setStatus(1);
                query.setLastQueryTime(System.currentTimeMillis());
            } else {
                query.setStatus(2);
            }
            repository.save(query);
            return result;
        }).orElseThrow(() -> new RuntimeException("查询配置不存在"));
    }

    /**
     * 执行查询逻辑
     */
    private Map<String, Object> doQuery(DataQuery query) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("执行数据查询: {}", query.getName());

            List<CollectedData> allData = collectedDataRepository.findAll();
            List<Map<String, String>> filteredData = new ArrayList<>();

            for (CollectedData data : allData) {
                Map<String, String> item = new HashMap<>();
                item.put("id", String.valueOf(data.getId()));
                item.put("collectName", data.getCollectName());
                item.put("dataType", data.getDataType());
                item.put("sourceUrl", data.getSourceUrl());

                if (data.getParsedData() != null && !data.getParsedData().isEmpty()) {
                    try {
                        com.alibaba.fastjson.JSONObject parsed = com.alibaba.fastjson.JSON.parseObject(data.getParsedData());
                        for (String key : parsed.keySet()) {
                            item.put(key, parsed.getString(key));
                        }
                    } catch (Exception e) {
                        item.put("data", data.getParsedData());
                    }
                } else if (data.getRawData() != null) {
                    try {
                        com.alibaba.fastjson.JSONObject raw = com.alibaba.fastjson.JSON.parseObject(data.getRawData());
                        for (String key : raw.keySet()) {
                            item.put(key, raw.getString(key));
                        }
                    } catch (Exception e) {
                        item.put("data", data.getRawData());
                    }
                }

                if (matchesConditions(item, query.getQueryCondition())) {
                    filteredData.add(item);
                }
            }

            if (query.getQueryColumns() != null && !query.getQueryColumns().isEmpty()) {
                filteredData = selectColumns(filteredData, query.getQueryColumns());
            }

            query.setResultData(JSON.toJSONString(filteredData));
            query.setResultCount(filteredData.size());

            result.put("success", true);
            result.put("message", "查询完成");
            result.put("count", filteredData.size());
            result.put("data", filteredData);

            log.info("查询完成: {}, 结果数: {}", query.getName(), filteredData.size());

        } catch (Exception e) {
            log.error("查询失败: {}", query.getName(), e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查数据是否满足查询条件
     *
     * @param item 数据项
     * @param conditionJson 查询条件（JSON格式）
     * @return boolean 是否匹配
     */
    private boolean matchesConditions(Map<String, String> item, String conditionJson) {
        if (conditionJson == null || conditionJson.isEmpty()) {
            return true;
        }

        try {
            com.alibaba.fastjson.JSONObject conditions = com.alibaba.fastjson.JSON.parseObject(conditionJson);

            for (String key : conditions.keySet()) {
                String expected = conditions.getString(key);
                String actual = item.get(key);

                if (actual == null || !actual.contains(expected)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("解析查询条件失败: {}", conditionJson, e);
            return true;
        }
    }

    /**
     * 选择指定的列
     *
     * @param data 数据列表
     * @param columns 列名（逗号分隔）
     * @return 筛选后的数据
     */
    private List<Map<String, String>> selectColumns(List<Map<String, String>> data, String columns) {
        List<String> columnList = Arrays.asList(columns.split(","));

        return data.stream().map(item -> {
            Map<String, String> filtered = new LinkedHashMap<>();
            for (String col : columnList) {
                String trimmed = col.trim();
                if (item.containsKey(trimmed)) {
                    filtered.put(trimmed, item.get(trimmed));
                }
            }
            return filtered;
        }).collect(Collectors.toList());
    }

    /**
     * 简单关键字查询
     *
     * @param keyword 搜索关键字
     * @return 查询结果
     */
    public Map<String, Object> simpleQuery(String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<CollectedData> allData = collectedDataRepository.findAll();
            List<Map<String, String>> matchedData = new ArrayList<>();

            for (CollectedData data : allData) {
                boolean match = false;
                String searchText = (data.getRawData() != null ? data.getRawData() : "") +
                                   (data.getParsedData() != null ? data.getParsedData() : "");

                if (keyword != null && !keyword.isEmpty() && searchText.contains(keyword)) {
                    match = true;
                }

                if (match || (keyword == null || keyword.isEmpty())) {
                    Map<String, String> item = new HashMap<>();
                    item.put("id", String.valueOf(data.getId()));
                    item.put("collectName", data.getCollectName());
                    item.put("collectTime", data.getCollectTime().toString());

                    if (data.getParsedData() != null) {
                        item.put("data", data.getParsedData());
                    } else {
                        item.put("data", data.getRawData());
                    }

                    matchedData.add(item);
                }
            }

            result.put("success", true);
            result.put("data", matchedData);
            result.put("count", matchedData.size());

        } catch (Exception e) {
            log.error("简单查询失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }
}
