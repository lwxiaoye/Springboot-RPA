package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rpa.service.ExcelService;
import rpa.service.ExcelService.*;

import java.util.List;
import java.util.Map;

/**
 * Excel操作控制器
 *
 * 提供Excel文件的REST API接口：
 * - 读取/分页读取
 * - 流式写入
 * - 模板生成
 * - 搜索筛选
 *
 * @author RPA System
 */
@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ExcelController {

    private final ExcelService excelService;

    // ==================== 读取接口 ====================

    /**
     * 读取Excel文件
     */
    @PostMapping("/read")
    public Result read(@RequestBody ReadRequest request) {
        try {
            ExcelReadResult result = excelService.read(
                request.getFilePath(),
                request.getSheetIndex(),
                request.getStartRow(),
                request.getPageSize()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("读取Excel失败", e);
            return Result.fail("读取失败: " + e.getMessage());
        }
    }

    /**
     * 上传并读取Excel
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file,
                        @RequestParam(value = "sheetIndex", defaultValue = "0") int sheetIndex,
                        @RequestParam(value = "startRow", defaultValue = "0") int startRow,
                        @RequestParam(value = "pageSize", defaultValue = "1000") int pageSize) {
        try {
            ExcelReadResult result = excelService.uploadAndRead(file, sheetIndex, startRow, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传读取失败", e);
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取Sheet信息
     */
    @GetMapping("/sheets")
    public Result getSheetInfo(@RequestParam String filePath) {
        try {
            List<SheetInfo> sheets = excelService.getSheetInfo(filePath);
            return Result.success(Map.of("sheets", sheets));
        } catch (Exception e) {
            return Result.fail("获取Sheet信息失败: " + e.getMessage());
        }
    }

    /**
     * 读取表头
     */
    @GetMapping("/headers")
    public Result readHeaders(@RequestParam String filePath) {
        try {
            List<String> headers = excelService.readHeaders(filePath);
            return Result.success(Map.of("headers", headers));
        } catch (Exception e) {
            return Result.fail("读取表头失败: " + e.getMessage());
        }
    }

    // ==================== 写入接口 ====================

    /**
     * 流式写入Excel
     */
    @PostMapping("/write")
    public Result write(@RequestBody WriteRequest request) {
        try {
            String filePath = excelService.writeStreaming(
                request.getHeaders(),
                request.getData(),
                request.getSheetName()
            );

            if (filePath != null) {
                return Result.success(Map.of("filePath", filePath));
            } else {
                return Result.fail("写入失败");
            }
        } catch (Exception e) {
            log.error("写入Excel失败", e);
            return Result.fail("写入失败: " + e.getMessage());
        }
    }

    /**
     * 追加写入
     */
    @PostMapping("/append")
    public Result append(@RequestBody AppendRequest request) {
        try {
            boolean success = excelService.appendToExcel(
                request.getFilePath(),
                request.getData(),
                request.getSheetIndex()
            );
            return Result.success(Map.of("success", success));
        } catch (Exception e) {
            return Result.fail("追加写入失败: " + e.getMessage());
        }
    }

    /**
     * 使用模板生成
     */
    @PostMapping("/template")
    public Result generateFromTemplate(@RequestBody TemplateRequest request) {
        try {
            boolean success = excelService.generateFromTemplate(
                request.getTemplatePath(),
                request.getData(),
                request.getOutputPath()
            );
            return Result.success(Map.of("success", success));
        } catch (Exception e) {
            return Result.fail("模板生成失败: " + e.getMessage());
        }
    }

    // ==================== 数据处理接口 ====================

    /**
     * 搜索数据
     */
    @PostMapping("/search")
    public Result search(@RequestBody SearchRequest request) {
        try {
            List<Map<String, Object>> results = excelService.search(
                request.getFilePath(),
                request.getKeyword(),
                request.getSheetIndex()
            );
            return Result.success(Map.of("results", results, "count", results.size()));
        } catch (Exception e) {
            return Result.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 筛选数据
     */
    @PostMapping("/filter")
    public Result filter(@RequestBody FilterRequest request) {
        try {
            List<Map<String, Object>> results = excelService.filter(
                request.getFilePath(),
                request.getSheetIndex(),
                request.getConditions(),
                request.getStartRow(),
                request.getPageSize()
            );
            return Result.success(Map.of("results", results, "count", results.size()));
        } catch (Exception e) {
            return Result.fail("筛选失败: " + e.getMessage());
        }
    }

    // ==================== 请求类 ====================

    @lombok.Data
    public static class ReadRequest {
        private String filePath;
        private int sheetIndex = 0;
        private int startRow = 0;
        private int pageSize = 1000;
    }

    @lombok.Data
    public static class WriteRequest {
        private List<String> headers;
        private List<List<Object>> data;
        private String sheetName = "Sheet1";
    }

    @lombok.Data
    public static class AppendRequest {
        private String filePath;
        private List<List<Object>> data;
        private int sheetIndex = 0;
    }

    @lombok.Data
    public static class TemplateRequest {
        private String templatePath;
        private Map<String, Object> data;
        private String outputPath;
    }

    @lombok.Data
    public static class SearchRequest {
        private String filePath;
        private String keyword;
        private int sheetIndex = 0;
    }

    @lombok.Data
    public static class FilterRequest {
        private String filePath;
        private int sheetIndex = 0;
        private Map<String, Object> conditions;
        private int startRow = 0;
        private int pageSize = 1000;
    }

    /**
     * 通用结果类
     */
    @lombok.Data
    public static class Result {
        private boolean success;
        private String message;
        private Object data;

        public static Result success(Object data) {
            Result r = new Result();
            r.setSuccess(true);
            r.setData(data);
            return r;
        }

        public static Result fail(String message) {
            Result r = new Result();
            r.setSuccess(false);
            r.setMessage(message);
            return r;
        }
    }
}