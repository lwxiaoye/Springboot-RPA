package rpa.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rpa.dto.AiCodeGenerationRequest;

import java.util.*;

/**
 * AI代码生成服务
 * 智能分析网页结构并生成RPA机器人代码
 */
@Slf4j
@Service
public class AiCodeGenerationService {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 网页分析结果
     */
    public static class WebPageAnalysis {
        public String title;
        public boolean tableDetected;
        public String tableSelector;
        public List<String> columns = new ArrayList<>();
        public Map<String, String> companyInfo = new HashMap<>();
        public String htmlSnippet;
    }

    /**
     * 获取网页内容
     */
    private String fetchHtml(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        } catch (Exception e) {
            log.error(">>> fetchHtml 获取网页失败: {} - {}", url, e.getMessage());
            return "";
        }
    }

    /**
     * 分析网页结构
     */
    public WebPageAnalysis analyzeWebPage(String url) {
        WebPageAnalysis result = new WebPageAnalysis();

        try {
            // 获取网页内容
            String html = fetchHtml(url);
            Document doc = Jsoup.parse(html);

            // 提取标题
            result.title = doc.title();
            log.info(">>> analyzeWebPage 网页标题: {}", result.title);

            // 检测表格
            Elements tables = doc.select("table");
            if (!tables.isEmpty()) {
                result.tableDetected = true;

                // 找到第一个有数据的表格
                for (Element table : tables) {
                    Elements rows = table.select("tr");
                    if (rows.size() > 1) {
                        result.tableSelector = detectTableSelector(table);

                        // 提取表头列名
                        Elements headers = rows.get(0).select("th, td");
                        for (Element th : headers) {
                            result.columns.add(th.text().trim());
                        }
                        break;
                    }
                }
            }

            // 检测企业信息
            detectCompanyInfo(doc, result.companyInfo);

            // 保存HTML片段用于调试
            result.htmlSnippet = doc.body().html().substring(0, Math.min(2000, doc.body().html().length()));

            log.info(">>> analyzeWebPage 完成: tableDetected={}, columns={}, companyInfo={}",
                result.tableDetected, result.columns.size(), result.companyInfo.size());

        } catch (Exception e) {
            log.error(">>> analyzeWebPage 异常: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 检测表格选择器
     */
    private String detectTableSelector(Element table) {
        // 尝试从父元素获取ID或class
        Element parent = table.parent();
        String id = table.id();
        if (!id.isEmpty()) {
            return "#" + id + " tbody tr";
        }

        // 尝试获取class
        String className = table.className();
        if (!className.isEmpty()) {
            return "." + className.replace(" ", ".") + " tbody tr";
        }

        // 遍历父元素查找可用的选择器
        while (parent != null && !parent.tagName().equals("body")) {
            String parentId = parent.id();
            if (!parentId.isEmpty()) {
                return "#" + parentId + " " + table.tagName() + " tbody tr";
            }
            parent = parent.parent();
        }

        // 默认返回通用选择器
        return "table tbody tr";
    }

    /**
     * 检测企业信息
     */
    private void detectCompanyInfo(Document doc, Map<String, String> companyInfo) {
        // 检测企业名称
        String[] nameSelectors = {"#company-name", ".company-name", "#companyName", ".enterprise-name"};
        for (String selector : nameSelectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                companyInfo.put("company_name", el.text().trim());
                break;
            }
        }

        // 检测信用代码
        String[] creditSelectors = {"#credit-code", "#creditCode", ".credit-code"};
        for (String selector : creditSelectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                companyInfo.put("credit_code", el.text().trim());
                break;
            }
        }

        // 检测税号
        String[] taxSelectors = {"#tax-no", "#taxNo", "#tax-no"};
        for (String selector : taxSelectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                companyInfo.put("tax_no", el.text().trim());
                break;
            }
        }
    }

    /**
     * 生成完整的RPA流程代码
     */
    public String generateFullWorkflow(AiCodeGenerationRequest.GenerationRequest request) {
        StringBuilder code = new StringBuilder();

        // 1. 采集部分
        code.append("// ").append(getSceneTitle(request.getScene())).append("\n");
        code.append("// 功能：自动采集网页数据并落库\n");
        code.append("@collect ").append(request.getUrl()).append("\n");

        if (request.getTableSelector() != null && !request.getTableSelector().isEmpty()) {
            code.append("@table_selector ").append(request.getTableSelector()).append("\n");
        }

        // 2. 列配置
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            code.append("@columns ").append(String.join(",", request.getColumns())).append("\n");
        } else {
            // 使用默认列配置
            code.append("@columns #,号码,类型,状态,日期,不含税,税额,价税合计\n");
        }

        // 3. 落库部分
        String targetTable = request.getTargetTable() != null ? request.getTargetTable() : "invoice_data";
        code.append("@store ").append(targetTable).append("\n");

        // 4. 日志
        code.append("@log ").append(getSceneTitle(request.getScene())).append("完成\n");

        return code.toString();
    }

    /**
     * 生成代码说明
     */
    public String generateExplanation(AiCodeGenerationRequest.GenerationRequest request) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("本代码实现了").append(getSceneTitle(request.getScene())).append("功能：\n");
        explanation.append("- 采集URL: ").append(request.getUrl()).append("\n");
        explanation.append("- 目标表: ").append(request.getTargetTable()).append("\n");
        if (request.getTableSelector() != null) {
            explanation.append("- 表格选择器: ").append(request.getTableSelector()).append("\n");
        }
        explanation.append("- 支持的数据类型: 发票、订单、企业信息、商品等\n");
        explanation.append("- 自动数据校验和清洗");
        return explanation.toString();
    }

    /**
     * 检测表格列
     */
    public Map<String, String> detectTableColumns(String url, String tableSelector) {
        Map<String, String> columns = new LinkedHashMap<>();

        try {
            String html = fetchHtml(url);
            Document doc = Jsoup.parse(html);

            Element table;
            if (tableSelector != null && !tableSelector.isEmpty()) {
                table = doc.selectFirst(tableSelector);
            } else {
                table = doc.selectFirst("table");
            }

            if (table != null) {
                Elements headers = table.select("thead th, thead td");
                if (headers.isEmpty()) {
                    Elements firstRow = table.select("tr:first-child th, tr:first-child td");
                    for (Element th : firstRow) {
                        columns.put(th.text().trim(), th.text().trim());
                    }
                } else {
                    for (Element th : headers) {
                        columns.put(th.text().trim(), th.text().trim());
                    }
                }
            }
        } catch (Exception e) {
            log.error(">>> detectTableColumns 异常: {}", e.getMessage(), e);
        }

        return columns;
    }

    /**
     * 获取可用的代码模板
     */
    public Map<String, String> getAvailableTemplates() {
        Map<String, String> templates = new LinkedHashMap<>();
        templates.put("invoice", "// 发票采集场景\n@collect {URL}\n@table_selector #invoice-table tbody tr\n@columns #,号码,类型,状态,日期,不含税,税额,价税合计\n@store invoice_data");
        templates.put("order", "// 订单采集场景\n@collect {URL}\n@table_selector .order-list tbody tr\n@columns 订单号,商品名称,数量,单价,总价,下单时间\n@store order_data");
        templates.put("company", "// 企业信息采集\n@collect {URL}\n@selector company_name=#company-name\n@selector credit_code=#credit-code\n@log 企业信息采集完成");
        templates.put("product", "// 商品采集场景\n@collect {URL}\n@table_selector .product-list tbody tr\n@columns 商品编码,商品名称,规格,单价,库存\n@store product_data");
        templates.put("general", "// 通用采集场景\n@collect {URL}\n@table_selector table tbody tr\n@store data_table");
        return templates;
    }

    private String getSceneTitle(String scene) {
        switch (scene != null ? scene : "general") {
            case "invoice": return "发票数据采集";
            case "order": return "订单数据采集";
            case "company": return "企业信息采集";
            case "product": return "商品信息采集";
            default: return "数据采集";
        }
    }
}