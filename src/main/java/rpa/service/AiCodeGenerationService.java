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

        if (request.getUrl() != null && !request.getUrl().isEmpty()) {
            code.append("@collect ").append(request.getUrl()).append("\n");
        }

        if (request.getTableSelector() != null && !request.getTableSelector().isEmpty()) {
            code.append("@table_selector ").append(request.getTableSelector()).append("\n");
        }

        // 2. 解析
        code.append("@parse\n");

        // 3. 列配置
        if (request.getColumns() != null && !request.getColumns().isEmpty()) {
            code.append("@columns ").append(String.join(",", request.getColumns())).append("\n");
        } else {
            // 使用默认列配置
            code.append("@columns 号码,金额,日期,类型\n");
        }

        // 4. 处理步骤
        if (request.getProcessSteps() != null && !request.getProcessSteps().isEmpty()) {
            code.append("@process ").append(String.join(",", request.getProcessSteps())).append("\n");
        }

        // 5. 落库部分
        String targetTable = request.getTargetTable() != null ? request.getTargetTable() : "data_table";
        code.append("@store ").append(targetTable).append("\n");

        // 6. 日志
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
            case "DATA_COLLECT": return "数据采集";
            case "DATA_PARSE": return "数据解析";
            case "DATA_PROCESS": return "数据加工";
            default: return "数据采集";
        }
    }

    /**
     * 根据提示词和类型生成RPA机器人代码
     * 支持分布式、单一职责的机器人生成
     *
     * @param prompt 用户需求描述
     * @param url 采集目标URL（采集机器人必需）
     * @param robotType 机器人类别：DATA_COLLECT, DATA_PARSE, DATA_PROCESS, DATA_STORE
     * @param targetTable 目标表名（落库机器人必需）
     * @return 生成的RPA机器人代码
     */
    public String generateFromPrompt(String prompt, String url, String robotType, String targetTable) {
        StringBuilder code = new StringBuilder();
        String type = robotType != null ? robotType : "DATA_COLLECT";

        log.info(">>> generateFromPrompt 开始生成代码");
        log.info("    prompt: {}", prompt);
        log.info("    url: {}", url);
        log.info("    robotType: {}", type);
        log.info("    targetTable: {}", targetTable);

        // 头部注释
        code.append("// ====== RPA机器人代码 - ").append(getRobotTypeName(type)).append(" ======\n");
        code.append("// 生成时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        code.append("// 需求: ").append(prompt).append("\n");
        code.append("// Redis上下文key: rpa:process:context:{processId}\n");
        code.append("// ====== 单一职责机器人 ======\n\n");

        switch (type) {
            case "DATA_COLLECT" -> code.append(generateCollectRobot(url, prompt));
            case "DATA_PARSE" -> code.append(generateParseRobot(url, prompt));
            case "DATA_PROCESS" -> code.append(generateProcessRobot(prompt));
            case "DATA_STORE" -> code.append(generateStoreRobot(targetTable, prompt));
            default -> code.append(generateCollectRobot(url, prompt));
        }

        log.info(">>> generateFromPrompt 代码生成完成，长度: {} 字符", code.length());
        return code.toString();
    }

    /**
     * 生成数据采集机器人
     * 职责：只负责采集网页HTML，不做其他处理
     */
    private String generateCollectRobot(String url, String prompt) {
        StringBuilder code = new StringBuilder();

        code.append("// 【数据采集机器人】\n");
        code.append("// 职责：从指定URL采集HTML内容，存入Redis上下文\n");
        code.append("// 输入：URL\n");
        code.append("// 输出：rawHtml（存储到Redis）\n\n");

        // URL处理
        String targetUrl = (url != null && !url.isEmpty()) ? url : "http://example.com/data";
        code.append("// 采集目标\n");
        code.append("@collect ").append(targetUrl).append("\n");

        code.append("\n// 日志\n");
        code.append("@log [DATA_COLLECT] 采集任务已完成\n");
        code.append("@log [DATA_COLLECT] rawHtml已存入Redis上下文\n");

        return code.toString();
    }

    /**
     * 生成数据解析机器人
     * 职责：从Redis读取HTML，解析出结构化数据
     */
    private String generateParseRobot(String url, String prompt) {
        StringBuilder code = new StringBuilder();

        code.append("// 【数据解析机器人】\n");
        code.append("// 职责：从Redis读取rawHtml，解析为结构化数据\n");
        code.append("// 输入：rawHtml（从Redis读取）\n");
        code.append("// 输出：parsedData（存储到Redis）\n\n");

        // URL配置（可选，用于自动识别表格）
        if (url != null && !url.isEmpty()) {
            code.append("// 来源URL\n");
            code.append("@url ").append(url).append("\n");
        }

        code.append("// 表格选择器\n");
        code.append("@table_selector table tbody tr\n");

        code.append("// 列配置\n");
        code.append("@columns 序号,字段1,字段2,字段3\n");

        code.append("// 执行解析\n");
        code.append("@parse\n");

        code.append("\n// 日志\n");
        code.append("@log [DATA_PARSE] 解析任务已完成\n");
        code.append("@log [DATA_PARSE] parsedData已存入Redis上下文\n");

        return code.toString();
    }

    /**
     * 生成数据加工机器人
     * 职责：对parsedData进行清洗、转换、校验
     */
    private String generateProcessRobot(String prompt) {
        StringBuilder code = new StringBuilder();

        code.append("// 【数据加工机器人】\n");
        code.append("// 职责：对Redis中的parsedData进行清洗、转换、校验\n");
        code.append("// 输入：parsedData（从Redis读取）\n");
        code.append("// 输出：processedData（存储到Redis）\n\n");

        code.append("// 数据处理步骤\n");
        code.append("@process clean,transform,validate\n");

        code.append("\n// 日志\n");
        code.append("@log [DATA_PROCESS] 加工任务已完成\n");
        code.append("@log [DATA_PROCESS] processedData已存入Redis上下文\n");

        return code.toString();
    }

    /**
     * 生成数据落库机器人
     * 职责：将processedData保存到数据库
     */
    private String generateStoreRobot(String targetTable, String prompt) {
        StringBuilder code = new StringBuilder();

        String table = (targetTable != null && !targetTable.isEmpty()) ? targetTable : "data_table";

        code.append("// 【数据落库机器人】\n");
        code.append("// 职责：将Redis中的processedData保存到数据库\n");
        code.append("// 输入：processedData（从Redis读取）\n");
        code.append("// 输出：数据库记录\n\n");

        code.append("// 目标表\n");
        code.append("@store ").append(table).append("\n");

        code.append("\n// 日志\n");
        code.append("@log [DATA_STORE] 落库任务已完成\n");
        code.append("@log [DATA_STORE] 数据已保存到表: ").append(table).append("\n");

        return code.toString();
    }

    /**
     * 获取机器人类型名称
     */
    private String getRobotTypeName(String type) {
        switch (type != null ? type : "DATA_COLLECT") {
            case "DATA_COLLECT": return "数据采集";
            case "DATA_PARSE": return "数据解析";
            case "DATA_PROCESS": return "数据加工";
            case "DATA_STORE": return "数据落库";
            default: return "数据采集";
        }
    }

    /**
     * 生成完整的分布式流水线代码（返回多个机器人的代码）
     */
    public List<String> generateDistributedPipeline(String prompt, String url, String targetTable) {
        List<String> robots = new ArrayList<>();

        robots.add(generateCollectRobot(url, prompt + " - 采集阶段"));
        robots.add(generateParseRobot(url, prompt + " - 解析阶段"));
        robots.add(generateProcessRobot(prompt + " - 加工阶段"));
        robots.add(generateStoreRobot(targetTable, prompt + " - 落库阶段"));

        return robots;
    }

    /**
     * 获取场景默认列配置
     */
    private String getDefaultColumns(String scene) {
        switch (scene != null ? scene : "general") {
            case "invoice": return "序号,号码,类型,状态,日期,不含税,税额,价税合计";
            case "order": return "订单号,商品名称,数量,单价,总价,下单时间";
            case "company": return "企业名称,信用代码,税号,类型,地址";
            case "product": return "商品编码,商品名称,规格,单价,库存";
            case "DATA_COLLECT": return "标题,内容,链接,时间";
            case "DATA_PARSE": return "字段1,字段2,字段3,字段4";
            case "DATA_PROCESS": return "原始值,处理后,状态,备注";
            default: return "序号,内容,金额,备注";
        }
    }

    /**
     * 解析并规范化AI生成的代码
     * 清理多余的双引号和格式问题
     */
    public String normalizeGeneratedCode(String rawCode) {
        if (rawCode == null || rawCode.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = rawCode.split("\n");

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("```")) {
                continue;
            }

            if (line.startsWith("import ") || line.startsWith("from ") ||
                line.startsWith("def ") || line.startsWith("class ") ||
                line.startsWith("if __name__")) {
                continue;
            }

            if (line.startsWith("@")) {
                line = line.replaceAll("\"+", "");
                line = line.replaceAll("'$", "");
            }

            result.append(line).append("\n");
        }

        return result.toString();
    }
}