package rpa.service;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rpa.entity.DataCollect;
import rpa.entity.CollectedData;
import rpa.repository.CollectedDataRepository;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 网页采集服务类
 * <p>
 * 提供网页数据采集的核心功能，整合多种采集方式。
 * </p>
 * <p>
 * 采集流程：
 * <ol>
 *   <li>获取分布式锁 - 防止多节点重复采集</li>
 *   <li>检查缓存 - 有缓存直接返回</li>
 *   <li>更新任务状态 - 标记为运行中</li>
 *   <li>频率控制 - 遵守反爬策略</li>
 *   <li>根据类型选择采集方式 - static/dynamic/api</li>
 *   <li>保存数据到数据库</li>
 *   <li>缓存结果</li>
 *   <li>释放分布式锁</li>
 * </ol>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebScraperService {

    private final CollectedDataRepository collectedDataRepository;
    private final CacheService cacheService;
    private final PlaywrightService playwrightService;
    private final DistributedCollectorService distributedService;
    private final AntiCrawlerService antiCrawlerService;

    /** 请求间隔（毫秒） */
    @Value("${scraper.request-interval:1000}")
    private long requestInterval;

    /** 默认User-Agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    
    /** 请求超时（毫秒） */
    private static final int TIMEOUT = 30000;
    
    /** 本地节点ID */
    private static final String LOCAL_NODE_ID = initNodeId();

    /**
     * 初始化节点ID
     */
    private static String initNodeId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "_" + ProcessHandle.current().pid();
        } catch (Exception e) {
            return "unknown_node_" + System.currentTimeMillis();
        }
    }

    /**
     * 执行采集
     * <p>
     * 整合所有采集功能的主入口方法。
     * </p>
     *
     * @param collect 采集配置
     * @return Map 执行结果
     */
    public Map<String, Object> executeCollect(DataCollect collect) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> scrapedData = new ArrayList<>();
        String lockRequestId = null;

        // 1. 尝试获取分布式锁（防止多节点重复采集）
        lockRequestId = distributedService.acquireTaskLock(collect.getId(), LOCAL_NODE_ID);
        if (lockRequestId == null) {
            result.put("success", false);
            result.put("message", "任务正在其他节点执行中");
            return result;
        }

        // 2. 检查缓存（如果有未过期的缓存数据）
        @SuppressWarnings("unchecked")
        List<Map<String, String>> cachedData = cacheService.getScrapedData(
            String.valueOf(collect.getId()),
            List.class
        );
        if (cachedData != null && !cachedData.isEmpty()) {
            log.info("使用缓存数据: {} 条", cachedData.size());
            result.put("success", true);
            result.put("message", "从缓存获取");
            result.put("count", cachedData.size());
            result.put("data", cachedData);
            result.put("fromCache", true);

            // 释放锁
            distributedService.releaseTaskLock(collect.getId(), lockRequestId);
            return result;
        }

        // 3. 更新任务状态
        distributedService.updateTaskStatus(collect.getId(), "running", LOCAL_NODE_ID);

        try {
            log.info("开始采集数据: {}, URL: {}", collect.getName(), collect.getSourceUrl());

            // 4. 频率控制
            antiCrawlerService.waitForRequest(collect.getSourceUrl());
            distributedService.recordRequest(LOCAL_NODE_ID);

            // 5. 根据采集类型选择采集方式
            String sourceType = collect.getSourceType();
            if ("dynamic".equalsIgnoreCase(sourceType)) {
                // 动态页面（使用Playwright）
                scrapedData = scrapeDynamic(collect);
            } else if ("api".equalsIgnoreCase(sourceType)) {
                // API接口
                scrapedData = scrapeApi(collect);
            } else {
                // 默认静态页面（使用JSoup）
                scrapedData = scrapeStatic(collect);
            }

            // 6. 保存采集结果
            int savedCount = saveCollectedData(collect, scrapedData);

            // 7. 缓存结果
            if (!scrapedData.isEmpty()) {
                cacheService.cacheScrapedData(String.valueOf(collect.getId()), scrapedData);
            }

            // 8. 记录成功请求
            antiCrawlerService.recordRequest(collect.getSourceUrl(), true);

            // 9. 同步进度
            distributedService.syncProgress(collect.getId(), savedCount, scrapedData.size());

            result.put("success", true);
            result.put("message", "采集成功");
            result.put("count", savedCount);
            result.put("data", scrapedData);
            result.put("nodeId", LOCAL_NODE_ID);

            log.info("采集完成: {}, 数据量: {}", collect.getName(), scrapedData.size());

        } catch (Exception e) {
            log.error("采集失败: {}", collect.getName(), e);

            // 记录失败请求
            antiCrawlerService.recordRequest(collect.getSourceUrl(), false);
            antiCrawlerService.incrementRetry(collect.getSourceUrl());

            // 检查是否需要重试
            if (antiCrawlerService.shouldRetry(collect.getSourceUrl())) {
                result.put("retry", true);
                result.put("retryCount", antiCrawlerService.getRetryCount(collect.getSourceUrl()));
            }

            result.put("success", false);
            result.put("message", "采集失败: " + e.getMessage());
            result.put("count", 0);

            distributedService.updateTaskStatus(collect.getId(), "failed", LOCAL_NODE_ID);
        } finally {
            // 10. 释放分布式锁
            distributedService.releaseTaskLock(collect.getId(), lockRequestId);
        }

        return result;
    }

    /**
     * 静态页面采集（JSoup）
     *
     * @param collect 采集配置
     * @return List 采集的数据
     */
    private List<Map<String, String>> scrapeStatic(DataCollect collect) {
        List<Map<String, String>> scrapedData = new ArrayList<>();

        Document doc = fetchPage(collect.getSourceUrl(), collect.getHeaders(), collect.getCookies());
        if (doc == null) {
            return scrapedData;
        }

        Map<String, String> selectorRules = parseSelectorRules(collect.getSelectorRules());
        String listSelector = selectorRules.get("listSelector");

        Elements rows;
        if (listSelector != null && !listSelector.isEmpty()) {
            rows = doc.select(listSelector);
        } else {
            rows = doc.body().children();
        }

        log.info("找到 {} 个元素", rows.size());

        for (Element row : rows) {
            Map<String, String> item = new HashMap<>();
            for (Map.Entry<String, String> entry : selectorRules.entrySet()) {
                if (!"listSelector".equals(entry.getKey())) {
                    try {
                        Elements els = row.select(entry.getValue());
                        String value = els.isEmpty() ? "" : els.first().text();
                        item.put(entry.getKey(), value);
                    } catch (Exception e) {
                        item.put(entry.getKey(), "");
                    }
                }
            }
            item.put("sourceUrl", collect.getSourceUrl());
            item.put("collectTime", LocalDateTime.now().toString());
            scrapedData.add(item);
        }

        return scrapedData;
    }

    /**
     * 动态页面采集（Playwright）
     *
     * @param collect 采集配置
     * @return List 采集的数据
     */
    private List<Map<String, String>> scrapeDynamic(DataCollect collect) {
        Map<String, String> selectorRules = parseSelectorRules(collect.getSelectorRules());

        int waitSeconds = selectorRules.containsKey("waitSeconds") ?
            Integer.parseInt(selectorRules.get("waitSeconds")) : 2;
        selectorRules.put("waitSeconds", String.valueOf(waitSeconds));

        Map<String, Object> playwrightResult = playwrightService.scrapeDynamicPage(
            collect.getSourceUrl(),
            selectorRules,
            waitSeconds
        );

        if ((boolean) playwrightResult.getOrDefault("success", false)) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> data = (List<Map<String, String>>) playwrightResult.get("data");
            return data != null ? data : new ArrayList<>();
        }

        log.warn("Playwright采集失败，回退到JSoup: {}", playwrightResult.get("message"));
        return scrapeStatic(collect);
    }

    /**
     * API接口采集
     *
     * @param collect 采集配置
     * @return List 采集的数据
     */
    private List<Map<String, String>> scrapeApi(DataCollect collect) {
        List<Map<String, String>> scrapedData = new ArrayList<>();

        Document doc = fetchPage(collect.getSourceUrl(), collect.getHeaders(), collect.getCookies());
        if (doc == null) {
            return scrapedData;
        }

        try {
            String html = doc.body().text();
            if (html.trim().startsWith("{")) {
                // JSON格式
                com.alibaba.fastjson.JSONObject json = JSON.parseObject(html);
                Map<String, String> item = new HashMap<>();
                flattenJson(json, "", item);
                item.put("sourceUrl", collect.getSourceUrl());
                item.put("collectTime", LocalDateTime.now().toString());
                scrapedData.add(item);
            }
        } catch (Exception e) {
            log.error("API解析失败: {}", e.getMessage());
        }

        return scrapedData;
    }

    /**
     * 拉取页面
     */
    private Document fetchPage(String url, String headersJson, String cookiesJson) {
        try {
            Map<String, String> headers = parseHeaders(headersJson);
            Map<String, String> cookies = parseCookies(cookiesJson);
            String userAgent = antiCrawlerService.getRandomUserAgent();

            org.jsoup.Connection conn = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(TIMEOUT)
                    .maxBodySize(0)
                    .followRedirects(true)
                    .ignoreHttpErrors(true);

            // 添加自定义headers
            headers.forEach(conn::header);

            // 添加cookies
            cookies.forEach(conn::cookie);

            // 检查是否使用代理
            String proxy = antiCrawlerService.getNextProxy();
            if (proxy != null) {
                String[] parts = proxy.split(":");
                if (parts.length == 2) {
                    conn.proxy(parts[0], Integer.parseInt(parts[1]));
                }
            }

            return conn.get();

        } catch (Exception e) {
            log.error("连接失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 保存采集数据
     */
    private int saveCollectedData(DataCollect collect, List<Map<String, String>> scrapedData) {
        int count = 0;
        for (Map<String, String> data : scrapedData) {
            try {
                CollectedData collected = new CollectedData();
                collected.setCollectId(collect.getId());
                collected.setCollectName(collect.getName());
                collected.setRawData(JSON.toJSONString(data));
                collected.setSourceUrl(collect.getSourceUrl());
                collected.setDataType(collect.getSourceType());
                collected.setParseStatus(0);
                collected.setCollectTime(LocalDateTime.now());
                collectedDataRepository.save(collected);
                count++;
            } catch (Exception e) {
                log.error("保存单条数据失败: {}", e.getMessage());
            }
        }
        return count;
    }

    /**
     * 解析选择器规则
     */
    private Map<String, String> parseSelectorRules(String rulesJson) {
        Map<String, String> rules = new HashMap<>();
        if (rulesJson == null || rulesJson.isEmpty()) {
            rules.put("listSelector", "tr, li, .item, .row");
            return rules;
        }
        try {
            com.alibaba.fastjson.JSONObject obj = JSON.parseObject(rulesJson);
            obj.forEach((k, v) -> rules.put(k, v.toString()));
        } catch (Exception e) {
            rules.put("listSelector", "tr, li, .item, .row");
        }
        return rules;
    }

    /**
     * 解析请求头
     */
    private Map<String, String> parseHeaders(String headersJson) {
        Map<String, String> headers = new HashMap<>();
        if (headersJson == null || headersJson.isEmpty()) {
            return headers;
        }
        try {
            com.alibaba.fastjson.JSONObject obj = JSON.parseObject(headersJson);
            obj.forEach((k, v) -> headers.put(k, v.toString()));
        } catch (Exception e) {
            log.warn("解析请求头失败: {}", e.getMessage());
        }
        return headers;
    }

    /**
     * 解析Cookies
     */
    private Map<String, String> parseCookies(String cookiesJson) {
        Map<String, String> cookies = new HashMap<>();
        if (cookiesJson == null || cookiesJson.isEmpty()) {
            return cookies;
        }
        try {
            com.alibaba.fastjson.JSONObject obj = JSON.parseObject(cookiesJson);
            obj.forEach((k, v) -> cookies.put(k, v.toString()));
        } catch (Exception e) {
            log.warn("解析Cookies失败: {}", e.getMessage());
        }
        return cookies;
    }

    /**
     * 扁平化JSON
     */
    private void flattenJson(com.alibaba.fastjson.JSONObject json, String prefix, Map<String, String> result) {
        for (String key : json.keySet()) {
            Object value = json.get(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof com.alibaba.fastjson.JSONObject) {
                flattenJson((com.alibaba.fastjson.JSONObject) value, fullKey, result);
            } else {
                result.put(fullKey, value != null ? value.toString() : "");
            }
        }
    }

    /**
     * 预览页面
     *
     * @param url 目标URL
     * @return String 页面HTML
     */
    public String previewPage(String url) {
        try {
            String userAgent = antiCrawlerService.getRandomUserAgent();
            Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(TIMEOUT)
                    .maxBodySize(0)
                    .get();
            return doc.body().html();
        } catch (Exception e) {
            return "<div style='color:red;padding:20px;'>获取失败: " + e.getMessage() + "</div>";
        }
    }

    /**
     * 获取采集状态
     */
    public Map<String, Object> getCollectStatus(Long collectId) {
        Map<String, Object> status = new HashMap<>();
        status.put("taskStatus", distributedService.getTaskStatus(collectId));
        status.put("progress", distributedService.getProgress(collectId));
        status.put("isBlocked", antiCrawlerService.isBlocked(String.valueOf(collectId)));
        status.put("playwrightAvailable", playwrightService.isAvailable());
        return status;
    }

    /**
     * 获取节点信息
     */
    public Map<String, Object> getNodeInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nodeId", LOCAL_NODE_ID);
        info.put("activeTasks", distributedService.getActiveNodes().size());
        info.put("playwrightAvailable", playwrightService.isAvailable());
        info.put("cacheEnabled", true);
        return info;
    }
}
