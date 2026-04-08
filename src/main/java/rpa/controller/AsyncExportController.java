package rpa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import rpa.entity.AsyncExportTask;
import rpa.entity.DataLineage;
import rpa.entity.TraceLog;
import rpa.repository.AsyncExportTaskRepository;
import rpa.repository.DataLineageRepository;
import rpa.repository.TraceLogRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/export")
public class AsyncExportController {

    @Autowired
    private AsyncExportTaskRepository exportTaskRepository;

    @GetMapping
    public Map<String, Object> getExportTasks(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AsyncExportTask> tasks;

        if (userId != null) {
            tasks = exportTaskRepository.findByUserId(userId, PageRequest.of(page, size));
        } else if (status != null) {
            tasks = exportTaskRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            tasks = exportTaskRepository.findAll(PageRequest.of(page, size));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", tasks.getContent());
        result.put("total", tasks.getTotalElements());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getExportTask(@PathVariable Long id) {
        return exportTaskRepository.findById(id)
            .map(task -> {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 0);
                result.put("data", task);
                return result;
            })
            .orElse(error("导出任务不存在"));
    }

    @GetMapping("/task-no/{taskNo}")
    public Map<String, Object> getExportTaskByNo(@PathVariable String taskNo) {
        return exportTaskRepository.findByTaskNo(taskNo)
            .map(task -> {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 0);
                result.put("data", task);
                return result;
            })
            .orElse(error("导出任务不存在"));
    }

    @PostMapping
    public Map<String, Object> createExportTask(@RequestBody Map<String, Object> params) {
        try {
            AsyncExportTask task = new AsyncExportTask();
            task.setTaskNo("EXP_" + System.currentTimeMillis());
            task.setExportType((String) params.getOrDefault("exportType", "custom"));
            task.setExportName((String) params.getOrDefault("exportName", "导出任务"));
            task.setQueryParams(toJson(params.get("queryParams")));
            task.setExportFields(toJson(params.get("exportFields")));
            task.setFileFormat((String) params.getOrDefault("fileFormat", "xlsx"));
            task.setUserId(params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null);
            task.setUserName((String) params.get("userName"));
            task.setDepartment((String) params.get("department"));
            task.setIp((String) params.get("ip"));
            task.setStatus("pending");
            task.setProgress(0);
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());

            AsyncExportTask saved = exportTaskRepository.save(task);
            return success(saved);
        } catch (Exception e) {
            log.error("创建导出任务失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        try {
            AsyncExportTask task = exportTaskRepository.findById(id).orElse(null);
            if (task == null) {
                return error("导出任务不存在");
            }

            String status = (String) params.get("status");
            task.setStatus(status);
            task.setUpdateTime(LocalDateTime.now());

            if ("processing".equals(status)) {
                task.setStartAt(LocalDateTime.now());
            } else if ("completed".equals(status)) {
                task.setCompletedAt(LocalDateTime.now());
                task.setProgress(100);
            } else if ("failed".equals(status)) {
                task.setCompletedAt(LocalDateTime.now());
                task.setErrorMessage((String) params.get("errorMessage"));
            }

            if (params.containsKey("progress")) {
                task.setProgress(Integer.valueOf(params.get("progress").toString()));
            }
            if (params.containsKey("totalRecords")) {
                task.setTotalRecords(Integer.valueOf(params.get("totalRecords").toString()));
            }
            if (params.containsKey("exportedRecords")) {
                task.setExportedRecords(Integer.valueOf(params.get("exportedRecords").toString()));
            }
            if (params.containsKey("filePath")) {
                task.setFilePath((String) params.get("filePath"));
            }
            if (params.containsKey("fileSize")) {
                task.setFileSize(Long.valueOf(params.get("fileSize").toString()));
            }
            if (params.containsKey("fileMd5")) {
                task.setFileMd5((String) params.get("fileMd5"));
            }

            exportTaskRepository.save(task);
            return success(task);
        } catch (Exception e) {
            log.error("更新导出任务状态失败", e);
            return error("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserTasks(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AsyncExportTask> tasks = exportTaskRepository.findUserTasks(userId, PageRequest.of(page, size));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", tasks.getContent());
        result.put("total", tasks.getTotalElements());
        return result;
    }

    @GetMapping("/processing/count")
    public Map<String, Object> getProcessingCount() {
        long count = exportTaskRepository.countProcessingTasks();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", count);
        return result;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}

// ==================== 数据血缘控制器 ====================

@Slf4j
@RestController
@RequestMapping("/api/data-lineage")
class DataLineageController {

    @Autowired
    private DataLineageRepository dataLineageRepository;

    @GetMapping
    public Map<String, Object> getLineage(
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Long processId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String businessKey) {
        List<DataLineage> lineage;

        if (traceId != null && !traceId.isEmpty()) {
            lineage = dataLineageRepository.findByTraceId(traceId);
        } else if (processId != null) {
            lineage = dataLineageRepository.findByProcessId(processId);
        } else if (taskId != null) {
            lineage = dataLineageRepository.findByTaskId(taskId);
        } else if (businessKey != null && !businessKey.isEmpty()) {
            lineage = dataLineageRepository.findByBusinessKey(businessKey);
        } else {
            lineage = dataLineageRepository.findAll();
        }

        return success(lineage);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getLineageById(@PathVariable Long id) {
        return dataLineageRepository.findById(id)
            .map(this::success)
            .orElse(error("血缘记录不存在"));
    }

    @GetMapping("/record/{recordId}")
    public Map<String, Object> getLineageByRecord(@PathVariable String recordId) {
        List<DataLineage> lineage = dataLineageRepository.findByDataRecordId(recordId);
        return success(lineage);
    }

    @GetMapping("/tree/{id}")
    public Map<String, Object> getLineageTree(@PathVariable Long id) {
        DataLineage lineage = dataLineageRepository.findById(id).orElse(null);
        if (lineage == null) {
            return error("血缘记录不存在");
        }

        Map<String, Object> tree = new HashMap<>();
        tree.put("id", lineage.getId());
        tree.put("type", lineage.getLineageType());
        tree.put("name", lineage.getSourceName());
        tree.put("processName", lineage.getProcessName());
        tree.put("robotName", lineage.getRobotName());
        tree.put("createTime", lineage.getCreateTime());

        // 获取子节点
        if (lineage.getChildLineageIds() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List childIds = mapper.readValue(lineage.getChildLineageIds(), List.class);
                tree.put("children", childIds);
            } catch (Exception e) {
                log.error("解析子节点失败", e);
            }
        }

        return success(tree);
    }

    @PostMapping
    public Map<String, Object> createLineage(@RequestBody DataLineage lineage) {
        try {
            lineage.setCreateTime(LocalDateTime.now());
            DataLineage saved = dataLineageRepository.save(lineage);
            return success(saved);
        } catch (Exception e) {
            log.error("创建血缘记录失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }
}

// ==================== 全链路追踪控制器 ====================

@Slf4j
@RestController
@RequestMapping("/api/trace")
class TraceController {

    @Autowired
    private TraceLogRepository traceLogRepository;

    @GetMapping
    public Map<String, Object> getTraces(
            @RequestParam String traceId,
            @RequestParam(required = false) String status) {
        List<TraceLog> traces = traceLogRepository.findByTraceId(traceId);
        if (status != null && !status.isEmpty()) {
            traces = traces.stream().filter(t -> status.equals(t.getStatus())).toList();
        }
        return success(traces);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getTraceById(@PathVariable Long id) {
        return traceLogRepository.findById(id)
            .map(this::success)
            .orElse(error("追踪记录不存在"));
    }

    @PostMapping
    public Map<String, Object> createTrace(@RequestBody TraceLog trace) {
        try {
            trace.setCreateTime(LocalDateTime.now());
            if (trace.getTraceId() == null) {
                trace.setTraceId(UUID.randomUUID().toString());
            }
            if (trace.getSpanId() == null) {
                trace.setSpanId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            }
            TraceLog saved = traceLogRepository.save(trace);
            return success(saved);
        } catch (Exception e) {
            log.error("创建追踪记录失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateTrace(@PathVariable Long id, @RequestBody TraceLog trace) {
        try {
            trace.setId(id);
            traceLogRepository.save(trace);
            return success(trace);
        } catch (Exception e) {
            log.error("更新追踪记录失败", e);
            return error("更新失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : LocalDateTime.now().minusDays(1);
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : LocalDateTime.now();

        List<TraceLog> traces = traceLogRepository.findByStartTimeBetween(start, end);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", traces.size());
        stats.put("success", traces.stream().filter(t -> "success".equals(t.getStatus())).count());
        stats.put("failed", traces.stream().filter(t -> "failed".equals(t.getStatus())).count());
        stats.put("running", traces.stream().filter(t -> "running".equals(t.getStatus())).count());

        double avgDuration = traces.stream()
            .filter(t -> t.getDurationMs() != null)
            .mapToLong(TraceLog::getDurationMs)
            .average()
            .orElse(0);
        stats.put("avgDuration", avgDuration);

        return success(stats);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }
}
