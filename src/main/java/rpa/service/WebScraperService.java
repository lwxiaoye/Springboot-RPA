package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import rpa.entity.DataCollect;
import rpa.entity.CollectedData;
import rpa.repository.CollectedDataRepository;
import java.time.LocalDateTime;
import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebScraperService {

    private final CollectedDataRepository collectedDataRepository;

    private static final int TIMEOUT = 30000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public Map<String, Object> executeCollect(DataCollect collect) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> scrapedData = new ArrayList<>();

        try {
            log.info("开始采集数据: {}, URL: {}", collect.getName(), collect.getSourceUrl());

            Document doc;
            try {
                doc = Jsoup.connect(collect.getSourceUrl())
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT)
                        .maxBodySize(0)
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .get();
            } catch (Exception e) {
                log.error("连接失败: {}", e.getMessage());
                result.put("success", false);
                result.put("message", "连接失败: " + e.getMessage());
                return result;
            }

            if (doc == null) {
                result.put("success", false);
                result.put("message", "获取页面内容失败");
                return result;
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

            log.info("解析出 {} 条数据，准备保存", scrapedData.size());

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
                } catch (Exception e) {
                    log.error("保存单条数据失败: {}", e.getMessage());
                }
            }

            collect.setLastCollectTime(System.currentTimeMillis());
            collect.setDataCount(collect.getDataCount() + scrapedData.size());

            result.put("success", true);
            result.put("message", "采集成功");
            result.put("count", scrapedData.size());
            result.put("data", scrapedData);

            log.info("采集完成: {}, 数据量: {}", collect.getName(), scrapedData.size());

        } catch (Exception e) {
            log.error("采集失败: {}", collect.getName(), e);
            result.put("success", false);
            result.put("message", "采集失败: " + e.getMessage());
            result.put("count", 0);
        }

        return result;
    }

    public Map<String, Object> scrapeByUrl(String url, Map<String, String> selectors) {
        Map<String, Object> result = new HashMap<>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .maxBodySize(0)
                    .get();

            Map<String, String> data = new HashMap<>();
            for (Map.Entry<String, String> entry : selectors.entrySet()) {
                Elements els = doc.select(entry.getValue());
                if (!els.isEmpty()) {
                    data.put(entry.getKey(), els.first().text());
                }
            }

            result.put("success", true);
            result.put("data", data);
            result.put("html", doc.html());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> scrapeDynamicContent(String url, String script) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "动态内容需要Playwright客户端支持，当前使用JSoup静态解析");
        result.put("tip", "请在机器人端安装Playwright并配置执行");
        return result;
    }

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

    private Elements selectElements(Document doc, String selector) {
        if (selector == null || selector.isEmpty()) {
            return new Elements(doc.body().children());
        }
        Elements elements = doc.select(selector);
        return elements.isEmpty() ? new Elements(doc.body().children()) : elements;
    }

    public String previewPage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .maxBodySize(0)
                    .get();
            return doc.body().html();
        } catch (Exception e) {
            return "<div style='color:red;padding:20px;'>获取失败: " + e.getMessage() + "</div>";
        }
    }
}
