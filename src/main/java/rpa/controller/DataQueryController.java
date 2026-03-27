package rpa.controller;

/**
 * 数据查询控制器
 * <p>
 * 提供数据查询相关的RESTful API接口，包括：
 * <ul>
 *   <li>查询配置管理：CRUD操作</li>
 *   <li>执行查询：根据配置执行数据查询</li>
 *   <li>采集数据查询：查询已采集的数据</li>
 *   <li>加工数据查询：查询加工后的数据，支持多维度查询</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import rpa.entity.DataQuery;
import rpa.entity.CollectedData;
import rpa.entity.ProcessedData;
import rpa.service.DataQueryService;
import rpa.service.DataProcessService;
import rpa.repository.CollectedDataRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dataQuery")
@RequiredArgsConstructor
@CrossOrigin
public class DataQueryController {

    private final DataQueryService service;
    private final DataProcessService dataProcessService;
    private final CollectedDataRepository collectedDataRepository;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> response = new HashMap<>();
        List<DataQuery> list = service.findAll();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        service.findById(id).ifPresentOrElse(
                query -> {
                    response.put("code", 0);
                    response.put("data", query);
                },
                () -> {
                    response.put("code", -1);
                    response.put("message", "查询配置不存在");
                }
        );
        return response;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataQuery query = service.create(request);
            response.put("code", 0);
            response.put("message", "创建成功");
            response.put("data", query);
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
            DataQuery query = service.update(id, request);
            response.put("code", 0);
            response.put("message", "更新成功");
            response.put("data", query);
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
            Map<String, Object> result = service.executeQuery(id);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/simple")
    public Map<String, Object> simpleQuery(@RequestParam(required = false) String keyword) {
        return service.simpleQuery(keyword);
    }

    @GetMapping("/collectedData")
    public Map<String, Object> listCollectedData() {
        Map<String, Object> response = new HashMap<>();
        List<CollectedData> list = collectedDataRepository.findAll();
        response.put("code", 0);
        response.put("data", list);
        return response;
    }

    // ========== 加工数据查询 ==========

    @GetMapping("/processedData")
    public Map<String, Object> listProcessedData(@RequestParam(required = false) Long processId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProcessedData> list;
            if (processId != null) {
                list = dataProcessService.findProcessedDataByProcessId(processId);
            } else {
                // 返回所有加工数据
                list = dataProcessService.findAll().stream()
                    .flatMap(p -> dataProcessService.findProcessedDataByProcessId(p.getId()).stream())
                    .collect(java.util.stream.Collectors.toList());
            }
            response.put("code", 0);
            response.put("data", list);
            response.put("count", list.size());
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/processedData/{id}")
    public Map<String, Object> getProcessedData(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProcessedData> list = dataProcessService.findAll().stream()
                .flatMap(p -> dataProcessService.findProcessedDataByProcessId(p.getId()).stream())
                .filter(d -> d.getId().equals(id))
                .collect(java.util.stream.Collectors.toList());

            if (!list.isEmpty()) {
                response.put("code", 0);
                response.put("data", list.get(0));
            } else {
                response.put("code", -1);
                response.put("message", "数据不存在");
            }
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/processedData/search")
    public Map<String, Object> searchProcessedData(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProcessedData> list = dataProcessService.searchProcessedData(keyword);
            response.put("code", 0);
            response.put("data", list);
            response.put("count", list.size());
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/processedData/byProcess/{processId}")
    public Map<String, Object> getProcessedDataByProcess(@PathVariable Long processId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProcessedData> list = dataProcessService.findProcessedDataByProcessId(processId);
            response.put("code", 0);
            response.put("data", list);
            response.put("count", list.size());
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/processedData/byCollect/{collectId}")
    public Map<String, Object> getProcessedDataByCollect(@PathVariable Long collectId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProcessedData> list = dataProcessService.findProcessedDataByCollectId(collectId);
            response.put("code", 0);
            response.put("data", list);
            response.put("count", list.size());
        } catch (Exception e) {
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
