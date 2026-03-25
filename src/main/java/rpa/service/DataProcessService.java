package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.DataProcess;
import rpa.entity.CollectedData;
import rpa.repository.DataProcessRepository;
import rpa.repository.CollectedDataRepository;
import com.alibaba.fastjson.JSON;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataProcessService {

    private final DataProcessRepository repository;
    private final CollectedDataRepository collectedDataRepository;

    public List<DataProcess> findAll() {
        return repository.findAll();
    }

    public Optional<DataProcess> findById(Long id) {
        return repository.findById(id);
    }

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

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Map<String, Object> executeProcess(Long id) {
        return repository.findById(id).map(process -> {
            Map<String, Object> result = doProcess(process);
            if ((boolean) result.getOrDefault("success", false)) {
                process.setStatus(1);
                process.setLastProcessTime(System.currentTimeMillis());
            } else {
                process.setStatus(2);
            }
            repository.save(process);
            return result;
        }).orElseThrow(() -> new RuntimeException("加工配置不存在"));
    }

    private Map<String, Object> doProcess(DataProcess process) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始数据加工: {}", process.getName());

            List<CollectedData> dataList = getSourceData(process.getSourceIds());
            List<Map<String, String>> processedData = new ArrayList<>();

            for (CollectedData data : dataList) {
                Map<String, String> item = new HashMap<>();

                if (data.getParsedData() != null && !data.getParsedData().isEmpty()) {
                    com.alibaba.fastjson.JSONObject parsed = com.alibaba.fastjson.JSON.parseObject(data.getParsedData());
                    for (String key : parsed.keySet()) {
                        item.put(key, parsed.getString(key));
                    }
                } else if (data.getRawData() != null) {
                    com.alibaba.fastjson.JSONObject raw = com.alibaba.fastjson.JSON.parseObject(data.getRawData());
                    for (String key : raw.keySet()) {
                        item.put(key, raw.getString(key));
                    }
                }

                item.put("_id", String.valueOf(data.getId()));
                item.put("_collectTime", data.getCollectTime().toString());
                processedData.add(item);
            }

            processedData = applyProcessRules(processedData, process.getProcessRules());

            process.setProcessedCount(process.getProcessedCount() + processedData.size());

            result.put("success", true);
            result.put("message", "加工完成");
            result.put("count", processedData.size());
            result.put("data", processedData);

            log.info("数据加工完成: {}, 处理数量: {}", process.getName(), processedData.size());

        } catch (Exception e) {
            log.error("数据加工失败: {}", process.getName(), e);
            result.put("success", false);
            result.put("message", "加工失败: " + e.getMessage());
        }

        return result;
    }

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

    private List<Map<String, String>> applyProcessRules(List<Map<String, String>> data, String rulesJson) {
        if (rulesJson == null || rulesJson.isEmpty()) {
            return data;
        }

        try {
            com.alibaba.fastjson.JSONObject rules = com.alibaba.fastjson.JSON.parseObject(rulesJson);
            List<Map<String, String>> result = new ArrayList<>();

            for (Map<String, String> item : data) {
                Map<String, String> processed = new HashMap<>(item);

                if (rules.containsKey("filter")) {
                    com.alibaba.fastjson.JSONObject filter = rules.getJSONObject("filter");
                    boolean shouldInclude = true;

                    for (String key : filter.keySet()) {
                        String expected = filter.getString(key);
                        String actual = processed.get(key);
                        if (!expected.equals(actual)) {
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
                            processed.put(key, transform.getString(key).replace("{value}", value));
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
}
