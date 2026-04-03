package rpa.service;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务类
 * <p>
 * 提供Redis缓存的统一操作接口，封装常用的缓存操作方法。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>基本缓存操作：set/get/delete/hasKey/expire</li>
 *   <li>分布式锁实现：基于Redis的互斥锁</li>
 *   <li>请求频率控制：基于时间窗口的限流</li>
 *   <li>采集结果缓存：60分钟过期</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis key前缀 */
    @Value("${scraper.redis-key-prefix:rpa:scraper:}")
    private String keyPrefix;

    /** 默认过期时间（30分钟） */
    private static final long DEFAULT_EXPIRE = 30;

    /**
     * 设置缓存（使用默认过期时间30分钟）
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 设置缓存（指定过期时间）
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expireMinutes 过期时间（分钟）
     */
    public void set(String key, Object value, long expireMinutes) {
        try {
            String fullKey = keyPrefix + key;
            redisTemplate.opsForValue().set(fullKey, JSON.toJSONString(value), expireMinutes, TimeUnit.MINUTES);
            log.debug("缓存已设置: {}", fullKey);
        } catch (Exception e) {
            log.error("设置缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 缓存值，不存在则返回null
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String fullKey = keyPrefix + key;
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                return JSON.parseObject(value.toString(), clazz);
            }
        } catch (Exception e) {
            log.error("获取缓存失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        try {
            String fullKey = keyPrefix + key;
            redisTemplate.delete(fullKey);
            log.debug("缓存已删除: {}", fullKey);
        } catch (Exception e) {
            log.error("删除缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return boolean 是否存在
     */
    public boolean hasKey(String key) {
        try {
            String fullKey = keyPrefix + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
        } catch (Exception e) {
            log.error("检查缓存存在失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     *
     * @param key 缓存键
     * @param minutes 过期时间（分钟）
     */
    public void expire(String key, long minutes) {
        try {
            String fullKey = keyPrefix + key;
            redisTemplate.expire(fullKey, minutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("设置缓存过期失败: {}", e.getMessage());
        }
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁
     * <p>
     * 基于Redis的SETNX实现，确保锁的互斥性。
     * </p>
     *
     * @param lockKey 锁键
     * @param requestId 请求ID（用于标识锁持有者）
     * @param expireSeconds 锁过期时间（秒）
     * @return boolean 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireSeconds) {
        try {
            String fullKey = keyPrefix + "lock:" + lockKey;
            Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, requestId, expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("获取分布式锁失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁
     * <p>
     * 只有锁持有者才能释放锁，通过requestId验证。
     * </p>
     *
     * @param lockKey 锁键
     * @param requestId 请求ID
     */
    public void unlock(String lockKey, String requestId) {
        try {
            String fullKey = keyPrefix + "lock:" + lockKey;
            Object currentValue = redisTemplate.opsForValue().get(fullKey);
            if (requestId.equals(currentValue)) {
                redisTemplate.delete(fullKey);
                log.debug("释放分布式锁: {}", fullKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁失败: {}", e.getMessage());
        }
    }

    // ==================== 采集结果缓存 ====================

    /**
     * 缓存采集数据（60分钟过期）
     *
     * @param collectId 采集任务ID
     * @param data 采集数据
     */
    public void cacheScrapedData(String collectId, Object data) {
        set("data:" + collectId, data, 60);
    }

    /**
     * 获取缓存的采集数据
     *
     * @param collectId 采集任务ID
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 采集数据
     */
    public <T> T getScrapedData(String collectId, Class<T> clazz) {
        return get("data:" + collectId, clazz);
    }

    // ==================== 请求频率控制 ====================

    /**
     * 检查是否允许请求（基于时间窗口的限流）
     *
     * @param clientId 客户端ID
     * @param maxRequests 最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return boolean 是否允许
     */
    public boolean isAllowed(String clientId, int maxRequests, int windowSeconds) {
        String key = "rate:" + clientId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            return count != null && count <= maxRequests;
        } catch (Exception e) {
            log.error("频率控制检查失败: {}", e.getMessage());
            return true; // 失败时放行
        }
    }

    /**
     * 增加请求计数
     *
     * @param clientId 客户端ID
     */
    public void incrementRequestCount(String clientId) {
        try {
            String key = "counter:" + clientId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("计数器增加失败: {}", e.getMessage());
        }
    }

    /**
     * 获取请求计数
     *
     * @param clientId 客户端ID
     * @return long 请求次数
     */
    public long getRequestCount(String clientId) {
        try {
            String key = "counter:" + clientId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 通用字符串操作 ====================

    /**
     * 设置原始字符串缓存
     *
     * @param key 缓存键（不含前缀）
     * @param value 字符串值
     * @param expireMinutes 过期时间（分钟）
     */
    public void setRaw(String key, String value, long expireMinutes) {
        try {
            String fullKey = keyPrefix + key;
            redisTemplate.opsForValue().set(fullKey, value, expireMinutes, TimeUnit.MINUTES);
            log.debug("字符串缓存已设置: {}", fullKey);
        } catch (Exception e) {
            log.error("设置字符串缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取原始字符串缓存
     *
     * @param key 缓存键（不含前缀）
     * @return 字符串值，不存在则返回null
     */
    public String getRaw(String key) {
        try {
            String fullKey = keyPrefix + key;
            Object value = redisTemplate.opsForValue().get(fullKey);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("获取字符串缓存失败: {}", e.getMessage());
            return null;
        }
    }
}
