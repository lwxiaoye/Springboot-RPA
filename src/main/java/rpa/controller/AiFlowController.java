package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.service.AiFlowGeneratorService;
import rpa.service.AiFlowGeneratorService.*;

import java.util.List;
import java.util.Map;

/**
 * AI流程生成控制器
 *
 * 提供AI驱动的流程生成REST API：
 * - 自然语言转流程
 * - 流程优化建议
 * - 导出到设计器格式
 *
 * @author RPA System
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/flow")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class AiFlowController {

    private final AiFlowGeneratorService flowGeneratorService;

    /**
     * 根据描述生成流程
     *
     * @param request 包含描述和上下文
     * @return 生成的流程配置
     */
    @PostMapping("/generate")
    public Result generate(@RequestBody GenerateRequest request) {
        try {
            log.info("收到流程生成请求: {}", request.getDescription());

            Map<String, Object> context = request.getContext() != null ?
                request.getContext() : Map.of();

            FlowGenerationResult result = flowGeneratorService.generateFromDescription(
                request.getDescription(),
                context
            );

            if (result.isSuccess()) {
                return Result.success(Map.of(
                    "intent", result.getIntent(),
                    "stepCount", result.getSteps().size(),
                    "config", result.getConfig(),
                    "designerData", flowGeneratorService.exportToDesigner(result.getConfig())
                ));
            } else {
                return Result.fail(result.getMessage());
            }

        } catch (Exception e) {
            log.error("流程生成失败", e);
            return Result.fail("生成失败: " + e.getMessage());
        }
    }

    /**
     * 优化现有流程
     */
    @PostMapping("/optimize")
    public Result optimize(@RequestBody OptimizeRequest request) {
        try {
            FlowGenerationResult result = flowGeneratorService.optimizeFlow(
                request.getFlow(),
                request.getGoal()
            );

            if (result.isSuccess()) {
                return Result.success(Map.of(
                    "config", result.getConfig(),
                    "designerData", flowGeneratorService.exportToDesigner(result.getConfig())
                ));
            } else {
                return Result.fail(result.getMessage());
            }

        } catch (Exception e) {
            log.error("流程优化失败", e);
            return Result.fail("优化失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程优化建议
     */
    @PostMapping("/suggest")
    public Result suggest(@RequestBody SuggestRequest request) {
        try {
            List<String> suggestions = flowGeneratorService.analyzeAndSuggest(request.getFlow());
            return Result.success(Map.of("suggestions", suggestions));
        } catch (Exception e) {
            return Result.fail("分析失败: " + e.getMessage());
        }
    }

    /**
     * 继续对话生成
     */
    @PostMapping("/continue")
    public Result continueGenerate(@RequestBody ContinueRequest request) {
        try {
            FlowGenerationResult result = flowGeneratorService.continueFromContext(
                request.getContext(),
                request.getUserInput()
            );

            if (result.isSuccess()) {
                return Result.success(Map.of(
                    "config", result.getConfig(),
                    "designerData", flowGeneratorService.exportToDesigner(result.getConfig())
                ));
            } else {
                return Result.fail(result.getMessage());
            }

        } catch (Exception e) {
            return Result.fail("继续生成失败: " + e.getMessage());
        }
    }

    /**
     * 导出为设计器格式
     */
    @PostMapping("/export")
    public Result exportToDesigner(@RequestBody ExportRequest request) {
        try {
            Map<String, Object> designerData = flowGeneratorService.exportToDesigner(request.getFlow());
            return Result.success(Map.of("designerData", designerData));
        } catch (Exception e) {
            return Result.fail("导出失败: " + e.getMessage());
        }
    }

    /**
     * 预置模板列表
     */
    @GetMapping("/templates")
    public Result getTemplates() {
        List<Map<String, String>> templates = List.of(
            Map.of(
                "id", "web_scraping",
                "name", "网页数据抓取",
                "description", "自动抓取网页数据并保存到Excel",
                "icon", "Globe"
            ),
            Map.of(
                "id", "excel_automation",
                "name", "Excel自动化",
                "description", "批量处理Excel文件",
                "icon", "Document"
            ),
            Map.of(
                "id", "email_processing",
                "name", "邮件处理",
                "description", "自动收发邮件并分类",
                "icon", "Message"
            ),
            Map.of(
                "id", "data_sync",
                "name", "数据同步",
                "description", "跨系统数据同步",
                "icon", "Connection"
            ),
            Map.of(
                "id", "report_generation",
                "name", "报表生成",
                "description", "自动生成业务报表",
                "icon", "DataAnalysis"
            ),
            Map.of(
                "id", "form_filling",
                "name", "表单填写",
                "description", "批量填写在线表单",
                "icon", "Edit"
            ),
            Map.of(
                "id", "database_backup",
                "name", "数据库备份",
                "description", "定时备份数据库",
                "icon", "Box"
            ),
            Map.of(
                "id", "file_organization",
                "name", "文件整理",
                "description", "自动整理归类文件",
                "icon", "Folder"
            )
        );

        return Result.success(Map.of("templates", templates));
    }

    /**
     * 从模板快速生成
     */
    @PostMapping("/from-template")
    public Result generateFromTemplate(@RequestBody TemplateRequest request) {
        try {
            // 根据模板ID生成预设描述
            String description = getTemplateDescription(request.getTemplateId());
            String extraContext = request.getContext() != null ? request.getContext() : "";

            FlowGenerationResult result = flowGeneratorService.generateFromDescription(
                description + (extraContext.isEmpty() ? "" : " - " + extraContext),
                Map.of("templateId", request.getTemplateId())
            );

            if (result.isSuccess()) {
                return Result.success(Map.of(
                    "config", result.getConfig(),
                    "designerData", flowGeneratorService.exportToDesigner(result.getConfig())
                ));
            } else {
                return Result.fail(result.getMessage());
            }

        } catch (Exception e) {
            return Result.fail("模板生成失败: " + e.getMessage());
        }
    }

    private String getTemplateDescription(String templateId) {
        return switch (templateId) {
            case "web_scraping" -> "打开浏览器，导航到目标网站，提取页面数据（标题、价格、内容等），保存到Excel文件";
            case "excel_automation" -> "读取Excel文件，进行数据筛选、汇总计算，将结果写入新文件";
            case "email_processing" -> "登录邮箱，读取未读邮件，根据主题分类，回复或转发指定邮件";
            case "data_sync" -> "连接源数据库，查询数据，连接到目标数据库，执行插入或更新操作";
            case "report_generation" -> "从数据库读取业务数据，进行统计计算，生成Excel报表并发送邮件";
            case "form_filling" -> "打开网页表单，逐行填写数据（姓名、电话、地址等），提交表单";
            case "database_backup" -> "连接MySQL数据库，执行备份命令，将备份文件压缩并上传到云存储";
            case "file_organization" -> "扫描指定文件夹，按文件类型或日期分类，移动到对应目录";
            default -> "自动执行指定的数据处理任务";
        };
    }

    // ==================== 请求类 ====================

    @lombok.Data
    public static class GenerateRequest {
        private String description;         // 用户描述
        private Map<String, Object> context; // 上下文信息
    }

    @lombok.Data
    public static class OptimizeRequest {
        private FlowConfig flow;             // 现有流程
        private String goal;                // 优化目标
    }

    @lombok.Data
    public static class SuggestRequest {
        private FlowConfig flow;             // 要分析的流程
    }

    @lombok.Data
    public static class ContinueRequest {
        private String context;             // 对话上下文
        private String userInput;           // 用户输入
    }

    @lombok.Data
    public static class ExportRequest {
        private FlowConfig flow;             // 要导出的流程
    }

    @lombok.Data
    public static class TemplateRequest {
        private String templateId;          // 模板ID
        private String context;             // 额外上下文
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