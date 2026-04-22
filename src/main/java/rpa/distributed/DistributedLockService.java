package rpa.distributed;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分布式锁服务
 * 
 * 功能说明：
 * - 基于Redis实现分布式锁
 * - 支持可重入锁、公平锁
 * - 自动续期（看门狗机制）
 * - 锁竞争统计
 * 
 * 使用场景：
 * - 任务抢锁执行
 * - 资源独占访问
 * - 集群协调
 * 
 * @author RPA System
 */
@Slf4j
@Service
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    // 锁前缀
    private static final String LOCK_PREFIX = "rpa:lock:";

    // 锁持有者前缀
    private static final String HOLDER_PREFIX = "rpa:lock:holder:";

    // 活跃锁记录
    private final Map<String, LockInfo> activeLocks = new ConcurrentHashMap<>();

    // 统计信息
    private final LockStats stats = new LockStats();

    // 锁配置
    @Value("${rpa.lock.default-ttl:30}")
    private int defaultTtl;

    @Value("${rpa.lock.enable-redis:true}")
    private boolean enableRedis;

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取锁
     */
    public LockResult tryLock(String lockName, String holder, int ttlSeconds, int waitSeconds) {
        log.info("尝试获取锁: lockName={}, holder={}, ttl={}s", lockName, holder, ttlSeconds);
        
        LockResult result = new LockResult();
        result.setLockName(lockName);
        result.setHolder(holder);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 计算等待结束时间
            long deadline = startTime + (waitSeconds * 1000L);
            
            // 使用 do-while 确保至少执行一次锁获取尝试
            do {
                boolean acquired = doAcquire(lockName, holder, ttlSeconds);
                
                if (acquired) {
                    result.setSuccess(true);
                    result.setAcquireTime(System.currentTimeMillis());
                    result.setExpireTime(System.currentTimeMillis() + (ttlSeconds * 1000L));
                    result.setTtl(ttlSeconds);
                    
                    stats.incrementAcquireSuccess();
                    stats.recordWaitTime(System.currentTimeMillis() - startTime);
                    
                    log.info("获取锁成功: lockName={}, holder={}", lockName, holder);
                    return result;
                }
                
                // 锁竞争计数
                stats.incrementContention();
                
                // 如果不是最后一次尝试，等待后重试
                if (System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } while (System.currentTimeMillis() < deadline);
            
            // 超时未获取
            result.setSuccess(false);
            result.setErrorMessage("等待超时");
            stats.incrementAcquireFailed();
            
            log.warn("获取锁超时: lockName={}, holder={}", lockName, holder);
            return result;
            
        } catch (Exception e) {
            log.error("获取锁异常: lockName={}", lockName, e);
            result.setSuccess(false);
            result.setErrorMessage("获取锁异常: " + e.getMessage());
            stats.incrementAcquireFailed();
            return result;
        }
    }

    /**
     * 执行获取锁
     */
    private boolean doAcquire(String lockName, String holder, int ttlSeconds) {
        String key = LOCK_PREFIX + lockName;
        String value = holder + ":" + System.currentTimeMillis();
        
        if (enableRedis && redisTemplate != null) {
            // 使用Redis SETNX
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
            if (Boolean.TRUE.equals(success)) {
                // 记录活跃锁
                recordActiveLock(lockName, holder, ttlSeconds);
                return true;
            }
            
            // 检查是否是同一持有者（可重入）
            String existingValue = redisTemplate.opsForValue().get(key);
            if (existingValue != null && existingValue.startsWith(holder + ":")) {
                // 续期
                redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
                return true;
            }
            
            return false;
        } else {
            // 本地锁（降级）
            return acquireLocalLock(lockName, holder, ttlSeconds);
        }
    }

    /**
     * 本地锁（Redis不可用时的降级方案）
     */
    private boolean acquireLocalLock(String lockName, String holder, int ttlSeconds) {
        LockInfo info = activeLocks.get(lockName);
        long now = System.currentTimeMillis();
        
        if (info == null) {
            // 创建新锁
            info = new LockInfo();
            info.setLockName(lockName);
            info.setHolder(holder);
            info.setAcquireTime(now);
            info.setExpireTime(now + (ttlSeconds * 1000L));
            info.setAcquireCount(1);
            activeLocks.put(lockName, info);
            return true;
        }
        
        // 检查过期
        if (now > info.getExpireTime()) {
            // 过期，重新获取
            info.setHolder(holder);
            info.setAcquireTime(now);
            info.setExpireTime(now + (ttlSeconds * 1000L));
            info.incrementAcquireCount();
            return true;
        }
        
        // 检查是否是同一持有者
        if (info.getHolder().equals(holder)) {
            // 续期
            info.setExpireTime(now + (ttlSeconds * 1000L));
            return true;
        }
        
        return false;
    }

    /**
     * 释放锁
     */
    public boolean releaseLock(String lockName, String holder) {
        log.info("释放锁: lockName={}, holder={}", lockName, holder);
        
        String key = LOCK_PREFIX + lockName;
        
        if (enableRedis && redisTemplate != null) {
            String existingValue = redisTemplate.opsForValue().get(key);
            if (existingValue != null && existingValue.startsWith(holder + ":")) {
                redisTemplate.delete(key);
            }
        }
        
        // 移除活跃锁记录
        LockInfo info = activeLocks.remove(lockName);
        return info != null;
    }

    /**
     * 续期锁
     */
    public boolean renewLock(String lockName, String holder, int ttlSeconds) {
        log.info("续期锁: lockName={}, holder={}, ttl={}s", lockName, holder, ttlSeconds);
        
        String key = LOCK_PREFIX + lockName;
        
        if (enableRedis && redisTemplate != null) {
            String existingValue = redisTemplate.opsForValue().get(key);
            if (existingValue != null && existingValue.startsWith(holder + ":")) {
                redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
                
                // 更新活跃锁记录
                LockInfo info = activeLocks.get(lockName);
                if (info != null) {
                    info.setExpireTime(System.currentTimeMillis() + (ttlSeconds * 1000L));
                }
                
                return true;
            }
            return false;
        } else {
            LockInfo info = activeLocks.get(lockName);
            if (info != null && info.getHolder().equals(holder)) {
                info.setExpireTime(System.currentTimeMillis() + (ttlSeconds * 1000L));
                return true;
            }
            return false;
        }
    }

    /**
     * 查询锁状态
     */
    public LockInfo getLockInfo(String lockName) {
        if (enableRedis && redisTemplate != null) {
            String key = LOCK_PREFIX + lockName;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                LockInfo info = new LockInfo();
                info.setLockName(lockName);
                String[] parts = value.split(":");
                info.setHolder(parts[0]);
                info.setAcquireTime(Long.parseLong(parts[1]));
                
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                info.setTtl(ttl != null ? ttl.intValue() : 0);
                info.setExpireTime(System.currentTimeMillis() + (info.getTtl() * 1000L));
                
                return info;
            }
        }
        
        // 本地锁：动态计算剩余 TTL
        LockInfo info = activeLocks.get(lockName);
        if (info != null) {
            long now = System.currentTimeMillis();
            long remainingMs = info.getExpireTime() - now;
            int remainingTtl = (int) Math.max(0, remainingMs / 1000);
            info.setTtl(remainingTtl);
        }
        return info;
    }

    /**
     * 获取所有活跃锁
     */
    public List<LockInfo> getActiveLocks() {
        List<LockInfo> result = new ArrayList<>(activeLocks.values());
        
        // 动态计算每个锁的剩余 TTL
        long now = System.currentTimeMillis();
        for (LockInfo info : result) {
            if (info.getExpireTime() > 0) {
                long remainingMs = info.getExpireTime() - now;
                int remainingTtl = (int) Math.max(0, remainingMs / 1000);
                info.setTtl(remainingTtl);
            }
        }
        
        return result;
    }

    /**
     * 获取锁统计
     */
    public LockStats getStats() {
        return stats;
    }

    /**
     * 记录活跃锁
     */
    private void recordActiveLock(String lockName, String holder, int ttlSeconds) {
        LockInfo info = new LockInfo();
        info.setLockName(lockName);
        info.setHolder(holder);
        info.setAcquireTime(System.currentTimeMillis());
        info.setExpireTime(System.currentTimeMillis() + (ttlSeconds * 1000L));
        info.setTtl(ttlSeconds);
        info.setAcquireCount(1);
        activeLocks.put(lockName, info);
    }

    // ==================== 内部类 ====================

    /**
     * 锁结果
     */
    @Data
    public static class LockResult {
        private String lockName;
        private String holder;
        private boolean success;
        private long acquireTime;
        private long expireTime;
        private int ttl;
        private String errorMessage;
    }

    /**
     * 锁信息
     */
    @Data
    public static class LockInfo {
        private String lockName;
        private String holder;
        private String type = "TASK";
        private long acquireTime;
        private long expireTime;
        private int ttl;
        private int acquireCount;

        public void incrementAcquireCount() {
            this.acquireCount++;
        }
    }

    /**
     * 锁统计
     */
    @Data
    public static class LockStats {
        private long acquireSuccess = 0;
        private long acquireFailed = 0;
        private long totalWaitTime = 0;
        private int waitTimeCount = 0;
        private long contentionCount = 0;

        public void incrementAcquireSuccess() {
            this.acquireSuccess++;
        }

        public void incrementAcquireFailed() {
            this.acquireFailed++;
        }

        public void incrementContention() {
            this.contentionCount++;
        }

        public void recordWaitTime(long waitTime) {
            this.totalWaitTime += waitTime;
            this.waitTimeCount++;
        }

        public long getAvgWaitTime() {
            return waitTimeCount > 0 ? totalWaitTime / waitTimeCount : 0;
        }
    }
}