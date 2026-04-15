package rpa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import rpa.entity.CollectedData;
import rpa.entity.Robot;
import rpa.entity.InvoiceData;
import rpa.repository.CollectedDataRepository;
import rpa.repository.InvoiceDataRepository;
import rpa.repository.RobotRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 机器人代码执行器
 * <p>
 * 解析并执行机器人代码中的命令，支持灵活的数据采集、解析、加工、落库。
 * 使用Redis缓存流程执行上下文，实现机器人间的数据共享。
 * </p>
 * <p>
 * 支持的命令：
 * <ul>
 *   <li>@url - 设置采集目标URL</li>
 *   <li>@headers - 设置请求头</li>
 *   <li>@collect - 执行数据采集</li>
 *   <li>@save - 保存数据到上下文</li>
 *   <li>@load - 从上下文加载数据</li>
 *   <li>@parse_html - HTML解析模式</li>
 *   <li>@selector - CSS选择器配置</li>
 *   <li>@table_selector - 表格选择器</li>
 *   <li>@columns - 表格列配置</li>
 *   <li>@execute - 执行解析</li>
 *   <li>@clean - 数据清洗</li>
 *   <li>@transform - 数据转换</li>
 *   <li>@validate - 数据校验</li>
 *   <li>@table - 目标数据库表</li>
 *   <li>@mapping - 字段映射</li>
 *   <li>@store - 执行落库</li>
 *   <li>@log - 输出日志</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotCodeExecutor {

    private final CollectedDataRepository collectedDataRepository;
    private final InvoiceDataRepository invoiceDataRepository;
    private final RobotRepository robotRepository;
    private final CacheService cacheService;
    private final UniversalStoreService universalStoreService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Redis缓存前缀 */
    private static final String CONTEXT_PREFIX = "rpa:process:context:";

    /** 上下文过期时间（1小时） */
    private static final long CONTEXT_EXPIRE = 60;

    // ==================== 运行时状态 ====================

    /** 当前流程执行ID */
    private String processId = "default";

    /** 当前采集URL */
    private String targetUrl = null;

    /** 请求头配置 */
    private Map<String, String> headers = new HashMap<>();

    /** 原始HTML内容 */
    private String rawHtml = null;

    /** 企业信息 */
    private Map<String, String> companyInfo = new HashMap<>();

    /** 解析后的数据列表 */
    private List<Map<String, String>> parsedData = new ArrayList<>();

    /** 加工后的数据列表 */
    private List<Map<String, String>> processedData = new ArrayList<>();

    /** 采集记录ID */
    private Long collectDataId = null;

    // ==================== 解析配置 ====================

    /** CSS选择器配置 */
    private Map<String, String> selectors = new LinkedHashMap<>();

    /** 表格行选择器 */
    private String tableSelector = null;

    /** 表格列名 */
    private List<String> tableColumns = new ArrayList<>();

    /** 解析模式 */
    private String parseMode = "html"; // html, json, regex

    // ==================== 落库配置 ====================

    /** 目标表名 */
    private String targetTable = null;

    /** 字段映射 */
    private Map<String, String> fieldMapping = new LinkedHashMap<>();

    /**
     * 执行机器人代码
     *
     * @param robot 机器人实体
     * @param config 配置参数（包含流程上下文信息）
     * @return 执行结果
     */
    public Map<String, Object> execute(Robot robot, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        List<String> logs = new ArrayList<>();

        // 重置状态
        resetState();

        // 获取流程执行ID
        processId = config.getOrDefault("processId", "default").toString();
        log.info(">>> RobotCodeExecutor.execute() 开始");
        log.info(">>> processId: {}", processId);
        log.info(">>> robotId: {}, robotName: {}", robot.getId(), robot.getName());

        try {
            String code = robot.getRobotCode();
            log.info(">>> 机器人代码长度: {}", code != null ? code.length() : 0);
            if (code == null || code.trim().isEmpty()) {
                logs.add("[警告] 机器人代码为空");
                result.put("logs", logs);
                return result;
            }

            // 从Redis加载上下文数据
            log.info(">>> 加载Redis上下文...");
            loadContext();

            // 逐行解析并执行命令
            log.info(">>> 开始逐行解析机��人代码...");
            String[] lines = code.split("\n");
            int lineIndex = 0;
            for (String line : lines) {
                lineIndex++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                    continue;
                }
                log.info(">>> 第{}行: {}", lineIndex, line);
                logs.add("[执行] " + line);

                // 解析命令
                executeCommand(line, logs);
            }
            log.info(">>> 代码解析完成, 共处理 {} 行", lineIndex);

            // 保存上下文到Redis
            log.info(">>> 保存Redis上下文...");
            saveContext();

            result.put("success", true);
            result.put("dataCount", processedData.isEmpty() ? parsedData.size() : processedData.size());
            result.put("logs", logs);

            if (!companyInfo.isEmpty()) {
                result.put("companyInfo", companyInfo);
            }

            log.info(">>> RobotCodeExecutor.execute() 完成, dataCount: {}", processedData.isEmpty() ? parsedData.size() : processedData.size());

        } catch (Exception e) {
            log.error(">>> RobotCodeExecutor.execute() 异常: {}", e.getMessage(), e);
            logs.add("[错误] " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("logs", logs);
            log.error("机器人代码执行失败", e);
        }

        return result;
    }

    /**
     * 重置状态
     */
    private void resetState() {
        targetUrl = null;
        headers.clear();
        rawHtml = null;
        companyInfo.clear();
        parsedData.clear();
        processedData.clear();
        collectDataId = null;
        selectors.clear();
        tableSelector = null;
        tableColumns.clear();
        parseMode = "html";
        targetTable = null;
        fieldMapping.clear();
    }

    /**
     * 执行单条命令
     */
    private void executeCommand(String line, List<String> logs) {
        String lowerLine = line.toLowerCase();

        if (lowerLine.startsWith("@url ")) {
            // 设置采集URL
            targetUrl = line.substring(5).trim();
            logs.add("[配置] 目标URL: " + targetUrl);

        } else if (lowerLine.startsWith("@headers ")) {
            // 设置请求头
            String headerLine = line.substring(9).trim();
            String[] parts = headerLine.split("=");
            if (parts.length == 2) {
                headers.put(parts[0].trim(), parts[1].trim());
                logs.add("[配置] 请求头: " + parts[0] + "=" + parts[1]);
            }

        } else if (lowerLine.startsWith("@collect")) {
            // 执行采集
            log.info(">>> @collect 命令解析, line: {}", line);
            if (lowerLine.length() > 8) {
                // @collect 后面直接跟URL
                targetUrl = line.substring(8).trim();
                log.info(">>> 从 @collect 提取URL: {}", targetUrl);
                logs.add("[配置] 目标URL: " + targetUrl);
            }
            log.info(">>> doCollect 调用前, targetUrl: {}", targetUrl);
            logs.addAll(doCollect(logs));

        } else if (lowerLine.startsWith("@save ")) {
            // 保存数据到上下文
            String key = line.substring(6).trim();
            logs.add("[保存] 保存到上下文: " + key);

        } else if (lowerLine.startsWith("@load ")) {
            // 从上下文加载数据
            String key = line.substring(6).trim();
            logs.add("[加载] 从上下文加载: " + key);

        } else if (lowerLine.equals("@parse_html")) {
            parseMode = "html";
            logs.add("[解析] 切换到HTML解析模式");

        } else if (lowerLine.equals("@parse_json")) {
            parseMode = "json";
            logs.add("[解析] 切换到JSON解析模式");

        } else if (lowerLine.equals("@selector")) {
            // 等待后续配置行
            logs.add("[配置] 等待选择器配置...");

        } else if (lowerLine.startsWith("#") && !lowerLine.contains(" ")) {
            // 注释行，跳过
        } else if (lowerLine.startsWith("tax_no=") || lowerLine.startsWith("credit_code=") ||
                   lowerLine.startsWith("company_name=") || lowerLine.startsWith("company_type=") ||
                   lowerLine.startsWith("apply_date=")) {
            // 选择器配置行
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                selectors.put(parts[0].trim(), parts[1].trim());
                logs.add("[选择器] " + parts[0] + " -> " + parts[1]);
            }

        } else if (lowerLine.startsWith("@table_selector ")) {
            tableSelector = line.substring(15).trim();
            logs.add("[配置] 表格选择器: " + tableSelector);

        } else if (lowerLine.startsWith("@columns ")) {
            String cols = line.substring(9).trim();
            tableColumns.clear();
            for (String col : cols.split(",")) {
                tableColumns.add(col.trim());
            }
            logs.add("[配置] 表格列: " + tableColumns);

        } else if (lowerLine.equals("@parse")) {
            log.info(">>> 匹配到 @parse 命令");
            logs.addAll(doParse(logs));

        } else if (lowerLine.equals("@execute")) {
            log.info(">>> 匹配到 @execute 命令");
            logs.addAll(doParse(logs));

        } else if (lowerLine.startsWith("@process")) {
            log.info(">>> 匹配到 @process 命令");
            // 执行数据加工，支持 @process clean,transform,validate 格式
            String params = line.substring(8).trim();
            if (!params.isEmpty()) {
                for (String action : params.split(",")) {
                    String actionLower = action.trim().toLowerCase();
                    if (actionLower.equals("clean")) {
                        logs.addAll(doClean(logs));
                    } else if (actionLower.equals("transform")) {
                        logs.addAll(doTransform(logs));
                    } else if (actionLower.equals("validate")) {
                        logs.addAll(doValidate(logs));
                    }
                }
            } else {
                logs.addAll(doClean(logs));
                logs.addAll(doTransform(logs));
                logs.addAll(doValidate(logs));
            }

        } else if (lowerLine.startsWith("@store")) {
            log.info(">>> 匹配到 @store 命令");
            // 执行落库，支持 @store tableName 格式
            if (line.length() > 6) {
                targetTable = line.substring(6).trim();
                logs.add("[配置] 目标表: " + targetTable);
            }
            logs.addAll(doStore(logs));

        } else if (lowerLine.startsWith("@log ")) {
            String message = line.substring(5).trim();
            logs.add("[日志] " + message);

        } else if (lowerLine.equals("@log")) {
            logs.add("[日志] -");

        }
    }

    /**
     * 执行数据采集
     */
    private List<String> doCollect(List<String> logs) {
        List<String> result = new ArrayList<>();

        log.info(">>> doCollect 被调用, targetUrl: {}", targetUrl);

        if (targetUrl == null || targetUrl.isEmpty()) {
            result.add("[错误] 请先使用 @url 设置采集目标");
            log.error(">>> doCollect 失败: targetUrl为空");
            return result;
        }

        try {
            log.info(">>> doCollect 正在采集: {}", targetUrl);
            result.add("[采集] 正在采集: " + targetUrl);

            Document doc = Jsoup.connect(targetUrl)
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();

            rawHtml = doc.html();
            log.info(">>> doCollect HTML长度: {}", rawHtml.length());

            // 保存到数据库
            CollectedData collectData = new CollectedData();
            collectData.setRawData(rawHtml);
            collectData.setSourceUrl(targetUrl);
            collectData.setDataType("web");
            collectData.setCollectTime(LocalDateTime.now());
            collectData = collectedDataRepository.save(collectData);
            collectDataId = collectData.getId();
            log.info(">>> doCollect 数据已保存, ID: {}", collectDataId);

            result.add("[采集] 成功! HTML长度: " + rawHtml.length() + " 字符");
            result.add("[采集] 数据已保存, ID: " + collectDataId);

        } catch (Exception e) {
            log.error(">>> doCollect 异常: {}", e.getMessage(), e);
            result.add("[错误] 采集失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行数据解析
     */
    private List<String> doParse(List<String> logs) {
        List<String> result = new ArrayList<>();

        log.info(">>> doParse 被调用, rawHtml: {}", rawHtml != null ? rawHtml.length() + " 字符" : "null");

        if (rawHtml == null || rawHtml.isEmpty()) {
            result.add("[错误] 没有可解析的数据，请先执行采集");
            return result;
        }

        try {
            Document doc = Jsoup.parse(rawHtml);
            log.info(">>> doParse HTML解析成功");

            // 解析企业信息
            // 优先使用选择器配置
            if (!selectors.isEmpty()) {
                for (Map.Entry<String, String> entry : selectors.entrySet()) {
                    String key = entry.getKey();
                    String selector = entry.getValue();
                    Element el = doc.selectFirst(selector);
                    if (el != null) {
                        String value = el.text().trim();
                        if (el.hasClass("badge-info")) {
                            value = el.text();
                        }
                        companyInfo.put(key, value);
                        log.info(">>> doParse 选择器提取企业信息: {} = {}", key, value);
                    }
                }
                if (!companyInfo.isEmpty()) {
                    result.add("[解析] 企业信息: " + companyInfo.getOrDefault("company_name", "未知"));
                }
            }

            // 如果没有选择器配置或选择器未提取到数据，则自动提取企业信息
            if (companyInfo.isEmpty()) {
                log.info(">>> doParse 未通过选择器提取到企业信息，开始自动提取...");
                autoExtractCompanyInfo(doc);
                if (!companyInfo.isEmpty()) {
                    result.add("[解析] 企业信息: " + companyInfo.getOrDefault("company_name", "未知"));
                } else {
                    log.warn(">>> doParse autoExtractCompanyInfo 后 companyInfo 仍然为空！");
                    // 调试：打印HTML的部分内容
                    log.debug(">>> HTML内容（前500字符）: {}", rawHtml.substring(0, Math.min(500, rawHtml.length())));
                }
            }

            // 解析表格数据
            if (tableSelector != null && !tableColumns.isEmpty()) {
                Elements rows = doc.select(tableSelector);
                parsedData.clear();

                for (Element row : rows) {
                    Elements cells = row.select("td");
                    if (cells.size() >= tableColumns.size()) {
                        Map<String, String> record = new LinkedHashMap<>();
                        for (int i = 0; i < tableColumns.size(); i++) {
                            String value = cells.get(i).text().trim();
                            if (i >= 4) {
                                value = value.replace(",", "").replace("¥", "").trim();
                            }
                            record.put(tableColumns.get(i), value);
                        }
                        parsedData.add(record);
                    }
                }

                result.add("[解析] 表格数据: " + parsedData.size() + " 条");
            } else if (parsedData.isEmpty()) {
                // 没有配置选择器时，尝试自动提取表格
                log.info(">>> doParse 没有配置选择器，尝试自动提取表格");
                result.addAll(autoExtractTables(doc));
            }

            if (parsedData.isEmpty() && !companyInfo.isEmpty()) {
                result.add("[解析] 企业信息解析完成");
            }

            log.info(">>> doParse 完成, parsedData: {} 条, companyInfo: {} 个键", parsedData.size(), companyInfo.size());

        } catch (Exception e) {
            log.error(">>> doParse 异常: {}", e.getMessage(), e);
            result.add("[错误] 解析失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 自动提取HTML中的表格数据
     */
    private List<String> autoExtractTables(Document doc) {
        List<String> result = new ArrayList<>();

        // 尝试查找所有表格
        Elements tables = doc.select("table");
        log.info(">>> autoExtractTables 发现 {} 个表格", tables.size());

        if (tables.isEmpty()) {
            result.add("[解析] 未发现HTML表格，尝试提取其他结构化数据");
            // 尝试查找 div、dl 或其他结构化数据
            Element content = doc.selectFirst("body");
            if (content != null) {
                String text = content.text();
                if (!text.isEmpty()) {
                    // 尝试按行分割，提取键值对
                    String[] lines = text.split("\n");
                    int extracted = 0;
                    for (String line : lines) {
                        line = line.trim();
                        if (line.contains(":") || line.contains("：")) {
                            String[] parts = line.split("[:：]", 2);
                            if (parts.length == 2 && !parts[0].trim().isEmpty()) {
                                Map<String, String> record = new LinkedHashMap<>();
                                record.put("key", parts[0].trim());
                                record.put("value", parts[1].trim());
                                parsedData.add(record);
                                extracted++;
                            }
                        }
                    }
                    if (extracted > 0) {
                        result.add("[解析] 自动提取键值对: " + extracted + " 条");
                    }
                }
            }
        } else {
            // 有表格，自动提取
            for (Element table : tables) {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    // 尝试用第一行作为列名
                    Elements headerCells = rows.get(0).select("th, td");
                    List<String> columns = new ArrayList<>();
                    for (Element cell : headerCells) {
                        columns.add(cell.text().trim());
                    }

                    // 如果第一行不是列名，用默认列名
                    if (columns.isEmpty() || columns.get(0).isEmpty()) {
                        Elements firstRowCells = rows.get(0).select("td, th");
                        for (int i = 0; i < firstRowCells.size(); i++) {
                            columns.add("列" + (i + 1));
                        }
                    }

                    // 解析数据行
                    for (int i = 1; i < rows.size(); i++) {
                        Elements cells = rows.get(i).select("td, th");
                        if (cells.size() >= columns.size()) {
                            Map<String, String> record = new LinkedHashMap<>();
                            for (int j = 0; j < columns.size(); j++) {
                                String colName = columns.get(j);
                                if (colName == null || colName.isEmpty()) {
                                    colName = "列" + (j + 1);
                                }
                                record.put(colName, cells.get(j).text().trim());
                            }
                            parsedData.add(record);
                        }
                    }
                    result.add("[解析] 自动提取表格: " + (rows.size() - 1) + " 行 x " + columns.size() + " 列");
                    break; // 只处理第一个有数据的表格
                }
            }
        }

        // 自动提取企业信息（如果还没有配置选择器）
        if (companyInfo.isEmpty()) {
            autoExtractCompanyInfo(doc);
            if (!companyInfo.isEmpty()) {
                result.add("[解析] 企业信息: " + companyInfo.getOrDefault("company_name", "未知"));
            }
        }

        return result;
    }

    /**
     * 自动提取企业信息
     */
    private void autoExtractCompanyInfo(Document doc) {
        log.info(">>> autoExtractCompanyInfo 开始自动提取企业信息");

        // 策略1：根据常见的id提取
        Map<String, String> idMap = new HashMap<>();
        idMap.put("tax_no", "tax-no");
        idMap.put("credit_code", "credit-code");
        idMap.put("company_name", "company-name");
        idMap.put("apply_date", "apply-date");

        for (Map.Entry<String, String> entry : idMap.entrySet()) {
            String fieldKey = entry.getKey();
            String elementId = entry.getValue();

            Element el = doc.getElementById(elementId);
            if (el != null) {
                String value = el.text().trim();
                if (!value.isEmpty()) {
                    companyInfo.put(fieldKey, value);
                    log.info(">>> autoExtractCompanyInfo 通过id提取 {} = {}", fieldKey, value);
                }
            }
        }

        // 策略2：通过CSS选择器提取（如果策略1未找到）
        if (!companyInfo.containsKey("company_name")) {
            List<String> companySelectors = Arrays.asList(
                ".company-name", "#companyName", "#company_name",
                ".enterprise-name", ".corp-name", "span.company",
                "div.company h1", "h1.company"
            );
            for (String selector : companySelectors) {
                Element el = doc.selectFirst(selector);
                if (el != null) {
                    String value = el.text().trim();
                    if (!value.isEmpty() && value.length() < 100) {
                        companyInfo.put("company_name", value);
                        log.info(">>> autoExtractCompanyInfo 通过选择器提取 company_name = {}", value);
                        break;
                    }
                }
            }
        }

        if (!companyInfo.containsKey("credit_code")) {
            List<String> creditCodeSelectors = Arrays.asList(
                ".credit-code", "#creditCode", "#credit_code",
                ".uniform-social-credit-code", ".tax-id", ".taxId"
            );
            for (String selector : creditCodeSelectors) {
                Element el = doc.selectFirst(selector);
                if (el != null) {
                    String value = el.text().trim();
                    if (!value.isEmpty() && value.length() < 50) {
                        companyInfo.put("credit_code", value);
                        log.info(">>> autoExtractCompanyInfo 通过选择器提取 credit_code = {}", value);
                        break;
                    }
                }
            }
        }

        if (!companyInfo.containsKey("tax_no")) {
            List<String> taxNoSelectors = Arrays.asList(
                ".tax-no", "#taxNo", "#tax_no", ".tax-number",
                ".taxId", "#taxId"
            );
            for (String selector : taxNoSelectors) {
                Element el = doc.selectFirst(selector);
                if (el != null) {
                    String value = el.text().trim();
                    if (!value.isEmpty() && value.length() < 50) {
                        companyInfo.put("tax_no", value);
                        log.info(">>> autoExtractCompanyInfo 通过选择器提取 tax_no = {}", value);
                        break;
                    }
                }
            }
        }

        // 策略3：通过文本内容模式提取
        if (companyInfo.isEmpty() || companyInfo.size() < 2) {
            String text = doc.body().text();
            if (text != null && !text.isEmpty()) {
                // 提取企业名称（通常包含"公司"、"集团"等关键词）
                if (!companyInfo.containsKey("company_name")) {
                    Pattern companyPattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,}?(?:公司|集团|企业|中心|事务所))");
                    Matcher m = companyPattern.matcher(text);
                    if (m.find()) {
                        String companyName = m.group(1);
                        if (!companyName.isEmpty()) {
                            companyInfo.put("company_name", companyName);
                            log.info(">>> autoExtractCompanyInfo 通过文本模式提取 company_name = {}", companyName);
                        }
                    }
                }

                // 提取信用代码（18位数字/字母组合）
                if (!companyInfo.containsKey("credit_code")) {
                    Pattern creditCodePattern = Pattern.compile("[A-Z0-9]{18}");
                    Matcher m = creditCodePattern.matcher(text);
                    if (m.find()) {
                        String creditCode = m.group();
                        // 简单的校验：开头两位是登记管理部门代码
                        if (creditCode.matches("^[0-9A-Z]{18}$")) {
                            companyInfo.put("credit_code", creditCode);
                            log.info(">>> autoExtractCompanyInfo 通过文本模式提取 credit_code = {}", creditCode);
                        }
                    }
                }

                // 提取纳税人识别号（15-20位）
                if (!companyInfo.containsKey("tax_no")) {
                    Pattern taxNoPattern = Pattern.compile("(?:纳税人识别号|税务登记号)[：: ]*([A-Z0-9]{15,20})");
                    Matcher m = taxNoPattern.matcher(text);
                    if (m.find()) {
                        String taxNo = m.group(1);
                        companyInfo.put("tax_no", taxNo);
                        log.info(">>> autoExtractCompanyInfo 通过文本模式提取 tax_no = {}", taxNo);
                    }
                }
            }
        }

        // 策略4：提取企业类型（通过class badge-info 或其他常见位置）
        if (companyInfo.isEmpty() || !companyInfo.containsKey("company_type")) {
            Element badge = doc.selectFirst(".badge-info");
            if (badge != null) {
                String companyType = badge.text().trim();
                if (!companyType.isEmpty()) {
                    companyInfo.put("company_type", companyType);
                    log.info(">>> autoExtractCompanyInfo 提取 company_type = {}", companyType);
                }
            }

            // 尝试从文本中提取企业类型
            if (!companyInfo.containsKey("company_type")) {
                String[] typeKeywords = {"有限责任公司", "股份有限公司", "合伙企业", "个人独资企业", "个体工商户"};
                String text = doc.body().text();
                for (String keyword : typeKeywords) {
                    if (text.contains(keyword)) {
                        companyInfo.put("company_type", keyword);
                        log.info(">>> autoExtractCompanyInfo 从文本提取 company_type = {}", keyword);
                        break;
                    }
                }
            }
        }

        // 设置默认值
        companyInfo.putIfAbsent("company_type", "通用企业");
        companyInfo.putIfAbsent("apply_date", LocalDateTime.now().toString().substring(0, 10));

        log.info(">>> autoExtractCompanyInfo 完成，共提取 {} 个字段", companyInfo.size());
    }

    /**
     * 执行数据清洗
     */
    private List<String> doClean(List<String> logs) {
        List<String> result = new ArrayList<>();

        if (parsedData.isEmpty()) {
            result.add("[警告] 没有可清洗的数据");
            return result;
        }

        int cleaned = 0;
        for (Map<String, String> record : parsedData) {
            for (String key : new ArrayList<>(record.keySet())) {
                String val = record.get(key);
                if (val != null) {
                    String cleanedVal = val.trim();
                    // 规范化日期格式
                    if (key.contains("date")) {
                        cleanedVal = cleanedVal.replace("/", "-");
                    }
                    record.put(key, cleanedVal);
                    cleaned++;
                }
            }
        }

        result.add("[清洗] 清洗了 " + cleaned + " 个字段");
        return result;
    }

    /**
     * 执行数据转换
     */
    private List<String> doTransform(List<String> logs) {
        List<String> result = new ArrayList<>();

        if (parsedData.isEmpty()) {
            result.add("[警告] 没有可转换的数据");
            return result;
        }

        int transformed = 0;
        int skipped = 0;
        processedData.clear();

        log.info(">>> doTransform 开始转换, parsedData有 {} 条", parsedData.size());

        // 中文列名到英文字段名的映射
        Map<String, String> columnMapping = new HashMap<>();
        columnMapping.put("#", "invoiceIndex");
        columnMapping.put("序号", "invoiceIndex");
        columnMapping.put("号码", "invoiceNo");
        columnMapping.put("发票号", "invoiceNo");
        columnMapping.put("发票号码", "invoiceNo");
        columnMapping.put("类型", "invoiceType");
        columnMapping.put("发票类型", "invoiceType");
        columnMapping.put("状态", "invoiceStatus");
        columnMapping.put("发票状态", "invoiceStatus");
        columnMapping.put("日期", "invoiceDate");
        columnMapping.put("开票日期", "invoiceDate");
        columnMapping.put("不含税", "taxExclusiveAmount");
        columnMapping.put("不含税金额", "taxExclusiveAmount");
        columnMapping.put("净额", "taxExclusiveAmount");
        columnMapping.put("税额", "taxAmount");
        columnMapping.put("税额合计", "taxAmount");
        columnMapping.put("价税合计", "totalAmount");
        columnMapping.put("价税", "totalAmount");
        columnMapping.put("含税金额", "totalAmount");
        columnMapping.put("价税合计", "totalAmount");
        columnMapping.put("合计", "totalAmount");

        for (Map<String, String> record : parsedData) {
            Map<String, String> newRecord = new LinkedHashMap<>();

            // 调试：打印原始记录的所有key
            if (transformed == 0) {
                log.info(">>> doTransform 第1条原始记录 keys: {}", record.keySet());
            }

            // 列名转换
            for (Map.Entry<String, String> entry : record.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String mappedKey = columnMapping.getOrDefault(key, key);
                newRecord.put(mappedKey, value);
            }

            // 调试：打印转换后的第一条记录
            if (transformed == 0) {
                log.info(">>> doTransform 第1条转换后 keys: {}", newRecord.keySet());
            }

            // 添加企业信息
            String taxNo = companyInfo.getOrDefault("tax_no", "");
            String creditCode = companyInfo.getOrDefault("credit_code", "");
            String companyName = companyInfo.getOrDefault("company_name", "");
            String companyType = companyInfo.getOrDefault("company_type", "通用企业");
            String applyDate = companyInfo.getOrDefault("apply_date", "");

            newRecord.put("tax_no", taxNo);
            newRecord.put("credit_code", creditCode);
            newRecord.put("company_name", companyName);
            newRecord.put("company_type", companyType);
            newRecord.put("apply_date", applyDate);

            // 调试：打印企业信息
            if (transformed == 0) {
                log.info(">>> doTransform 企业信息: tax_no={}, company_name={}, credit_code={}", taxNo, companyName, creditCode);
            }

            // 计算价税合计（如果不准确的话）
            try {
                String taxExclusive = newRecord.getOrDefault("taxExclusiveAmount", "0").replace(",", "").replace("¥", "");
                String taxAmount = newRecord.getOrDefault("taxAmount", "0").replace(",", "").replace("¥", "");
                if (!taxExclusive.isEmpty() && !taxAmount.isEmpty() && !"0".equals(taxExclusive) && !"0".equals(taxAmount)) {
                    double total = Double.parseDouble(taxExclusive) + Double.parseDouble(taxAmount);
                    newRecord.put("calculated_total", String.format("%.2f", total));
                    transformed++;
                } else if (!taxExclusive.isEmpty() && !"0".equals(taxExclusive)) {
                    // 如果税额为空，直接使用不含税金额
                    newRecord.put("calculated_total", taxExclusive);
                    transformed++;
                } else {
                    skipped++;
                    log.warn(">>> doTransform 跳过记录，taxExclusive={}, taxAmount={}", taxExclusive, taxAmount);
                }
            } catch (NumberFormatException e) {
                skipped++;
                log.warn(">>> doTransform 跳过记录，数字格式异常: taxExclusive={}, taxAmount={}",
                    newRecord.getOrDefault("taxExclusiveAmount", ""), newRecord.getOrDefault("taxAmount", ""));
            }

            processedData.add(newRecord);
        }

        result.add("[转换] 转换了 " + transformed + " 条记录，跳过 " + skipped + " 条");

        // 调试：打印processedData的第一条记录
        if (!processedData.isEmpty()) {
            Map<String, String> first = processedData.get(0);
            log.info(">>> doTransform processedData[0] keys: {}", first.keySet());
            log.info(">>> doTransform processedData[0] invoiceNo={}, total={}",
                first.getOrDefault("invoiceNo", "NULL"), first.getOrDefault("calculated_total", "NULL"));
        }

        return result;
    }

    /**
     * 执行数据校验
     */
    private List<String> doValidate(List<String> logs) {
        List<String> result = new ArrayList<>();

        List<Map<String, String>> dataToCheck = processedData.isEmpty() ? parsedData : processedData;

        if (dataToCheck.isEmpty()) {
            result.add("[警告] 没有可校验的数据");
            return result;
        }

        int beforeCount = dataToCheck.size();

        // 调试：打印第一条记录的所有字段
        if (!dataToCheck.isEmpty()) {
            Map<String, String> first = dataToCheck.get(0);
            log.info(">>> doValidate 第一条记录的所有字段: {}", first);
            log.info(">>> doValidate 第一条记录关键字段 - invoiceNo: {}, totalAmount: {}, calculated_total: {}",
                first.get("invoiceNo"), first.get("totalAmount"), first.get("calculated_total"));
        }

        // 过滤无效数据
        dataToCheck.removeIf(r -> {
            String invoiceNo = r.getOrDefault("invoiceNo", "");
            String total = r.getOrDefault("totalAmount", r.getOrDefault("calculated_total", ""));
            boolean invalid = invoiceNo.isEmpty() || total.isEmpty() || "0".equals(total);
            if (invalid) {
                log.warn(">>> doValidate 过滤掉记录 - invoiceNo='{}', totalAmount='{}'", invoiceNo, total);
            }
            return invalid;
        });

        int removed = beforeCount - dataToCheck.size();
        result.add("[校验] 移除了 " + removed + " 条无效数据");
        result.add("[校验] 有效数据: " + dataToCheck.size() + " 条");

        return result;
    }

    /**
     * 执行数据落库
     */
    /**
     * 执行数据落库
     */
    private List<String> doStore(List<String> logs) {
        List<String> result = new ArrayList<>();

        List<Map<String, String>> dataToStore = processedData.isEmpty() ? parsedData : processedData;

        if (dataToStore.isEmpty()) {
            result.add("[错误] 没有可存储的数据");
            return result;
        }

        result.add("[落库] 目标表: " + (targetTable != null ? targetTable : "未指定"));
        result.add("[落库] 数据量: " + dataToStore.size() + " 条");

        if (targetTable == null || targetTable.isEmpty()) {
            result.add("[警告] 未指定目标表，跳过落库");
            return result;
        }

        try {
            int savedCount = 0;

            // 根据目标表调用不同的处理
            if ("invoice_data".equalsIgnoreCase(targetTable) || "invoice".equalsIgnoreCase(targetTable)) {
                // 发票数据：使用专门的repository
                result.addAll(storeInvoiceData(dataToStore));
            } else {
                // 其他表：使用通用落库服务
                log.info(">>> doStore 使用UniversalStoreService保存到表 {}", targetTable);

                // 创建灵活的字段映射
                Map<String, String> mapping = new HashMap<>();
                mapping.put("invoiceNo", "invoice_no");
                mapping.put("invoiceType", "invoice_type");
                mapping.put("invoiceStatus", "invoice_status");
                mapping.put("invoiceDate", "invoice_date");
                mapping.put("taxExclusiveAmount", "tax_exclusive_amount");
                mapping.put("taxAmount", "tax_amount");
                mapping.put("totalAmount", "total_amount");
                mapping.put("calculated_total", "total_amount");

                // 企业信息作为额外字段
                Map<String, String> extraInfo = new HashMap<>(companyInfo);
                extraInfo.put("collect_id", String.valueOf(collectDataId));
                extraInfo.put("collect_name", "RPA数据采集");
                extraInfo.put("source_url", targetUrl != null ? targetUrl : "");

                savedCount = universalStoreService.saveToTable(targetTable, dataToStore, mapping, extraInfo);

                if (savedCount > 0) {
                    result.add("[落库] 通用落库成功: " + savedCount + " 条");
                } else {
                    result.add("[警告] 通用落库可能失败，请检查日志");
                }
            }

        } catch (Exception e) {
            log.error(">>> doStore 异常: {}", e.getMessage(), e);
            result.add("[错误] 落库失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 存储发票数据
     */
    private List<String> storeInvoiceData(List<Map<String, String>> data) {
        List<String> result = new ArrayList<>();
        int savedCount = 0;

        try {
            for (Map<String, String> record : data) {
                try {
                    InvoiceData entity = createInvoiceDataEntity(record);
                    invoiceDataRepository.save(entity);
                    savedCount++;
                } catch (Exception e) {
                    result.add("[警告] 保存失败: " + e.getMessage());
                }
            }

            result.add("[落库] 发票数据保存成功: " + savedCount + " 条");

        } catch (Exception e) {
            result.add("[错误] 落库失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 创建发票数据实体
     */
    private InvoiceData createInvoiceDataEntity(Map<String, String> record) {
        InvoiceData entity = new InvoiceData();

        entity.setTaxNo(getFirstNonEmpty(record, "tax_no", "TaxNo"));
        entity.setCreditCode(getFirstNonEmpty(record, "credit_code", "CreditCode"));
        entity.setCompanyName(getFirstNonEmpty(record, "company_name", "CompanyName"));
        entity.setCompanyType(getFirstNonEmpty(record, "company_type", "CompanyType", "通用企业"));
        entity.setApplyDate(getFirstNonEmpty(record, "apply_date", "ApplyDate"));

        try {
            String indexStr = getFirstNonEmpty(record, "index", "invoiceIndex", "InvoiceIndex", "InvoiceIndex", "0");
            entity.setInvoiceIndex(Integer.parseInt(indexStr.replace(",", "")));
        } catch (Exception e) {
            entity.setInvoiceIndex(0);
        }

        entity.setInvoiceType(getFirstNonEmpty(record, "type", "invoiceType", "InvoiceType"));
        entity.setInvoiceStatus(getFirstNonEmpty(record, "status", "invoiceStatus", "InvoiceStatus", "正常"));
        entity.setInvoiceDate(getFirstNonEmpty(record, "date", "invoiceDate", "InvoiceDate"));

        String invoiceNo = getFirstNonEmpty(record, "invoiceNo", "invoice_no", "InvoiceNo", "InvoiceNo");
        entity.setInvoiceNo(invoiceNo);

        entity.setTaxExclusiveAmount(parseBigDecimal(getFirstNonEmpty(record, "taxExclusive", "tax_exclusive", "TaxExclusive", "0")));
        entity.setTaxAmount(parseBigDecimal(getFirstNonEmpty(record, "tax", "Tax", "0")));

        String total = getFirstNonEmpty(record, "calculated_total", "total", "Total", "0");
        entity.setTotalAmount(parseBigDecimal(total));

        entity.setCollectId(collectDataId);
        entity.setCollectName("企业发票数据采集");
        entity.setSourceUrl(targetUrl);
        entity.setCollectTime(LocalDateTime.now());

        return entity;
    }

    /**
     * 从多个可能的key中找到第一个非空值
     */
    private String getFirstNonEmpty(Map<String, String> record, String... keys) {
        for (String key : keys) {
            String value = record.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            String cleaned = value.replace(",", "").replace("¥", "").trim();
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ==================== Redis上下文管理 ====================

    /**
     * 从Redis加载上下文
     */
    private void loadContext() {
        String key = CONTEXT_PREFIX + processId;

        try {
            log.info(">>> loadContext 尝试加载Redis键: {}", key);
            String contextJson = cacheService.getRaw(key);
            log.info(">>> loadContext 原始JSON: {}", contextJson != null ? contextJson.substring(0, Math.min(200, contextJson.length())) : "null");

            if (contextJson != null) {
                Map<String, Object> context = objectMapper.readValue(contextJson, Map.class);

                rawHtml = (String) context.get("rawHtml");
                log.info(">>> loadContext rawHtml加载: {}", rawHtml != null ? rawHtml.length() + " 字符" : "null");

                Object collectId = context.get("collectDataId");
                if (collectId != null) {
                    collectDataId = ((Number) collectId).longValue();
                    log.info(">>> loadContext collectDataId: {}", collectDataId);
                }

                @SuppressWarnings("unchecked")
                Map<String, String> info = (Map<String, String>) context.get("companyInfo");
                if (info != null) {
                    companyInfo = info;
                    log.info(">>> loadContext companyInfo: {} 个键", info.size());
                }

                @SuppressWarnings("unchecked")
                List<Map<String, String>> data = (List<Map<String, String>>) context.get("parsedData");
                if (data != null) {
                    parsedData = data;
                    log.info(">>> loadContext parsedData: {} 条", data.size());
                }

                @SuppressWarnings("unchecked")
                List<Map<String, String>> processed = (List<Map<String, String>>) context.get("processedData");
                if (processed != null) {
                    processedData = processed;
                    log.info(">>> loadContext processedData: {} 条", processed.size());
                }
            } else {
                log.warn(">>> loadContext Redis键不存在: {}", key);
            }
        } catch (Exception e) {
            log.error(">>> loadContext 异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存上下文到Redis
     */
    private void saveContext() {
        String key = CONTEXT_PREFIX + processId;

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("rawHtml", rawHtml);
            context.put("companyInfo", companyInfo);
            context.put("parsedData", parsedData);
            context.put("processedData", processedData);
            context.put("collectDataId", collectDataId);

            cacheService.setRaw(key, objectMapper.writeValueAsString(context), CONTEXT_EXPIRE);
        } catch (JsonProcessingException e) {
            log.error("保存上下文失败: {}", e.getMessage());
        }
    }
}
