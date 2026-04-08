package rpa.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rpa.ai.AiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI能力控制器
 * 
 * 提供OCR、NLP等AI能力的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AiController {

    private final AiService aiService;

    /**
     * 通用文字识别（OCR）
     */
    @PostMapping("/ocr")
    public Map<String, Object> ocr(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "language", defaultValue = "chi_sim+eng") String language) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiRequest request = new AiService.AiRequest();
            request.setServiceType(AiService.AiServiceType.OCR);
            request.setLanguage(language);
            
            AiService.AiResult result = aiService.processImage(file, AiService.AiServiceType.OCR, Map.of("language", language));
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            log.error("OCR识别失败", e);
            response.put("code", 500);
            response.put("message", "OCR识别失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 身份证识别
     */
    @PostMapping("/idcard")
    public Map<String, Object> recognizeIdCard(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "side", defaultValue = "front") String side) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiResult result = aiService.processImage(file, AiService.AiServiceType.ID_CARD, Map.of("side", side));
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "身份证识别失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 发票识别
     */
    @PostMapping("/invoice")
    public Map<String, Object> recognizeInvoice(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiResult result = aiService.processImage(file, AiService.AiServiceType.INVOICE, null);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "发票识别失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 银行卡识别
     */
    @PostMapping("/bankcard")
    public Map<String, Object> recognizeBankCard(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiResult result = aiService.processImage(file, AiService.AiServiceType.BANK_CARD, null);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "银行卡识别失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 验证码识别
     */
    @PostMapping("/captcha")
    public Map<String, Object> recognizeCaptcha(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiResult result = aiService.processImage(file, AiService.AiServiceType.CAPTCHA, null);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "验证码识别失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 表格识别
     */
    @PostMapping("/table")
    public Map<String, Object> recognizeTable(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiResult result = aiService.processImage(file, AiService.AiServiceType.TABLE, null);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "表格识别失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 情感分析
     */
    @PostMapping("/sentiment")
    public Map<String, Object> analyzeSentiment(@RequestBody SentimentRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiRequest aiRequest = new AiService.AiRequest();
            aiRequest.setServiceType(AiService.AiServiceType.NLP);
            aiRequest.setParams(Map.of("text", request.getText(), "operation", "sentiment"));
            
            AiService.AiResult result = aiService.recognize(aiRequest);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "情感分析失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 文本翻译
     */
    @PostMapping("/translate")
    public Map<String, Object> translate(@RequestBody TranslateRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiRequest aiRequest = new AiService.AiRequest();
            aiRequest.setServiceType(AiService.AiServiceType.TRANSLATE);
            aiRequest.setParams(Map.of(
                "text", request.getText(),
                "from", request.getFrom() != null ? request.getFrom() : "auto",
                "to", request.getTo() != null ? request.getTo() : "zh"
            ));
            
            AiService.AiResult result = aiService.recognize(aiRequest);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "翻译失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 实体提取
     */
    @PostMapping("/entity")
    public Map<String, Object> extractEntities(@RequestBody EntityRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiRequest aiRequest = new AiService.AiRequest();
            aiRequest.setServiceType(AiService.AiServiceType.NLP);
            aiRequest.setParams(Map.of("text", request.getText(), "operation", "extract"));
            
            AiService.AiResult result = aiService.recognize(aiRequest);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "实体提取失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 关键词提取
     */
    @PostMapping("/keyword")
    public Map<String, Object> extractKeywords(@RequestBody KeywordRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiRequest aiRequest = new AiService.AiRequest();
            aiRequest.setServiceType(AiService.AiServiceType.NLP);
            aiRequest.setParams(Map.of(
                "text", request.getText(),
                "operation", "keyword",
                "topN", request.getTopN() != null ? request.getTopN() : 10
            ));
            
            AiService.AiResult result = aiService.recognize(aiRequest);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "关键词提取失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 文本分类
     */
    @PostMapping("/classify")
    public Map<String, Object> classifyText(@RequestBody ClassifyRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            AiService.AiRequest aiRequest = new AiService.AiRequest();
            aiRequest.setServiceType(AiService.AiServiceType.NLP);
            aiRequest.setParams(Map.of("text", request.getText(), "operation", "classify"));
            
            AiService.AiResult result = aiService.recognize(aiRequest);
            response.put("code", 0);
            response.put("data", convertAiResult(result));
            return response;
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "文本分类失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 获取支持的AI能力列表
     */
    @GetMapping("/capabilities")
    public Map<String, Object> getCapabilities() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> capabilities = new HashMap<>();
        
        capabilities.put("ocr", Map.of(
            "name", "文字识别",
            "description", "通用文字识别，支持多种语言"
        ));
        capabilities.put("idcard", Map.of(
            "name", "身份证识别",
            "description", "识别身份证正反面信息"
        ));
        capabilities.put("invoice", Map.of(
            "name", "发票识别",
            "description", "识别增值税发票信息"
        ));
        capabilities.put("bankcard", Map.of(
            "name", "银行卡识别",
            "description", "识别银行卡号和类型"
        ));
        capabilities.put("captcha", Map.of(
            "name", "验证码识别",
            "description", "识别图片验证码"
        ));
        capabilities.put("table", Map.of(
            "name", "表格识别",
            "description", "从图片中识别表格结构"
        ));
        capabilities.put("sentiment", Map.of(
            "name", "情感分析",
            "description", "分析文本情感倾向"
        ));
        capabilities.put("translate", Map.of(
            "name", "文本翻译",
            "description", "中英文互译"
        ));
        capabilities.put("entity", Map.of(
            "name", "实体提取",
            "description", "提取文本中的实体"
        ));
        capabilities.put("keyword", Map.of(
            "name", "关键词提取",
            "description", "提取文本关键词"
        ));
        
        response.put("code", 0);
        response.put("data", capabilities);
        return response;
    }

    /**
     * 转换AI识别结果
     */
    private Map<String, Object> convertAiResult(AiService.AiResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", result.isSuccess());
        map.put("serviceType", result.getServiceType() != null ? result.getServiceType().name() : null);
        map.put("text", result.getText());
        map.put("confidence", result.getConfidence());
        map.put("duration", result.getDuration());
        map.put("errorMessage", result.getErrorMessage());
        
        if (result.getData() != null) {
            map.put("data", result.getData());
        }
        
        return map;
    }

    // ==================== 请求类 ====================

    @Data
    public static class SentimentRequest {
        private String text;
    }

    @Data
    public static class TranslateRequest {
        private String text;
        private String from;
        private String to;
    }

    @Data
    public static class EntityRequest {
        private String text;
    }

    @Data
    public static class KeywordRequest {
        private String text;
        private Integer topN;
    }

    @Data
    public static class ClassifyRequest {
        private String text;
    }
}