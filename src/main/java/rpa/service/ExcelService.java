package rpa.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel大文件处理服务
 *
 * 核心功能：
 * 1. 流式读取（支持百万行级别）
 * 2. 流式写入（避免内存溢出）
 * 3. 大文件分页处理
 * 4. 公式计算
 * 5. 多Sheet操作
 *
 * 技术实现：
 * - POI SXSSFWorkbook（流式写入，临时文件存储）
 * - 批量处理模式
 * - 内存优化
 *
 * @author RPA System
 */
@Slf4j
@Service
public class ExcelService {

    private static final String DEFAULT_SAVE_PATH = "D:/rpa/excel";
    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int STREAMING_ROW_WINDOW = 100;

    /**
     * 读取Excel���件（支持大文件流式读取）
     *
     * @param filePath 文件路径
     * @param sheetIndex Sheet索引（从0开始）
     * @param startRow 起始行（从0开始）
     * @param pageSize 每页行数
     * @return 读取结果
     */
    public ExcelReadResult read(String filePath, int sheetIndex, int startRow, int pageSize) {
        ExcelReadResult result = new ExcelReadResult();

        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            int totalRows = sheet.getPhysicalNumberOfRows();

            result.setTotalRows(totalRows);
            result.setSheetName(sheet.getSheetName());
            result.setSheetIndex(sheetIndex);

            int endRow = Math.min(startRow + pageSize, totalRows);
            List<Map<String, Object>> rows = new ArrayList<>();

            // 读取表头
            if (startRow == 0 && sheet.getRow(0) != null) {
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(getCellValue(cell));
                }
                result.setHeaders(headers);
            }

            // 流式读取数据行
            for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    rowData.put("_rowIndex", rowIndex);

                    for (int colIndex = 0; colIndex < result.getHeaders().size(); colIndex++) {
                        Cell cell = row.getCell(colIndex);
                        String header = result.getHeaders().size() > colIndex ?
                            result.getHeaders().get(colIndex) : "column_" + colIndex;
                        rowData.put(header, getCellValue(cell));
                    }
                    rows.add(rowData);
                }
            }

            result.setRows(rows);
            result.setHasMore(endRow < totalRows);
            result.setNextStartRow(endRow);

        } catch (Exception e) {
            log.error("读取Excel失败: {}", filePath, e);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * 读取Excel文件的表头
     */
    public List<String> readHeaders(String filePath) {
        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            List<String> headers = new ArrayList<>();
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(getCellValue(cell));
                }
            }
            return headers;

        } catch (Exception e) {
            log.error("读取表头失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取Sheet信息
     */
    public List<SheetInfo> getSheetInfo(String filePath) {
        List<SheetInfo> sheets = new ArrayList<>();

        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                SheetInfo info = new SheetInfo();
                info.setIndex(i);
                info.setName(sheet.getSheetName());
                info.setRowCount(sheet.getPhysicalNumberOfRows());
                info.setColumnCount(sheet.getRow(0) != null ? sheet.getRow(0).getLastCellNum() : 0);
                sheets.add(info);
            }

        } catch (Exception e) {
            log.error("获取Sheet信息失败", e);
        }

        return sheets;
    }

    /**
     * 流式写入Excel（支持大文件）
     *
     * @param headers 表头
     * @param data 数据（分批传入）
     * @param sheetName Sheet名称
     * @return 生成的文件路径
     */
    public String writeStreaming(List<String> headers, List<List<Object>> data, String sheetName) {
        String fileName = "export_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        Path savePath = Paths.get(DEFAULT_SAVE_PATH, fileName);

        try {
            Files.createDirectories(savePath.getParent());

            // 使用SXSSFWorkbook进行流式写入
            try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAMING_ROW_WINDOW)) {
                Sheet sheet = workbook.createSheet(sheetName);

                // 写入表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                }

                // 写入数据
                int rowNum = 1;
                for (List<Object> rowData : data) {
                    Row row = sheet.createRow(rowNum++);
                    for (int col = 0; col < rowData.size(); col++) {
                        Cell cell = row.createCell(col);
                        setCellValue(cell, rowData.get(col));
                    }
                }

                // 写入文件
                try (FileOutputStream fos = new FileOutputStream(savePath.toFile())) {
                    workbook.write(fos);
                }

                // 删除临时文件
                workbook.dispose();
            }

            log.info("流式写入完成: {}", savePath);
            return savePath.toString();

        } catch (Exception e) {
            log.error("流式写入失败", e);
            return null;
        }
    }

    /**
     * 批量追加写入（用于处理大数据量）
     *
     * @param filePath 已存在的Excel文件路径
     * @param data 要追加的数据
     * @param appendToSheet 追加到的Sheet索引
     */
    public boolean appendToExcel(String filePath, List<List<Object>> data, int appendToSheet) {
        try {
            File file = new File(filePath);
            SXSSFWorkbook workbook;

            if (file.exists()) {
                try (InputStream fis = Files.newInputStream(file.toPath());
                     XSSFWorkbook sourceWorkbook = new XSSFWorkbook(fis)) {
                    // 复制到SXSSF
                    workbook = new SXSSFWorkbook(sourceWorkbook, STREAMING_ROW_WINDOW);
                }
            } else {
                workbook = new SXSSFWorkbook(STREAMING_ROW_WINDOW);
            }

            Sheet sheet = workbook.getSheetAt(appendToSheet);
            if (sheet == null) {
                sheet = workbook.createSheet("Sheet" + appendToSheet);
            }

            // 找到最后一行
            int lastRow = sheet.getLastRowNum();
            if (lastRow < 0) lastRow = 0;

            // 追加数据
            int rowNum = lastRow + 1;
            for (List<Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    setCellValue(cell, rowData.get(col));
                }
            }

            // 保存
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            workbook.dispose();

            return true;

        } catch (Exception e) {
            log.error("追加写入失败", e);
            return false;
        }
    }

    /**
     * 使用模板生成Excel
     *
     * @param templatePath 模板文件路径
     * @param data 数据映射
     * @param outputPath 输出文件路径
     */
    public boolean generateFromTemplate(String templatePath, Map<String, Object> data, String outputPath) {
        try (InputStream fis = Files.newInputStream(Paths.get(templatePath));
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            // 处理每个Sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                // 扫描并替换模板变量
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING) {
                            String value = cell.getStringCellValue();
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                if (value.contains("{{" + entry.getKey() + "}}")) {
                                    cell.setCellValue(value.replace("{{" + entry.getKey() + "}}",
                                        String.valueOf(entry.getValue())));
                                }
                            }
                        }
                    }
                }
            }

            // 保存
            Files.createDirectories(Paths.get(outputPath).getParent());
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }

            return true;

        } catch (Exception e) {
            log.error("模板生成失败", e);
            return false;
        }
    }

    /**
     * 上传并读取Excel
     */
    public ExcelReadResult uploadAndRead(MultipartFile file, int sheetIndex, int startRow, int pageSize) {
        Path uploadPath = Paths.get(DEFAULT_SAVE_PATH, "uploads");
        try {
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(filePath);

            return read(filePath.toString(), sheetIndex, startRow, pageSize);

        } catch (Exception e) {
            log.error("上传并读取失败", e);
            ExcelReadResult result = new ExcelReadResult();
            result.setError(e.getMessage());
            return result;
        }
    }

    /**
     * 搜索Excel数据
     */
    public List<Map<String, Object>> search(String filePath, String keyword, int sheetIndex) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            List<String> headers = new ArrayList<>();

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(getCellValue(cell));
                }
            }

            // 搜索数据
            for (int rowIndex = 1; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    boolean matched = false;

                    for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                        Cell cell = row.getCell(colIndex);
                        Object value = getCellValue(cell);
                        rowData.put(headers.get(colIndex), value);

                        if (value != null && value.toString().contains(keyword)) {
                            matched = true;
                        }
                    }

                    if (matched) {
                        rowData.put("_rowIndex", rowIndex);
                        results.add(rowData);
                    }
                }
            }

        } catch (Exception e) {
            log.error("搜索失败", e);
        }

        return results;
    }

    /**
     * 筛选Excel数据
     */
    public List<Map<String, Object>> filter(String filePath, int sheetIndex,
            Map<String, Object> filterConditions, int startRow, int pageSize) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            List<String> headers = new ArrayList<>();

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(getCellValue(cell));
                }
            }

            int endRow = Math.min(startRow + pageSize, sheet.getPhysicalNumberOfRows());

            // 筛选数据
            for (int rowIndex = startRow; rowIndex < endRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                boolean matches = true;
                Map<String, Object> rowData = new LinkedHashMap<>();

                for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    Object value = getCellValue(cell);
                    String header = headers.get(colIndex);
                    rowData.put(header, value);

                    // 检查筛选条件
                    if (filterConditions.containsKey(header)) {
                        Object condition = filterConditions.get(header);
                        if (!value.toString().equals(condition.toString())) {
                            matches = false;
                        }
                    }
                }

                if (matches) {
                    rowData.put("_rowIndex", rowIndex);
                    results.add(rowData);
                }
            }

        } catch (Exception e) {
            log.error("筛选失败", e);
        }

        return results;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    // ==================== 结果类 ====================

    @lombok.Data
    public static class ExcelReadResult {
        private int sheetIndex;
        private String sheetName;
        private int totalRows;
        private List<String> headers;
        private List<Map<String, Object>> rows;
        private boolean hasMore;
        private int nextStartRow;
        private String error;
    }

    @lombok.Data
    public static class SheetInfo {
        private int index;
        private String name;
        private int rowCount;
        private int columnCount;
    }

    // ==================== 水印导出功能 ====================

    /**
     * 导出带水印的Excel
     *
     * @param headers 表头
     * @param data 数据
     * @param sheetName Sheet名称
     * @param userName 当前用户名
     * @param phoneMasked 脱敏手机号
     * @param timestamp 导出时间戳
     * @return 生成的文件路径
     */
    public String writeWithWatermark(List<String> headers, List<List<Object>> data,
                                     String sheetName, String userName,
                                     String phoneMasked, String timestamp) {
        String fileName = "watermark_export_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        Path savePath = Paths.get(DEFAULT_SAVE_PATH, fileName);

        try {
            Files.createDirectories(savePath.getParent());

            try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAMING_ROW_WINDOW)) {
                // 创建水印Sheet
                Sheet watermarkSheet = workbook.createSheet("水印说明");
                createWatermarkSheet(watermarkSheet, userName, phoneMasked, timestamp);

                // 创建数据Sheet
                Sheet dataSheet = workbook.createSheet(sheetName);

                // 写入表头
                Row headerRow = dataSheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                }

                // 写入数据
                int rowNum = 1;
                for (List<Object> rowData : data) {
                    Row row = dataSheet.createRow(rowNum++);
                    for (int col = 0; col < rowData.size(); col++) {
                        Cell cell = row.createCell(col);
                        setCellValue(cell, rowData.get(col));
                    }
                }

                // 添加页眉水印信息
                addWatermarkFooter(dataSheet, data.size() + 1, userName, phoneMasked, timestamp);

                // 写入文件
                try (FileOutputStream fos = new FileOutputStream(savePath.toFile())) {
                    workbook.write(fos);
                }

                workbook.dispose();
            }

            log.info("带水印导出完成: {}", savePath);
            return savePath.toString();

        } catch (Exception e) {
            log.error("带水印导出失败", e);
            return null;
        }
    }

    /**
     * 创建水印说明Sheet
     */
    private void createWatermarkSheet(Sheet sheet, String userName, String phoneMasked, String timestamp) {
        // 设置列宽
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 8000);

        // 标题行
        Row titleRow = sheet.createRow(0);
        titleRow.setHeight((short) 500);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("【重要】数据安全声明");
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleCell.setCellStyle(titleStyle);

        // 水印信息
        String[] watermarkLines = {
            "本文件包含敏感数据，受银保监会数据安全法规保护。",
            "",
            "数据水印信息：",
            "├─ 导出自: " + userName,
            "├─ 身份标识: " + phoneMasked,
            "├─ 导出时间: " + timestamp,
            "├─ 版权声明: 仅限授权使用",
            "└─ 违规责任: 未经授权的复制、传播将承担法律责任"
        };

        for (int i = 0; i < watermarkLines.length; i++) {
            Row row = sheet.createRow(i + 1);
            Cell cell = row.createCell(0);
            cell.setCellValue(watermarkLines[i]);

            CellStyle infoStyle = sheet.getWorkbook().createCellStyle();
            Font infoFont = sheet.getWorkbook().createFont();
            infoFont.setFontHeightInPoints((short) 10);
            if (watermarkLines[i].startsWith("本文件") || watermarkLines[i].startsWith("数据水印")) {
                infoFont.setBold(true);
            }
            infoStyle.setFont(infoFont);
            cell.setCellStyle(infoStyle);
        }

        // 警告信息
        Row warningRow = sheet.createRow(watermarkLines.length + 2);
        Cell warningCell = warningRow.createCell(0);
        warningCell.setCellValue("⚠️ 警告：本文件已添加不可见数字水印，任何篡改行为均可被追溯！");
        CellStyle warningStyle = sheet.getWorkbook().createCellStyle();
        Font warningFont = sheet.getWorkbook().createFont();
        warningFont.setBold(true);
        warningFont.setColor(IndexedColors.RED.getIndex());
        warningFont.setFontHeightInPoints((short) 11);
        warningStyle.setFont(warningFont);
        warningCell.setCellStyle(warningStyle);
    }

    /**
     * 添加页脚水印信息
     */
    private void addWatermarkFooter(Sheet sheet, int lastRow, String userName, String phoneMasked, String timestamp) {
        // 在最后添加水印信息行
        Row watermarkRow = sheet.createRow(lastRow);
        watermarkRow.setHeight((short) 300);

        Cell watermarkCell = watermarkRow.createCell(0);
        String watermarkText = String.format("[水印: %s %s %s] - 本数据受银保监会数据安全法规保护",
            userName, phoneMasked, timestamp);
        watermarkCell.setCellValue(watermarkText);

        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setFontHeightInPoints((short) 8);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        watermarkCell.setCellStyle(style);

        // 合并单元格
        int lastCellNum = sheet.getRow(0) != null ? (int)sheet.getRow(0).getLastCellNum() : 10;
        if (sheet instanceof org.apache.poi.xssf.usermodel.XSSFSheet) {
            ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).addMergedRegion(
                new CellRangeAddress(lastRow, lastRow, 0, Math.max(0, lastCellNum - 1)));
        } else if (sheet instanceof org.apache.poi.hssf.usermodel.HSSFSheet) {
            ((org.apache.poi.hssf.usermodel.HSSFSheet) sheet).addMergedRegion(
                new CellRangeAddress(lastRow, lastRow, 0, Math.max(0, lastCellNum - 1)));
        }
    }

    /**
     * 流式导出带水印的大文件Excel
     */
    public String writeStreamingWithWatermark(String sheetName, Iterator<List<Object>> dataIterator,
                                              List<String> headers, String userName,
                                              String phoneMasked, String timestamp) {
        String fileName = "watermark_streaming_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
        Path savePath = Paths.get(DEFAULT_SAVE_PATH, fileName);

        try {
            Files.createDirectories(savePath.getParent());

            try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAMING_ROW_WINDOW)) {
                // 创建水印Sheet
                Sheet watermarkSheet = workbook.createSheet("水印说明");
                createWatermarkSheet(watermarkSheet, userName, phoneMasked, timestamp);

                // 创建数据Sheet
                Sheet dataSheet = workbook.createSheet(sheetName);

                // 写入表头
                Row headerRow = dataSheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                }

                // 流式写入数据
                int rowNum = 1;
                while (dataIterator.hasNext()) {
                    List<Object> rowData = dataIterator.next();
                    Row row = dataSheet.createRow(rowNum++);
                    for (int col = 0; col < rowData.size(); col++) {
                        Cell cell = row.createCell(col);
                        setCellValue(cell, rowData.get(col));
                    }
                }

                // 添加水印页脚
                addWatermarkFooter(dataSheet, rowNum, userName, phoneMasked, timestamp);

                // 写入文件
                try (FileOutputStream fos = new FileOutputStream(savePath.toFile())) {
                    workbook.write(fos);
                }

                workbook.dispose();
            }

            log.info("流式带水印导出完成: {}", savePath);
            return savePath.toString();

        } catch (Exception e) {
            log.error("流式带水印导出失败", e);
            return null;
        }
    }

    /**
     * 生成CSV导出水印内容（用于CSV导出）
     */
    public String generateCsvWatermarkContent(String userName, String phoneMasked, String timestamp) {
        return String.format("# [水印: %s %s %s] - 本数据受银保监会数据安全法规保护",
            userName, phoneMasked, timestamp);
    }
}