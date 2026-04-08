package rpa.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.distributed.DistributedLockService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式锁控制器
 * 
 * 提供分布式锁的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/distributed-lock")
@RequiredArgsConstructor
@CrossOrigin
public class DistributedLockController {

    private final DistributedLockService lockService;

    /**
     * 获取锁
     */
    @PostMapping("/acquire")
    public Map<String, Object> acquireLock(@RequestBody AcquireLockRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int ttl = request.getTtl() != null ? request.getTtl() : 30;
            int waitTime = request.getWaitTime() != null ? request.getWaitTime() : 0;
            
            DistributedLockService.LockResult result = lockService.tryLock(
                request.getLockName(),
                request.getHolder(),
                ttl,
                waitTime
            );
            
            response.put("code", result.isSuccess() ? 0 : -1);
            response.put("data", convertLockResult(result));
            if (!result.isSuccess()) {
                response.put("message", result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("获取锁失败", e);
            response.put("code", 500);
            response.put("message", "获取锁失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 释放锁
     */
    @PostMapping("/release")
    public Map<String, Object> releaseLock(@RequestBody ReleaseLockRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean released = lockService.releaseLock(request.getLockName(), request.getHolder());
            response.put("code", released ? 0 : -1);
            response.put("message", released ? "锁已释放" : "释放失败");
        } catch (Exception e) {
            log.error("释放锁失败", e);
            response.put("code", 500);
            response.put("message", "释放锁失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 续期锁
     */
    @PostMapping("/renew")
    public Map<String, Object> renewLock(@RequestBody RenewLockRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int ttl = request.getTtl() != null ? request.getTtl() : 30;
            boolean renewed = lockService.renewLock(request.getLockName(), request.getHolder(), ttl);
            response.put("code", renewed ? 0 : -1);
            response.put("message", renewed ? "续期成功" : "续期失败");
        } catch (Exception e) {
            log.error("续期锁失败", e);
            response.put("code", 500);
            response.put("message", "续期失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 查询锁状态
     */
    @GetMapping("/status/{lockName}")
    public Map<String, Object> getLockStatus(@PathVariable String lockName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DistributedLockService.LockInfo info = lockService.getLockInfo(lockName);
            response.put("code", 0);
            response.put("data", convertLockInfo(info));
        } catch (Exception e) {
            log.error("查询锁状态失败", e);
            response.put("code", 500);
            response.put("message", "查询失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取所有活跃锁
     */
    @GetMapping("/active")
    public Map<String, Object> getActiveLocks() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<DistributedLockService.LockInfo> locks = lockService.getActiveLocks();
            response.put("code", 0);
            response.put("data", locks.stream().map(this::convertLockInfo).toList());
        } catch (Exception e) {
            log.error("获取活跃锁失败", e);
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 获取锁统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getLockStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DistributedLockService.LockStats stats = lockService.getStats();
            response.put("code", 0);
            response.put("data", convertLockStats(stats));
        } catch (Exception e) {
            log.error("获取锁统计失败", e);
            response.put("code", 500);
            response.put("message", "获取失败: " + e.getMessage());
        }
        
        return response;
    }

    // ==================== 转换方法 ====================

    private Map<String, Object> convertLockResult(DistributedLockService.LockResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("lockName", result.getLockName());
        map.put("holder", result.getHolder());
        map.put("success", result.isSuccess());
        map.put("acquireTime", result.getAcquireTime());
        map.put("expireTime", result.getExpireTime());
        map.put("ttl", result.getTtl());
        map.put("errorMessage", result.getErrorMessage());
        return map;
    }

    private Map<String, Object> convertLockInfo(DistributedLockService.LockInfo info) {
        if (info == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("lockName", info.getLockName());
        map.put("holder", info.getHolder());
        map.put("type", info.getType());
        map.put("status", info.getHolder() != null ? "ACQUIRED" : "RELEASED");
        map.put("acquireTime", info.getAcquireTime());
        map.put("expireTime", info.getExpireTime());
        map.put("ttl", info.getTtl());
        map.put("acquireCount", info.getAcquireCount());
        return map;
    }

    private Map<String, Object> convertLockStats(DistributedLockService.LockStats stats) {
        Map<String, Object> map = new HashMap<>();
        map.put("acquireSuccess", stats.getAcquireSuccess());
        map.put("acquireFailed", stats.getAcquireFailed());
        map.put("avgWaitTime", stats.getAvgWaitTime());
        map.put("contentionCount", stats.getContentionCount());
        return map;
    }

    // ==================== 请求类 ====================

    @Data
    public static class AcquireLockRequest {
        private String lockName;
        private String holder;
        private Integer ttl;      // TTL秒数
        private Integer waitTime; // 等待超时秒数
    }

    @Data
    public static class ReleaseLockRequest {
        private String lockName;
        private String holder;
    }

    @Data
    public static class RenewLockRequest {
        private String lockName;
        private String holder;
        private Integer ttl;
    }
}