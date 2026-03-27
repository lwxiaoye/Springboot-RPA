package rpa.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Playwright浏览器自动化服务类
 * <p>
 * 提供基于Playwright的浏览器自动化功能，
 * 用于采集JavaScript动态渲染的网页内容。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>动态页面采集 - 支持JS渲染的页面</li>
 *   <li>登录后采集 - 自动填写登录表单</li>
 *   <li>滚动加载采集 - 模拟滚动触发懒加载</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
public class PlaywrightService {

    private Playwright playwright;
    private Browser browser;
    private final Map<String, BrowserContext> contexts = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    /**
     * 初始化Playwright
     * <p>
     * 应用启动时自动调用，使用无头模式启动Chromium。
     * </p>
     */
    @PostConstruct
    public void init() {
        try {
            playwright = Playwright.create();
            log.info("Playwright 初始化中...");

            try {
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(List.of(
                                "--no-sandbox",
                                "--disable-setuid-sandbox",
                                "--disable-dev-shm-usage",
                                "--disable-gpu"
                        )));
                initialized = true;
                log.info("Playwright Chromium 启动成功 (无头模式)");
            } catch (Exception e) {
                log.warn("Playwright Chromium 启动失败: {}. 动态内容功能不可用。", e.getMessage());
                log.info("提示: 运行 'mvn playwright install' 安装浏览器驱动");
                initialized = false;
            }

        } catch (Exception e) {
            log.error("Playwright 初始化失败: {}", e.getMessage());
            initialized = false;
        }
    }

    /**
     * 释放资源
     * <p>
     * 应用关闭时自动调用，关闭所有浏览器资源。
     * </p>
     */
    @PreDestroy
    public void cleanup() {
        try {
            contexts.values().forEach(BrowserContext::close);
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            log.info("Playwright 资源已释放");
        } catch (Exception e) {
            log.error("Playwright 清理失败: {}", e.getMessage());
        }
    }

    /**
     * 检查Playwright是否可用
     */
    public boolean isAvailable() {
        return initialized && browser != null;
    }

    /**
     * 采集动态页面
     * <p>
     * 使用Playwright访问动态渲染的页面，
     * 支持等待特定元素出现和自定义选择器。
     * </p>
     *
     * @param url 目标URL
     * @param selectors 选择器规则（listSelector/waitSeconds等）
     * @param waitSeconds 等待动态内容加载的时间
     * @return Map 采集结果
     */
    public Map<String, Object> scrapeDynamicPage(String url, Map<String, String> selectors, int waitSeconds) {
        Map<String, Object> result = new HashMap<>();

        if (!isAvailable()) {
            result.put("success", false);
            result.put("message", "Playwright 不可用，请确保已安装浏览器驱动");
            result.put("tip", "运行: mvn playwright install");
            return result;
        }

        BrowserContext context = null;
        Page page = null;

        try {
            log.info("使用Playwright采集动态页面: {}", url);

            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            page = context.newPage();

            page.setDefaultTimeout(30000);

            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            if (waitSeconds > 0) {
                page.waitForTimeout(waitSeconds * 1000L);
            }

            String listSelector = selectors.get("listSelector");
            if (listSelector != null && !listSelector.isEmpty()) {
                try {
                    page.waitForSelector(listSelector, new Page.WaitForSelectorOptions().setTimeout(10000));
                } catch (Exception e) {
                    log.warn("等待元素 {} 超时，继续采集", listSelector);
                }
            }

            List<Map<String, String>> dataList = new ArrayList<>();

            if (listSelector != null && !listSelector.isEmpty()) {
                List<Locator> rows = page.locator(listSelector).all();
                for (Locator row : rows) {
                    Map<String, String> item = new HashMap<>();
                    for (Map.Entry<String, String> entry : selectors.entrySet()) {
                        if (!"listSelector".equals(entry.getKey()) && !"waitSeconds".equals(entry.getKey())) {
                            try {
                                Locator el = row.locator(entry.getValue()).first();
                                String text = el.textContent();
                                item.put(entry.getKey(), text != null ? text.trim() : "");
                            } catch (Exception e) {
                                item.put(entry.getKey(), "");
                            }
                        }
                    }
                    if (!item.isEmpty()) {
                        dataList.add(item);
                    }
                }
            } else {
                Map<String, String> item = new HashMap<>();
                for (Map.Entry<String, String> entry : selectors.entrySet()) {
                    if (!"listSelector".equals(entry.getKey()) && !"waitSeconds".equals(entry.getKey())) {
                        try {
                            Locator el = page.locator(entry.getValue()).first();
                            String text = el.textContent();
                            item.put(entry.getKey(), text != null ? text.trim() : "");
                        } catch (Exception e) {
                            item.put(entry.getKey(), "");
                        }
                    }
                }
                if (!item.isEmpty()) {
                    dataList.add(item);
                }
            }

            result.put("success", true);
            result.put("message", "动态页面采集成功");
            result.put("count", dataList.size());
            result.put("data", dataList);

            log.info("Playwright 采集完成，获取 {} 条数据", dataList.size());

        } catch (Exception e) {
            log.error("Playwright 采集失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "采集失败: " + e.getMessage());
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
        }

        return result;
    }

    /**
     * 登录后采集
     * <p>
     * 先执行登录操作，再访问目标页面采集数据。
     * 适用于需要认证才能访问的页面。
     * </p>
     *
     * @param url 目标URL
     * @param loginUrl 登录页面URL
     * @param loginForm 登录表单字段（用户名/密码）
     * @param selectors 数据选择器
     * @return Map 采集结果
     */
    public Map<String, Object> loginAndScrape(String url, String loginUrl,
                                              Map<String, String> loginForm,
                                              Map<String, String> selectors) {
        Map<String, Object> result = new HashMap<>();

        if (!isAvailable()) {
            result.put("success", false);
            result.put("message", "Playwright 不可用");
            return result;
        }

        BrowserContext context = null;
        Page page = null;

        try {
            log.info("开始登录采集: {}", url);

            context = browser.newContext();
            page = context.newPage();

            page.navigate(loginUrl);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            for (Map.Entry<String, String> field : loginForm.entrySet()) {
                page.fill(field.getKey(), field.getValue());
            }

            page.click("button[type='submit'], input[type='submit']");
            page.waitForTimeout(2000);

            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            List<Map<String, String>> dataList = new ArrayList<>();
            String listSelector = selectors.get("listSelector");

            if (listSelector != null) {
                List<Locator> rows = page.locator(listSelector).all();
                for (Locator row : rows) {
                    Map<String, String> item = new HashMap<>();
                    for (Map.Entry<String, String> entry : selectors.entrySet()) {
                        if (!"listSelector".equals(entry.getKey())) {
                            try {
                                String text = row.locator(entry.getValue()).first().textContent();
                                item.put(entry.getKey(), text != null ? text.trim() : "");
                            } catch (Exception e) {
                                item.put(entry.getKey(), "");
                            }
                        }
                    }
                    if (!item.isEmpty()) {
                        dataList.add(item);
                    }
                }
            }

            result.put("success", true);
            result.put("data", dataList);
            result.put("count", dataList.size());

        } catch (Exception e) {
            log.error("登录采集失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
        }

        return result;
    }

    /**
     * 滚动加载采集
     * <p>
     * 通过模拟滚动操作触发页面的懒加载，
     * 适用于无限滚动的页面。
     * </p>
     *
     * @param url 目标URL
     * @param listSelector 列表选择器
     * @param scrollCount 滚动次数
     * @param scrollDelayMs 滚动间隔（毫秒）
     * @return Map 采集结果
     */
    public Map<String, Object> scrollAndLoad(String url, String listSelector, int scrollCount, int scrollDelayMs) {
        Map<String, Object> result = new HashMap<>();

        if (!isAvailable()) {
            result.put("success", false);
            result.put("message", "Playwright 不可用");
            return result;
        }

        BrowserContext context = null;
        Page page = null;

        try {
            context = browser.newContext();
            page = context.newPage();
            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            for (int i = 0; i < scrollCount; i++) {
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                page.waitForTimeout(scrollDelayMs);
                log.debug("滚动第 {} 次", i + 1);
            }

            page.waitForTimeout(1000);

            List<String> items = page.locator(listSelector).allTextContents();

            result.put("success", true);
            result.put("count", items.size());
            result.put("data", items);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } finally {
            if (page != null) page.close();
            if (context != null) context.close();
        }

        return result;
    }
}
