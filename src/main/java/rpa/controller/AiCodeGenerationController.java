package rpa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rpa.dto.AiCodeGenerationRequest;
import rpa.dto.AiCodeGenerationResponse;
import rpa.service.AiCodeGenerationService;

import java.util.Map;

/**
 * AI代码生成控制器
 * 提供智能分析网页并生成RPA机器人代码的能力
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-code")
@CrossOrigin
public class AiCodeGenerationController {

    @Autowired
    private AiCodeGenerationService aiCodeService;

    /**
     * 分析网页结构并返回分析结果
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeWebPage(
            @RequestBody AiCodeGenerationRequest.AnalysisRequest request) {
        try {
            log.info(">>> AI代码生成: 分析网页: {}", request.getUrl());

            AiCodeGenerationService.WebPageAnalysis analysis = aiCodeService.analyzeWebPage(request.getUrl());

            AiCodeGenerationResponse.AnalysisResult result = new AiCodeGenerationResponse.AnalysisResult();
            result.setSuccess(true);
            result.setTitle(analysis.title);
            result.setTableDetected(analysis.tableDetected);
            result.setTableSelector(analysis.tableSelector);
            result.setColumns(analysis.columns);
            result.setCompanyInfoDetected(!analysis.companyInfo.isEmpty());
            result.setCompanyInfo(analysis.companyInfo);
            result.setHtmlSnippet(analysis.htmlSnippet);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(">>> AI代码生成: 分析网页失败: {}", e.getMessage(), e);
            AiCodeGenerationResponse.AnalysisResult result = new AiCodeGenerationResponse.AnalysisResult();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 生成完整的机器人代码
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateCode(
            @RequestBody AiCodeGenerationRequest.GenerationRequest request) {
        try {
            log.info(">>> AI代码生成: 生成代码 - 场景: {}, 表: {}", request.getScene(), request.getTargetTable());

            String generatedCode = aiCodeService.generateFullWorkflow(request);

            AiCodeGenerationResponse.CodeGenerationResult result = new AiCodeGenerationResponse.CodeGenerationResult();
            result.setSuccess(true);
            result.setCode(generatedCode);
            result.setExplanation(aiCodeService.generateExplanation(request));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(">>> AI代码生成: 生成代码失败: {}", e.getMessage(), e);
            AiCodeGenerationResponse.CodeGenerationResult result = new AiCodeGenerationResponse.CodeGenerationResult();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 智能识别表格列并生成列配置
     */
    @PostMapping("/detect-columns")
    public ResponseEntity<?> detectColumns(
            @RequestBody AiCodeGenerationRequest.ColumnDetectionRequest request) {
        try {
            log.info(">>> AI代码生成: 检测表格列 - URL: {}, 选择器: {}", request.getUrl(), request.getTableSelector());

            Map<String, String> detectedColumns = aiCodeService.detectTableColumns(
                request.getUrl(), 
                request.getTableSelector()
            );

            AiCodeGenerationResponse.ColumnDetectionResult result = new AiCodeGenerationResponse.ColumnDetectionResult();
            result.setSuccess(true);
            result.setColumns(detectedColumns);
            result.setColumnList(new java.util.ArrayList<>(detectedColumns.keySet()));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(">>> AI代码生成: 检测列失败: {}", e.getMessage(), e);
            AiCodeGenerationResponse.ColumnDetectionResult result = new AiCodeGenerationResponse.ColumnDetectionResult();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取可用的代码模板
     */
    @GetMapping("/templates")
    public ResponseEntity<?> getTemplates() {
        try {
            Map<String, String> templates = aiCodeService.getAvailableTemplates();
            AiCodeGenerationResponse.TemplateList result = new AiCodeGenerationResponse.TemplateList();
            result.setSuccess(true);
            result.setTemplates(templates);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(">>> AI代码生成: 获取模板失败: {}", e.getMessage(), e);
            AiCodeGenerationResponse.TemplateList result = new AiCodeGenerationResponse.TemplateList();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
