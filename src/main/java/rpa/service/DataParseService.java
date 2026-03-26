package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpa.entity.DataParse;
import rpa.entity.CollectedData;
import rpa.repository.DataParseRepository;
import rpa.repository.CollectedDataRepository;
import com.alibaba.fastjson.JSON;
import java.util.*;
import java.util.regex.*;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataParseService {

    private final DataParseRepository repository;
    private final CollectedDataRepository collectedDataRepository;

    public List<DataParse> findAll() {
        return repository.findAll();
    }

    public Optional<DataParse> findById(Long id) {
        return repository.findById(id);
    }

    public DataParse create(Map<String, Object> request) {
        DataParse parse = new DataParse();
        parse.setName((String) request.get("name"));
        parse.setCollectId((String) request.get("collectId"));
        parse.setParseType((String) request.getOrDefault("parseType", "json"));
        parse.setParseRules((String) request.get("parseRules"));
        parse.setOutputFormat((String) request.getOrDefault("outputFormat", "json"));
        parse.setStatus(0);
        parse.setSuccessCount(0);
        parse.setFailCount(0);
        parse.setCreateTime(LocalDateTime.now());
        parse.setUpdateTime(LocalDateTime.now());
        return repository.save(parse);
    }

    public DataParse update(Long id, Map<String, Object> request) {
        return repository.findById(id).map(parse -> {
            if (request.containsKey("name")) parse.setName((String) request.get("name"));
            if (request.containsKey("collectId")) parse.setCollectId((String) request.get("collectId"));
            if (request.containsKey("parseType")) parse.setParseType((String) request.get("parseType"));
            if (request.containsKey("parseRules")) parse.setParseRules((String) request.get("parseRules"));
            if (request.containsKey("outputFormat")) parse.setOutputFormat((String) request.get("outputFormat"));
            if (request.containsKey("status")) parse.setStatus((Integer) request.get("status"));
            parse.setUpdateTime(LocalDateTime.now());
            return repository.save(parse);
        }).orElseThrow(() -> new RuntimeException("解析配置不存在"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Map<String, Object> executeParse(Long id) {
        return repository.findById(id).map(parse -> {
            Map<String, Object> result = doParse(parse);
            if ((boolean) result.getOrDefault("success", false)) {
                parse.setStatus(1);
                parse.setLastParseTime(System.currentTimeMillis());
            } else {
                parse.setStatus(2);
            }
            repository.save(parse);
            return result;
        }).orElseThrow(() -> new RuntimeException("解析配置不存在"));
    }

    private Map<String, Object> doParse(DataParse parse) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        try {
            log.info("开始解析数据: {}", parse.getName());

            List<CollectedData> rawDataList;
            if (parse.getCollectId() != null && !parse.getCollectId().isEmpty()) {
                rawDataList = collectedDataRepository.findByCollectId(Long.parseLong(parse.getCollectId()));
            } else {
                rawDataList = collectedDataRepository.findAll();
            }

            List<Map<String, String>> parsedResults = new ArrayList<>();

            for (CollectedData rawData : rawDataList) {
                try {
                    Map<String, String> parsed = parseRawData(rawData.getRawData(), parse.getParseRules());
                    parsed.put("id", String.valueOf(rawData.getId()));
                    parsedResults.add(parsed);

                    rawData.setParsedData(JSON.toJSONString(parsed));
                    rawData.setParseStatus(1);
                    rawData.setParseTime(LocalDateTime.now());
                    collectedDataRepository.save(rawData);
                    successCount++;

                } catch (Exception e) {
                    log.error("解析单条数据失败: {}", rawData.getId(), e);
                    rawData.setParseStatus(2);
                    rawData.setParseTime(LocalDateTime.now());
                    collectedDataRepository.save(rawData);
                    failCount++;
                }
            }

            parse.setSuccessCount(parse.getSuccessCount() + successCount);
            parse.setFailCount(parse.getFailCount() + failCount);

            result.put("success", true);
            result.put("message", "解析完成");
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("data", parsedResults);

            log.info("解析完成: {}, 成功: {}, 失败: {}", parse.getName(), successCount, failCount);

        } catch (Exception e) {
            log.error("解析失败: {}", parse.getName(), e);
            result.put("success", false);
            result.put("message", "解析失败: " + e.getMessage());
        }

        return result;
    }

    private Map<String, String> parseRawData(String rawJson, String parseRules) {
        Map<String, String> result = new HashMap<>();

        try {
            com.alibaba.fastjson.JSONObject raw = com.alibaba.fastjson.JSON.parseObject(rawJson);

            if (parseRules == null || parseRules.isEmpty()) {
                for (String key : raw.keySet()) {
                    result.put(key, raw.getString(key));
                }
                return result;
            }

            com.alibaba.fastjson.JSONObject rules = com.alibaba.fastjson.JSON.parseObject(parseRules);

            for (Map.Entry<String, Object> entry : rules.entrySet()) {
                String fieldName = entry.getKey();
                String rule = entry.getValue().toString();

                if (raw.containsKey(fieldName)) {
                    String value = raw.getString(fieldName);

                    if (rule.startsWith("regex:")) {
                        String pattern = rule.substring(6);
                        value = extractByRegex(value, pattern);
                    } else if (rule.startsWith("trim")) {
                        value = value.trim();
                    } else if (rule.startsWith("upper")) {
                        value = value.toUpperCase();
                    } else if (rule.startsWith("lower")) {
                        value = value.toLowerCase();
                    } else if (rule.startsWith("replace:")) {
                        String[] parts = rule.substring(8).split(",");
                        if (parts.length == 2) {
                            value = value.replace(parts[0], parts[1]);
                        }
                    }

                    result.put(fieldName, value);
                }
            }

            for (String key : raw.keySet()) {
                if (!result.containsKey(key)) {
                    result.put(key, raw.getString(key));
                }
            }

        } catch (Exception e) {
            log.error("解析数据失败", e);
        }

        return result;
    }

    private String extractByRegex(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1) != null ? m.group(1) : m.group(0);
            }
        } catch (Exception e) {
            log.error("正则提取失败: {}", pattern, e);
        }
        return text;
    }

    public Map<String, Object> batchParse(List<Long> dataIds, String parseRules) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> parsedResults = new ArrayList<>();

        for (Long id : dataIds) {
            collectedDataRepository.findById(id).ifPresent(data -> {
                Map<String, String> parsed = parseRawData(data.getRawData(), parseRules);
                parsedResults.add(parsed);
            });
        }

        result.put("success", true);
        result.put("data", parsedResults);
        result.put("count", parsedResults.size());

        return result;
    }
}
