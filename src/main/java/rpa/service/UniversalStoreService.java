package rpa.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import rpa.entity.InvoiceData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 通用落库服务
 * 支持将处理后的数据灵活保存到不同类型的业务表中
 * 可根据配置动态创建表结构、字段映射和批量插入
 */
@Slf4j
@Service
public class UniversalStoreService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 将数据保存到指定表
     *
     * @param tableName  目标表名
     * @param records    待保存的记录列表（每条记录的key是字段名）
     * @param mapping    字段映射配置（key: 通用字段名，value: 目标表字段名）
     * @param extraInfo  额外信息（如企业信息等）
     * @return 保存的记录数
     */
    public int saveToTable(String tableName,
                           List<Map<String, String>> records,
                           Map<String, String> mapping,
                           Map<String, String> extraInfo) {
        if (records == null || records.isEmpty()) {
            log.warn(">>> UniversalStoreService: 没有可保存的数据");
            return 0;
        }

        try {
            // 1. 检查并创建表
            createTableIfNotExists(tableName, mapping);

            // 2. 构建插入SQL
            String sql = buildInsertSql(tableName, mapping, extraInfo);

            // 3. 批量插入
            int batchCount = 0;
            for (Map<String, String> record : records) {
                List<Object> params = buildInsertParams(record, mapping, extraInfo);
                jdbcTemplate.update(sql, params.toArray());
                batchCount++;
            }

            log.info(">>> UniversalStoreService 成功保存 {} 条记录到表 {}", batchCount, tableName);
            return batchCount;

        } catch (Exception e) {
            log.error(">>> UniversalStoreService 保存到表 {} 失败: {}", tableName, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 将发票数据保存到invoice_data表（兼容旧逻辑）
     */
    public int saveInvoiceData(List<Map<String, String>> records,
                               Map<String, String> companyInfo,
                               String collectId,
                               String collectName,
                               String sourceUrl) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("invoiceIndex", "invoice_index");
        mapping.put("invoiceNo", "invoice_no");
        mapping.put("invoiceType", "invoice_type");
        mapping.put("invoiceStatus", "invoice_status");
        mapping.put("invoiceDate", "invoice_date");
        mapping.put("taxExclusiveAmount", "tax_exclusive_amount");
        mapping.put("taxAmount", "tax_amount");
        mapping.put("totalAmount", "total_amount");
        mapping.put("calculated_total", "total_amount"); // 备用字段

        Map<String, String> extraInfo = new HashMap<>();
        extraInfo.put("collect_id", collectId);
        extraInfo.put("collect_name", collectName);
        extraInfo.put("source_url", sourceUrl);
        extraInfo.put("collect_time", LocalDateTime.now().toString());

        // 添加企业信息
        if (companyInfo != null) {
            extraInfo.putAll(companyInfo);
        }

        return saveToTable("invoice_data", records, mapping, extraInfo);
    }

    /**
     * 智能创建表（如果不存在）
     */
    private void createTableIfNotExists(String tableName, Map<String, String> mapping) {
        String checkSql = "SHOW TABLES LIKE ?";
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(checkSql, tableName);

        if (tables.isEmpty()) {
            log.info(">>> UniversalStoreService 表 {} 不存在，开始创建", tableName);
            String createSql = buildCreateTableSql(tableName, mapping);
            jdbcTemplate.execute(createSql);
            log.info(">>> UniversalStoreService 表 {} 创建成功", tableName);
        } else {
            log.info(">>> UniversalStoreService 表 {} 已存在，跳过创建", tableName);
        }
    }

    /**
     * 构建建表SQL（通用结构）
     */
    private String buildCreateTableSql(String tableName, Map<String, String> mapping) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(tableName).append("` (");
        sql.append("`id` BIGINT NOT NULL AUTO_INCREMENT,");
        sql.append("`create_time` DATETIME NOT NULL,");
        sql.append("`update_time` DATETIME,");

        // 根据mapping动态添加字段
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String fieldName = entry.getValue();
            sql.append("`").append(fieldName).append("` VARCHAR(255),");
        }

        sql.append("PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        return sql.toString();
    }

    /**
     * 构建插入SQL
     */
    private String buildInsertSql(String tableName, Map<String, String> mapping, Map<String, String> extraInfo) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO `").append(tableName).append("` (");

        Set<String> columns = new LinkedHashSet<>();
        columns.addAll(mapping.values());
        if (extraInfo != null) {
            columns.addAll(extraInfo.keySet());
        }
        columns.add("create_time");

        sql.append(String.join(",", columns.stream()
                .map(col -> "`" + col + "`")
                .toArray(String[]::new)));
        sql.append(") VALUES (");
        sql.append(String.join(",", Collections.nCopies(columns.size(), "?")));
        sql.append(")");

        return sql.toString();
    }

    /**
     * 构建插入参数列表
     */
    private List<Object> buildInsertParams(Map<String, String> record,
                                            Map<String, String> mapping,
                                            Map<String, String> extraInfo) {
        List<Object> params = new ArrayList<>();

        // 按列顺序添加record中的值
        for (String targetField : mapping.values()) {
            // 反向查找对应的sourceKey
            String sourceKey = findSourceKeyByTarget(mapping, targetField);
            String value = record.getOrDefault(sourceKey, "");
            params.add(value);
        }

        // 添加extraInfo中的值
        if (extraInfo != null) {
            for (String key : mapping.keySet()) { // 确保顺序一致
                String extraValue = extraInfo.get(key);
                params.add(extraValue != null ? extraValue : "");
            }
        }

        // 添加创建时间
        params.add(LocalDateTime.now().toString());

        return params;
    }

    /**
     * 反向查找源字段名
     */
    private String findSourceKeyByTarget(Map<String, String> mapping, String targetField) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (targetField.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return targetField;
    }

    /**
     * 通用字段映射（支持多种数据格式）
     */
    public static Map<String, String[]> createFlexibleMapping() {
        Map<String, String[]> mapping = new HashMap<>();
        mapping.put("invoiceNo", new String[]{"invoiceNo", "发票号", "号码", "invoice_no"});
        mapping.put("invoiceType", new String[]{"invoiceType", "发票类型", "类型", "invoice_type"});
        mapping.put("invoiceStatus", new String[]{"invoiceStatus", "发票状态", "状态", "invoice_status"});
        mapping.put("invoiceDate", new String[]{"invoiceDate", "开票日期", "日期", "invoice_date"});
        mapping.put("taxExclusiveAmount", new String[]{"taxExclusiveAmount", "不含税金额", "不含税", "tax_exclusive"});
        mapping.put("taxAmount", new String[]{"taxAmount", "税额", "税额合计", "tax_amount"});
        mapping.put("totalAmount", new String[]{"totalAmount", "价税合计", "总金额", "total_amount", "calculated_total"});
        return mapping;
    }
}
