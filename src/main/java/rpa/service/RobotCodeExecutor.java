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

        try {
            String code = robot.getRobotCode();
            if (code == null || code.trim().isEmpty()) {
                logs.add("[警告] 机器人代码为空");
                result.put("logs", logs);
                return result;
            }

            // 从Redis加载上下文数据
            loadContext();

            // 逐行解析并执行命令
            String[] lines = code.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                    continue;
                }
                logs.add("[执行] " + line);

                // 解析命令
                executeCommand(line, logs);
            }

            // 保存上下文到Redis
            saveContext();

            result.put("success", true);
            result.put("dataCount", processedData.isEmpty() ? parsedData.size() : processedData.size());
            result.put("logs", logs);

            if (!companyInfo.isEmpty()) {
                result.put("companyInfo", companyInfo);
            }

        } catch (Exception e) {
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

        } else if (lowerLine.equals("@collect")) {
            // 执行采集
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
            logs.addAll(doParse(logs));

        } else if (lowerLine.equals("@execute")) {
            logs.addAll(doParse(logs));

        } else if (lowerLine.equals("@clean")) {
            logs.addAll(doClean(logs));

        } else if (lowerLine.equals("@transform")) {
            logs.addAll(doTransform(logs));

        } else if (lowerLine.equals("@validate")) {
            logs.addAll(doValidate(logs));

        } else if (lowerLine.startsWith("@table ")) {
            targetTable = line.substring(7).trim();
            logs.add("[配置] 目标表: " + targetTable);

        } else if (lowerLine.equals("@mapping")) {
            logs.add("[配置] 等待字段映射配置...");

        } else if (lowerLine.contains("=") && !lowerLine.startsWith("@")) {
            // 可能是字段映射配置
            String[] parts = line.split("=", 2);
            if (parts.length == 2 && parts[0].contains("_")) {
                fieldMapping.put(parts[0].trim(), parts[1].trim());
                logs.add("[映射] " + parts[0] + " -> " + parts[1]);
            }

        } else if (lowerLine.equals("@store")) {
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

        if (targetUrl == null || targetUrl.isEmpty()) {
            result.add("[错误] 请先使用 @url 设置采集目标");
            return result;
        }

        try {
            result.add("[采集] 正在采集: " + targetUrl);

            Document doc = Jsoup.connect(targetUrl)
                    .timeout(30000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();

            rawHtml = doc.html();

            // 保存到数据库
            CollectedData collectData = new CollectedData();
            collectData.setRawData(rawHtml);
            collectData.setSourceUrl(targetUrl);
            collectData.setDataType("web");
            collectData.setCollectTime(LocalDateTime.now());
            collectData = collectedDataRepository.save(collectData);
            collectDataId = collectData.getId();

            result.add("[采集] 成功! HTML长度: " + rawHtml.length() + " 字符");
            result.add("[采集] 数据已保存, ID: " + collectDataId);

        } catch (Exception e) {
            result.add("[错误] 采集失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行数据解析
     */
    private List<String> doParse(List<String> logs) {
        List<String> result = new ArrayList<>();

        if (rawHtml == null) {
            result.add("[错误] 没有可解析的数据，请先执行采集");
            return result;
        }

        try {
            Document doc = Jsoup.parse(rawHtml);

            // 解析企业信息（使用选择器）
            if (!selectors.isEmpty()) {
                for (Map.Entry<String, String> entry : selectors.entrySet()) {
                    String key = entry.getKey();
                    String selector = entry.getValue();
                    Element el = doc.selectFirst(selector);
                    if (el != null) {
                        String value = el.text().trim();
                        // 跳过badge类的元素文本
                        if (el.hasClass("badge-info")) {
                            value = el.text();
                        }
                        companyInfo.put(key, value);
                    }
                }
                result.add("[解析] 企业信息: " + companyInfo.getOrDefault("company_name", "未知"));
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
                            // 处理数字格式
                            if (i >= 4) { // 金额列
                                value = value.replace(",", "").replace("¥", "").trim();
                            }
                            record.put(tableColumns.get(i), value);
                        }
                        parsedData.add(record);
                    }
                }

                result.add("[解析] 表格数据: " + parsedData.size() + " 条");
            }

            if (parsedData.isEmpty() && !companyInfo.isEmpty()) {
                result.add("[解析] 企业信息解析完成");
            }

        } catch (Exception e) {
            result.add("[错误] 解析失败: " + e.getMessage());
        }

        return result;
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
        processedData.clear();

        for (Map<String, String> record : parsedData) {
            Map<String, String> newRecord = new LinkedHashMap<>(record);

            // 添加企业信息
            newRecord.put("tax_no", companyInfo.getOrDefault("tax_no", ""));
            newRecord.put("credit_code", companyInfo.getOrDefault("credit_code", ""));
            newRecord.put("company_name", companyInfo.getOrDefault("company_name", ""));
            newRecord.put("company_type", companyInfo.getOrDefault("company_type", ""));
            newRecord.put("apply_date", companyInfo.getOrDefault("apply_date", ""));

            // 计算价税合计（如果不准确的话）
            try {
                String taxExclusive = newRecord.getOrDefault("taxExclusive", "0").replace(",", "");
                String tax = newRecord.getOrDefault("tax", "0").replace(",", "");
                if (!taxExclusive.isEmpty() && !tax.isEmpty()) {
                    double total = Double.parseDouble(taxExclusive) + Double.parseDouble(tax);
                    newRecord.put("calculated_total", String.format("%.2f", total));
                    transformed++;
                }
            } catch (NumberFormatException e) {
                // 忽略
            }

            processedData.add(newRecord);
        }

        result.add("[转换] 转换了 " + transformed + " 条记录");
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

        // 过滤无效数据
        dataToCheck.removeIf(r -> {
            String invoiceNo = r.getOrDefault("invoiceNo", "");
            String total = r.getOrDefault("total", r.getOrDefault("calculated_total", ""));
            return invoiceNo.isEmpty() || total.isEmpty() || "0".equals(total);
        });

        int removed = beforeCount - dataToCheck.size();
        result.add("[校验] 移除了 " + removed + " 条无效数据");
        result.add("[校验] 有效数据: " + dataToCheck.size() + " 条");

        return result;
    }

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

        // 这里调用对应的Service来保存数据
        // 由于目标表可能不同，需要动态处理

        if (targetTable == null || targetTable.isEmpty()) {
            result.add("[警告] 未指定目标表，跳过落库");
            return result;
        }

        // 根据目标表调用不同的处理
        if ("invoice_data".equalsIgnoreCase(targetTable) || "invoice".equalsIgnoreCase(targetTable)) {
            result.addAll(storeInvoiceData(dataToStore));
        } else {
            result.add("[信息] 目标表 " + targetTable + " 的落库逻辑需要扩展");
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

        entity.setTaxNo(record.getOrDefault("tax_no", ""));
        entity.setCreditCode(record.getOrDefault("credit_code", ""));
        entity.setCompanyName(record.getOrDefault("company_name", ""));
        entity.setCompanyType(record.getOrDefault("company_type", "通用企业"));
        entity.setApplyDate(record.getOrDefault("apply_date", ""));

        try {
            String indexStr = record.getOrDefault("index", "0").replace(",", "");
            entity.setInvoiceIndex(Integer.parseInt(indexStr));
        } catch (Exception e) {
            entity.setInvoiceIndex(0);
        }

        entity.setInvoiceType(record.getOrDefault("type", ""));
        entity.setInvoiceStatus(record.getOrDefault("status", "正常"));
        entity.setInvoiceDate(record.getOrDefault("date", ""));

        String invoiceNo = record.getOrDefault("invoiceNo", "");
        if (invoiceNo.isEmpty()) {
            invoiceNo = record.getOrDefault("invoice_no", "");
        }
        entity.setInvoiceNo(invoiceNo);

        entity.setTaxExclusiveAmount(parseBigDecimal(record.getOrDefault("taxExclusive", "0")));
        entity.setTaxAmount(parseBigDecimal(record.getOrDefault("tax", "0")));

        String total = record.getOrDefault("calculated_total", record.getOrDefault("total", "0"));
        entity.setTotalAmount(parseBigDecimal(total));

        entity.setCollectId(collectDataId);
        entity.setCollectName("企业发票数据采集");
        entity.setSourceUrl(targetUrl);
        entity.setCollectTime(LocalDateTime.now());

        return entity;
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
            String contextJson = cacheService.getRaw(key);
            if (contextJson != null) {
                Map<String, Object> context = objectMapper.readValue(contextJson, Map.class);

                rawHtml = (String) context.get("rawHtml");
                Object collectId = context.get("collectDataId");
                if (collectId != null) {
                    collectDataId = ((Number) collectId).longValue();
                }

                @SuppressWarnings("unchecked")
                Map<String, String> info = (Map<String, String>) context.get("companyInfo");
                if (info != null) {
                    companyInfo = info;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, String>> data = (List<Map<String, String>>) context.get("parsedData");
                if (data != null) {
                    parsedData = data;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, String>> processed = (List<Map<String, String>>) context.get("processedData");
                if (processed != null) {
                    processedData = processed;
                }
            }
        } catch (Exception e) {
            log.warn("加载上下文失败: {}", e.getMessage());
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
