package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rpa.entity.DataQuery;
import rpa.entity.CollectedData;
import rpa.service.DataQueryService;
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
}
