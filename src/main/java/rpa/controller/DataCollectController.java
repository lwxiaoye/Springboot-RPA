package rpa.controller;

/**
 * 数据采集控制器
 * <p>
 * 提供数据采集相关的RESTful API接口，包括：
 * <ul>
 *   <li>采集配置管理：CRUD操作</li>
 *   <li>执行采集：触发采集任务执行</li>
 *   <li>采集数据查询：查看已采集的数据</li>
 *   <li>页面预览：预览目标网页内容</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.DataCollect;
import rpa.entity.CollectedData;
import rpa.service.DataCollectService;
import rpa.service.WebScraperService;
import rpa.repository.CollectedDataRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dataCollect")
@RequiredArgsConstructor
@CrossOrigin
public class DataCollectController {

    private final DataCollectService service;
    private final WebScraperService scraperService;
    private final CollectedDataRepository collectedDataRepository;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<DataCollect> list = service.findAll();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        service.findById(id).ifPresentOrElse(
                collect -> {
                    response.put("code", 0);
                    response.put("data", collect);
                },
                () -> {
                    response.put("code", -1);
                    response.put("message", "采集配置不存在");
                }
        );
        return response;
    }

    @GetMapping("/{id}/data")
    public Map<String, Object> getCollectedData(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CollectedData> dataList = collectedDataRepository.findByCollectId(id);
            List<Map<String, Object>> result = dataList.stream().map(data -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", data.getId());
                item.put("collectName", data.getCollectName());
                item.put("rawData", data.getRawData());
                item.put("dataType", data.getDataType());
                item.put("sourceUrl", data.getSourceUrl());
                item.put("collectTime", data.getCollectTime());
                item.put("parseStatus", data.getParseStatus());
                return item;
            }).collect(Collectors.toList());
            response.put("code", 0);
            response.put("data", result);
            response.put("count", result.size());
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataCollect collect = service.create(request);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", collect);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataCollect collect = service.update(id, request);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", collect);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
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

    @PostMapping("/{id}/execute")
    public Map<String, Object> execute(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = service.executeCollect(id);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            String html = scraperService.previewPage(url);
            response.put("code", 0);
            response.put("data", html);
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
