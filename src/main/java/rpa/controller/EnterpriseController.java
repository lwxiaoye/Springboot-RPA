package rpa.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.recording.ScreenRecorder;
import rpa.security.CredentialVault;
import rpa.security.DataMaskingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业级特性控制器
 * 
 * 提供录屏、数据脱敏、凭据管理等企业级功能的API
 */
@Slf4j
@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@CrossOrigin
public class EnterpriseController {

    private final ScreenRecorder screenRecorder;
    private final DataMaskingService dataMaskingService;
    private final CredentialVault credentialVault;

    // ==================== 录屏相关 ====================

    /**
     * 开始录屏
     */
    @PostMapping("/recording/start")
    public Map<String, Object> startRecording(@RequestBody RecordingStartRequest request) {
        Map<String, Object> response = new HashMap<>();
        ScreenRecorder.RecordingRequest recordRequest = new ScreenRecorder.RecordingRequest();
        recordRequest.setRobotId(request.getRobotId());
        recordRequest.setTaskId(request.getTaskId());
        recordRequest.setMonitorIndex(request.getMonitorIndex());
        
        ScreenRecorder.RecordingResult result = screenRecorder.startRecording(recordRequest);
        response.put("code", 0);
        response.put("data", convertRecordingResult(result));
        return response;
    }

    /**
     * 暂停录屏
     */
    @PostMapping("/recording/pause/{sessionId}")
    public Map<String, Object> pauseRecording(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        ScreenRecorder.RecordingResult result = screenRecorder.pauseRecording(sessionId);
        response.put("code", 0);
        response.put("data", convertRecordingResult(result));
        return response;
    }

    /**
     * 恢复录屏
     */
    @PostMapping("/recording/resume/{sessionId}")
    public Map<String, Object> resumeRecording(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        ScreenRecorder.RecordingResult result = screenRecorder.resumeRecording(sessionId);
        response.put("code", 0);
        response.put("data", convertRecordingResult(result));
        return response;
    }

    /**
     * 停止录屏
     */
    @PostMapping("/recording/stop/{sessionId}")
    public Map<String, Object> stopRecording(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        ScreenRecorder.RecordingResult result = screenRecorder.stopRecording(sessionId);
        response.put("code", 0);
        response.put("data", convertRecordingResult(result));
        return response;
    }

    /**
     * 获取录屏状态
     */
    @GetMapping("/recording/status/{sessionId}")
    public Map<String, Object> getRecordingStatus(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        ScreenRecorder.RecordingSession session = screenRecorder.getSessionStatus(sessionId);
        response.put("code", 0);
        response.put("data", convertRecordingSession(session));
        return response;
    }

    /**
     * 获取活跃录屏会话
     */
    @GetMapping("/recording/active")
    public Map<String, Object> getActiveSessions() {
        Map<String, Object> response = new HashMap<>();
        List<ScreenRecorder.RecordingSession> sessions = screenRecorder.getActiveSessions();
        response.put("code", 0);
        response.put("data", sessions.stream().map(this::convertRecordingSession).collect(Collectors.toList()));
        return response;
    }

    /**
     * 录制动作
     */
    @PostMapping("/recording/action")
    public Map<String, Object> recordAction(@RequestBody RecordActionRequest request) {
        Map<String, Object> response = new HashMap<>();
        screenRecorder.recordAction(request.getSessionId(), request.getAction(), request.getMetadata());
        response.put("code", 0);
        response.put("message", "动作已录制");
        return response;
    }

    // ==================== 数据脱敏相关 ====================

    /**
     * 脱敏数据
     */
    @PostMapping("/mask")
    public Map<String, Object> maskData(@RequestBody MaskRequest request) {
        Map<String, Object> response = new HashMap<>();
        DataMaskingService.MaskingType type;
        try {
            type = DataMaskingService.MaskingType.valueOf(request.getType());
        } catch (Exception e) {
            type = DataMaskingService.MaskingType.CUSTOM;
        }
        
        String masked = dataMaskingService.mask(request.getData(), type);
        
        Map<String, Object> result = new HashMap<>();
        result.put("original", request.getData());
        result.put("masked", masked);
        result.put("type", request.getType());
        
        response.put("code", 0);
        response.put("data", result);
        return response;
    }

    /**
     * 批量脱敏
     */
    @PostMapping("/mask/batch")
    public Map<String, Object> maskBatch(@RequestBody BatchMaskRequest request) {
        Map<String, Object> response = new HashMap<>();
        List<String> fields = request.getFields();
        Map<String, DataMaskingService.MaskingType> typeMap = new HashMap<>();
        
        for (String field : fields) {
            try {
                typeMap.put(field, DataMaskingService.MaskingType.valueOf(
                    request.getTypeMap().getOrDefault(field, "CUSTOM")));
            } catch (Exception e) {
                typeMap.put(field, DataMaskingService.MaskingType.CUSTOM);
            }
        }
        
        Map<String, Object> result = dataMaskingService.maskBatch(request.getData(), fields, typeMap);
        response.put("code", 0);
        response.put("data", result);
        return response;
    }

    /**
     * 获取脱敏类型
     */
    @GetMapping("/mask/types")
    public Map<String, Object> getMaskTypes() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> types = dataMaskingService.getSupportedTypes();
        response.put("code", 0);
        response.put("data", types);
        return response;
    }

    // ==================== 凭据管理相关 ====================

    /**
     * 存储凭据
     */
    @PostMapping("/credential/store")
    public Map<String, Object> storeCredential(@RequestBody CredentialStoreRequest request) {
        Map<String, Object> response = new HashMap<>();
        CredentialVault.StoreRequest storeRequest = new CredentialVault.StoreRequest();
        storeRequest.setName(request.getName());
        try {
            storeRequest.setType(CredentialVault.CredentialType.valueOf(request.getType()));
        } catch (Exception e) {
            storeRequest.setType(CredentialVault.CredentialType.OTHER);
        }
        storeRequest.setUsername(request.getUsername());
        storeRequest.setValue(request.getValue());
        storeRequest.setSecret(request.getSecret());
        storeRequest.setUrl(request.getUrl());
        storeRequest.setDescription(request.getDescription());
        storeRequest.setTags(request.getTags());
        storeRequest.setAllowUpdate(request.isAllowUpdate());
        
        CredentialVault.CredentialResult result = credentialVault.storeCredential(storeRequest);
        response.put("code", result.isSuccess() ? 0 : -1);
        response.put("message", result.getMessage());
        return response;
    }

    /**
     * 获取凭据
     */
    @PostMapping("/credential/retrieve")
    public Map<String, Object> retrieveCredential(@RequestBody CredentialRetrieveRequest request) {
        Map<String, Object> response = new HashMap<>();
        CredentialVault.CredentialResult result = credentialVault.retrieveCredential(request.getName(), request.getReason());
        response.put("code", result.isSuccess() ? 0 : -1);
        if (result.isSuccess()) {
            response.put("data", convertCredentialResult(result));
        } else {
            response.put("message", result.getMessage());
        }
        return response;
    }

    /**
     * 获取凭据引用
     */
    @GetMapping("/credential/reference/{name}")
    public Map<String, Object> getCredentialReference(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        String reference = credentialVault.getCredentialReference(name);
        response.put("code", reference != null ? 0 : -1);
        response.put("data", reference);
        return response;
    }

    /**
     * 更新凭据
     */
    @PutMapping("/credential")
    public Map<String, Object> updateCredential(@RequestBody CredentialUpdateRequest request) {
        Map<String, Object> response = new HashMap<>();
        CredentialVault.UpdateRequest updateRequest = new CredentialVault.UpdateRequest();
        updateRequest.setName(request.getName());
        updateRequest.setUsername(request.getUsername());
        updateRequest.setValue(request.getValue());
        updateRequest.setSecret(request.getSecret());
        updateRequest.setUrl(request.getUrl());
        updateRequest.setDescription(request.getDescription());
        
        CredentialVault.CredentialResult result = credentialVault.updateCredential(updateRequest);
        response.put("code", result.isSuccess() ? 0 : -1);
        response.put("message", result.getMessage());
        return response;
    }

    /**
     * 删除凭据
     */
    @DeleteMapping("/credential/{name}")
    public Map<String, Object> deleteCredential(@PathVariable String name,
                                                  @RequestParam(required = false) String reason) {
        Map<String, Object> response = new HashMap<>();
        CredentialVault.CredentialResult result = credentialVault.deleteCredential(name, reason != null ? reason : "用户删除");
        response.put("code", result.isSuccess() ? 0 : -1);
        response.put("message", result.getMessage());
        return response;
    }

    /**
     * 列出凭据
     */
    @GetMapping("/credential/list")
    public Map<String, Object> listCredentials() {
        Map<String, Object> response = new HashMap<>();
        List<CredentialVault.CredentialSummary> summaries = credentialVault.listCredentials();
        response.put("code", 0);
        response.put("data", summaries.stream().map(this::convertCredentialSummary).collect(Collectors.toList()));
        return response;
    }

    /**
     * 搜索凭据
     */
    @GetMapping("/credential/search")
    public Map<String, Object> searchCredentials(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        Map<String, Object> response = new HashMap<>();
        CredentialVault.CredentialType credentialType = null;
        if (type != null && !type.isEmpty()) {
            try {
                credentialType = CredentialVault.CredentialType.valueOf(type);
            } catch (Exception e) {
                // 忽略
            }
        }
        List<CredentialVault.CredentialSummary> results = credentialVault.searchCredentials(keyword, credentialType);
        response.put("code", 0);
        response.put("data", results.stream().map(this::convertCredentialSummary).collect(Collectors.toList()));
        return response;
    }

    /**
     * 获取审计日志
     */
    @GetMapping("/credential/audit")
    public Map<String, Object> getAuditLogs(
            @RequestParam(required = false) String credentialName,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "100") int limit) {
        Map<String, Object> response = new HashMap<>();
        List<CredentialVault.AuditRecord> logs = credentialVault.getAuditLogs(credentialName, startTime, endTime, limit);
        response.put("code", 0);
        response.put("data", logs);
        return response;
    }

    // ==================== 转换方法 ====================

    private Map<String, Object> convertRecordingResult(ScreenRecorder.RecordingResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("sessionId", result.getSessionId());
        map.put("videoPath", result.getVideoPath());
        map.put("duration", result.getDuration());
        map.put("frameCount", result.getFrameCount());
        map.put("message", result.getMessage());
        return map;
    }

    private Map<String, Object> convertRecordingSession(ScreenRecorder.RecordingSession session) {
        if (session == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", session.getSessionId());
        map.put("robotId", session.getRobotId());
        map.put("taskId", session.getTaskId());
        map.put("status", session.getStatus() != null ? session.getStatus().name() : null);
        map.put("startTime", session.getStartTime());
        map.put("endTime", session.getEndTime());
        map.put("frameCount", session.getFrameCount());
        return map;
    }

    private Map<String, Object> convertCredentialResult(CredentialVault.CredentialResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("credentialName", result.getCredentialName());
        map.put("type", result.getType() != null ? result.getType().name() : null);
        map.put("username", result.getUsername());
        map.put("value", result.getValue());
        map.put("secret", result.getSecret());
        map.put("url", result.getUrl());
        return map;
    }

    private Map<String, Object> convertCredentialSummary(CredentialVault.CredentialSummary summary) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", summary.getName());
        map.put("type", summary.getType() != null ? summary.getType().name() : null);
        map.put("username", summary.getUsername());
        map.put("url", summary.getUrl());
        map.put("description", summary.getDescription());
        map.put("tags", summary.getTags());
        map.put("createdAt", summary.getCreatedAt());
        map.put("updatedAt", summary.getUpdatedAt());
        map.put("lastUsedAt", summary.getLastUsedAt());
        map.put("useCount", summary.getUseCount());
        map.put("hasSecret", summary.isHasSecret());
        return map;
    }

    // ==================== 请求类 ====================

    @Data
    public static class RecordingStartRequest {
        private String robotId;
        private String taskId;
        private int monitorIndex = 0;
    }

    @Data
    public static class RecordActionRequest {
        private String sessionId;
        private String action;
        private Map<String, Object> metadata;
    }

    @Data
    public static class MaskRequest {
        private String data;
        private String type;  // ID_CARD, PHONE, BANK_CARD, EMAIL, NAME, ADDRESS, PASSWORD, AMOUNT, CUSTOM
    }

    @Data
    public static class BatchMaskRequest {
        private Map<String, Object> data;
        private List<String> fields;
        private Map<String, String> typeMap;
    }

    @Data
    public static class CredentialStoreRequest {
        private String name;
        private String type;
        private String username;
        private String value;
        private String secret;
        private String url;
        private String description;
        private List<String> tags;
        private boolean allowUpdate;
    }

    @Data
    public static class CredentialRetrieveRequest {
        private String name;
        private String reason;
    }

    @Data
    public static class CredentialUpdateRequest {
        private String name;
        private String username;
        private String value;
        private String secret;
        private String url;
        private String description;
    }
}