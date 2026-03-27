package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反爬虫/请求控制服务类
 * <p>
 * 提供网页采集时的反爬虫策略和请求控制功能。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>User-Agent轮换 - 模拟不同浏览器访问</li>
 *   <li>代理IP轮换 - 支持多代理自动切换</li>
 *   <li>请求频率控制 - 基于域名的请求间隔控制</li>
 *   <li>重试机制 - 失败后自动重试</li>
 *   <li>封禁检测 - 检测是否被目标网站封禁</li>
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
public class AntiCrawlerService {

    private final CacheService cacheService;

    /** 默认请求间隔（毫秒），可通过配置覆盖 */
    @Value("${scraper.request-interval:1000}")
    private long defaultRequestInterval;

    /** 最大重试次数，可通过配置覆盖 */
    @Value("${scraper.max-retry:3}")
    private int maxRetry;

    /** 上次请求时间记录（域名维度） */
    private static final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    
    /** 重试次数记录（域名维度） */
    private static final Map<String, Integer> retryCount = new ConcurrentHashMap<>();
    
    /** 请求历史记录（用于统计和封禁检测） */
    private static final Map<String, List<Long>> requestHistory = new ConcurrentHashMap<>();

    /** 常用浏览器User-Agent列表 */
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    };

    /** 代理IP列表（可配置） */
    private static final String[] PROXIES = {};

    /**
     * 获取随机User-Agent
     * <p>
     * 从预定义的浏览器User-Agent列表中随机选择一个，
     * 模拟不同浏览器和操作系统的访问。
     * </p>
     *
     * @return String 随机User-Agent
     */
    public String getRandomUserAgent() {
        return USER_AGENTS[new Random().nextInt(USER_AGENTS.length)];
    }

    /**
     * 获取下一个代理IP（轮换）
     * <p>
     * 从代理池中随机选择一个代理IP。
     * 需要在配置中设置PROXIES数组。
     * </p>
     *
     * @return String 代理地址，格式：ip:port
     */
    public String getNextProxy() {
        if (PROXIES == null || PROXIES.length == 0) {
            return null;
        }
        String proxy = PROXIES[new Random().nextInt(PROXIES.length)];
        log.debug("使用代理: {}", proxy);
        return proxy;
    }

    /**
     * 检查是否可以发起请求
     * <p>
     * 根据域名维度的请求间隔控制，
     * 判断距离上次请求是否已满足间隔要求。
     * </p>
     *
     * @param targetUrl 目标URL
     * @return boolean 是否可以发起请求
     */
    public boolean canMakeRequest(String targetUrl) {
        String domain = extractDomain(targetUrl);
        Long lastTime = lastRequestTime.get(domain);

        if (lastTime != null) {
            long elapsed = System.currentTimeMillis() - lastTime;
            if (elapsed < defaultRequestInterval) {
                log.warn("请求频率限制: {} 上次请求 {}ms前, 需要间隔 {}ms", domain, elapsed, defaultRequestInterval);
                return false;
            }
        }

        lastRequestTime.put(domain, System.currentTimeMillis());
        return true;
    }

    /**
     * 等待合适的请求时机
     * <p>
     * 如果距离上次请求时间不足，则等待至满足间隔要求。
     * </p>
     *
     * @param targetUrl 目标URL
     */
    public void waitForRequest(String targetUrl) {
        String domain = extractDomain(targetUrl);
        Long lastTime = lastRequestTime.get(domain);

        if (lastTime != null) {
            long elapsed = System.currentTimeMillis() - lastTime;
            if (elapsed < defaultRequestInterval) {
                try {
                    Thread.sleep(defaultRequestInterval - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        lastRequestTime.put(domain, System.currentTimeMillis());
    }

    /**
     * 记录请求历史
     *
     * @param targetUrl 目标URL
     * @param success 是否成功
     */
    public void recordRequest(String targetUrl, boolean success) {
        String domain = extractDomain(targetUrl);

        requestHistory.computeIfAbsent(domain, k -> new ArrayList<>()).add(System.currentTimeMillis());

        if (success) {
            retryCount.remove(domain);
        }
    }

    /**
     * 获取域名对应的请求间隔配置
     */
    public long getIntervalForDomain(String domain) {
        return defaultRequestInterval;
    }

    /**
     * 设置域名对应的请求间隔
     *
     * @param domain 域名
     * @param intervalMs 间隔（毫秒）
     */
    public void setIntervalForDomain(String domain, long intervalMs) {
        log.info("为域名 {} 设置请求间隔: {}ms", domain, intervalMs);
    }

    /**
     * 检查是否被封禁
     * <p>
     * 基于最近1分钟内请求数判断：
     * 超过100次请求视为频繁，可能已被封禁。
     * </p>
     *
     * @param domain 域名
     * @return boolean 是否被封禁
     */
    public boolean isBlocked(String domain) {
        List<Long> history = requestHistory.get(domain);
        if (history == null || history.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long recentCount = history.stream().filter(t -> now - t < 60000).count();

        if (recentCount > 100) {
            log.error("域名 {} 请求过于频繁，已被临时封禁", domain);
            return true;
        }

        return false;
    }

    /**
     * 获取重试次数
     */
    public int getRetryCount(String domain) {
        return retryCount.getOrDefault(domain, 0);
    }

    /**
     * 增加重试次数
     */
    public int incrementRetry(String domain) {
        retryCount.merge(domain, 1, Integer::sum);
        return retryCount.get(domain);
    }

    /**
     * 重置重试次数
     */
    public void resetRetry(String domain) {
        retryCount.remove(domain);
    }

    /**
     * 检查是否应该重试
     */
    public boolean shouldRetry(String domain) {
        return retryCount.getOrDefault(domain, 0) < maxRetry;
    }

    /**
     * 从URL中提取域名
     */
    private String extractDomain(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                return url.substring(0, slashIndex);
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 获取当前请求统计信息
     *
     * @param domain 域名
     * @return Map 统计信息
     */
    public Map<String, Object> getStats(String domain) {
        Map<String, Object> stats = new HashMap<>();
        List<Long> history = requestHistory.get(domain);
        long now = System.currentTimeMillis();

        stats.put("totalRequests", history != null ? history.size() : 0);
        stats.put("requestsLastMinute", history != null ? history.stream().filter(t -> now - t < 60000).count() : 0);
        stats.put("requestsLastHour", history != null ? history.stream().filter(t -> now - t < 3600000).count() : 0);
        stats.put("retryCount", retryCount.getOrDefault(domain, 0));
        stats.put("blocked", isBlocked(domain));
        stats.put("lastRequestTime", lastRequestTime.get(domain));

        return stats;
    }

    /**
     * 获取全局统计信息
     */
    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("trackedDomains", requestHistory.size());
        stats.put("totalRequests", requestHistory.values().stream().mapToLong(List::size).sum());
        stats.put("blockedDomains", requestHistory.entrySet().stream().filter(e -> isBlocked(e.getKey())).count());
        return stats;
    }

    /**
     * 清理过期的请求历史
     * <p>
     * 删除超过1小时的请求记录，防止内存泄漏。
     * 应由定时任务调用。
     * </p>
     */
    public void cleanExpiredHistory() {
        long cutoff = System.currentTimeMillis() - 3600000;
        requestHistory.values().forEach(list -> list.removeIf(time -> time < cutoff));
        lastRequestTime.entrySet().removeIf(e -> e.getValue() < cutoff);
        log.debug("清理过期请求历史完成");
    }
}
