package rpa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import rpa.entity.ProcessVersion;
import rpa.entity.ApprovalFlow;
import rpa.repository.ProcessVersionRepository;
import rpa.repository.ApprovalFlowRepository;
import rpa.repository.RpaProcessRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/process-version")
public class ProcessVersionController {

    @Autowired
    private ProcessVersionRepository processVersionRepository;

    @Autowired
    private ApprovalFlowRepository approvalFlowRepository;

    @Autowired
    private RpaProcessRepository rpaProcessRepository;

    @GetMapping
    public Map<String, Object> getVersions(
            @RequestParam(required = false) Long processId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ProcessVersion> versions;

        if (processId != null) {
            versions = processVersionRepository.findByProcessIdOrderByCreateTimeDesc(processId);
        } else {
            versions = processVersionRepository.findAll();
        }

        return success(versions);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getVersion(@PathVariable Long id) {
        return processVersionRepository.findById(id)
            .map(this::success)
            .orElse(error("版本不存在"));
    }

    @GetMapping("/process/{processId}")
    public Map<String, Object> getVersionsByProcess(@PathVariable Long processId) {
        List<ProcessVersion> versions = processVersionRepository.findByProcessIdOrderByCreateTimeDesc(processId);
        return success(versions);
    }

    @GetMapping("/process/{processId}/latest")
    public Map<String, Object> getLatestVersion(@PathVariable Long processId) {
        return processVersionRepository.findTopByProcessIdOrderByVersionDesc(processId)
            .map(this::success)
            .orElse(error("版本不存在"));
    }

    @GetMapping("/process/{processId}/published")
    public Map<String, Object> getPublishedVersions(@PathVariable Long processId) {
        List<ProcessVersion> versions = processVersionRepository.findPublishedVersions(processId);
        return success(versions);
    }

    @GetMapping("/gray/{robotId}")
    public Map<String, Object> getGrayVersions(@PathVariable Long robotId) {
        List<ProcessVersion> versions = processVersionRepository.findByGrayRobotIdsContaining(robotId);
        return success(versions);
    }

    @PostMapping
    public Map<String, Object> createVersion(@RequestBody ProcessVersion version) {
        try {
            version.setCreateTime(LocalDateTime.now());
            version.setUpdateTime(LocalDateTime.now());
            if (version.getStatus() == null) {
                version.setStatus("draft");
            }
            ProcessVersion saved = processVersionRepository.save(version);
            return success(saved);
        } catch (Exception e) {
            log.error("创建版本失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateVersion(@PathVariable Long id, @RequestBody ProcessVersion version) {
        try {
            version.setId(id);
            version.setUpdateTime(LocalDateTime.now());
            ProcessVersion saved = processVersionRepository.save(version);
            return success(saved);
        } catch (Exception e) {
            log.error("更新版本失败", e);
            return error("更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    public Map<String, Object> publishVersion(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            ProcessVersion version = processVersionRepository.findById(id).orElse(null);
            if (version == null) {
                return error("版本不存在");
            }

            // 检查是否需要审批
            if (params.containsKey("needApproval") && (Boolean) params.get("needApproval")) {
                // 创建审批流程
                ApprovalFlow approval = new ApprovalFlow();
                approval.setName("流程发布审批 - " + version.getProcessId());
                approval.setCode("APPROVAL_" + System.currentTimeMillis());
                approval.setFlowType("process_publish");
                approval.setTargetType("process_version");
                approval.setTargetId(id);
                approval.setTargetName("v" + version.getVersion());
                approval.setStatus("pending");
                approval.setCreateTime(LocalDateTime.now());
                approvalFlowRepository.save(approval);

                version.setStatus("pending_approval");
                version.setApprovalFlowId(approval.getId());
            } else {
                // 直接发布
                version.setStatus("published");
                version.setPublishedAt(LocalDateTime.now());
                version.setPublishedBy((String) params.getOrDefault("publishedBy", "system"));
            }

            version.setUpdateTime(LocalDateTime.now());
            processVersionRepository.save(version);
            return success(version);
        } catch (Exception e) {
            log.error("发布版本失败", e);
            return error("发布失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/rollback")
    public Map<String, Object> rollbackVersion(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            ProcessVersion version = processVersionRepository.findById(id).orElse(null);
            if (version == null) {
                return error("版本不存在");
            }

            String targetVersion = (String) params.get("targetVersion");
            ProcessVersion target = processVersionRepository.findByProcessIdAndVersion(version.getProcessId(), targetVersion).orElse(null);

            if (target == null) {
                return error("回滚目标版本不存在");
            }

            // 将当前版本标记为废弃
            version.setStatus("deprecated");
            version.setUpdateTime(LocalDateTime.now());
            processVersionRepository.save(version);

            // 发布目标版本
            target.setStatus("published");
            target.setPublishedAt(LocalDateTime.now());
            target.setRollbackFromVersion(version.getVersion());
            target.setUpdateTime(LocalDateTime.now());
            processVersionRepository.save(target);

            return success(target);
        } catch (Exception e) {
            log.error("回滚版本失败", e);
            return error("回滚失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteVersion(@PathVariable Long id) {
        try {
            processVersionRepository.deleteById(id);
            return success("删除成功");
        } catch (Exception e) {
            log.error("删除版本失败", e);
            return error("删除失败: " + e.getMessage());
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
