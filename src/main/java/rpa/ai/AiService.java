package rpa.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rpa.service.SystemConfigService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI服务统一入口
 * 
 * 功能说明：
 * - 提供OCR文字识别服务
 * - 提供身份证、发票、银行卡等证件识别
 * - 提供验证码识别
 * - 提供NLP文本处理
 * - 支持本地Tesseract和云端API双模式
 * 
 * @author RPA System
 */
@Slf4j
@Service
public class AiService {

    // AI服务类型
    public enum AiServiceType {
        OCR,              // 通用文字识别
        ID_CARD,          // 身份证识别
        INVOICE,          // 发票识别
        BANK_CARD,        // 银行卡识别
        CAPTCHA,          // 验证码识别
        TABLE,            // 表格识别
        NLP,              // 文本NLP
        TRANSLATE          // 翻译
    }

    // 识别模式
    public enum RecognitionMode {
        LOCAL,    // 本地Tesseract
        CLOUD     // 云端API（百度、腾讯等）
    }

    // 注入依赖服务
    private final OcrService ocrService;
    private final CaptchaService captchaService;
    private final NlpService nlpService;
    private final LlmService llmService;
    private final SystemConfigService systemConfigService;

    @Value("${rpa.ai.mode:local}")
    private String aiMode;  // local 或 cloud

    @Value("${rpa.ai.tesseract.path:/usr/bin/tesseract}")
    private String tesseractPath;

    // 百度AI配置（从数据库读取）
    private String baiduAppId;
    private String baiduApiKey;
    private String baiduSecretKey;

    public AiService(OcrService ocrService, CaptchaService captchaService, NlpService nlpService,
                     LlmService llmService, SystemConfigService systemConfigService) {
        this.ocrService = ocrService;
        this.captchaService = captchaService;
        this.nlpService = nlpService;
        this.llmService = llmService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 获取AI模式
     */
    public String getAiMode() {
        return systemConfigService.getConfig(SystemConfigService.KEY_OCR_PROVIDER, "baidu");
    }

    /**
     * 获取百度API配置
     */
    public Map<String, String> getBaiduConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("appId", systemConfigService.getConfig(SystemConfigService.KEY_OCR_APP_ID, ""));
        config.put("apiKey", systemConfigService.getConfig(SystemConfigService.KEY_OCR_API_KEY, ""));
        config.put("secretKey", systemConfigService.getConfig(SystemConfigService.KEY_OCR_SECRET_KEY, ""));
        return config;
    }

    /**
     * 检查OCR配置是否完整
     */
    public boolean isOcrConfigured() {
        String provider = getAiMode();
        if ("tesseract".equals(provider)) {
            return true;
        }
        Map<String, String> config = getBaiduConfig();
        return !config.get("apiKey").isEmpty() && !config.get("secretKey").isEmpty();
    }

    /**
     * 检查LLM是否已配置
     */
    public boolean isLlmConfigured() {
        return llmService.isConfigured();
    }

    /**
     * 获取LLM配置信息
     */
    public Map<String, Object> getLlmInfo() {
        Map<String, String> config = llmService.getConfig();
        Map<String, Object> info = new HashMap<>();
        info.put("provider", config.get("provider"));
        info.put("apiUrl", config.get("apiUrl"));
        info.put("model", config.get("model"));
        info.put("configured", llmService.isConfigured());
        return info;
    }

    /**
     * 获取LLM服务实例（供Controller使用）
     */
    public LlmService getLlmService() {
        return llmService;
    }

    /**
     * 执行AI识别
     */
    public AiResult recognize(AiRequest request) {
        log.info("执行AI识别: type={}, mode={}", request.getServiceType(), aiMode);
        
        AiResult result = new AiResult();
        result.setServiceType(request.getServiceType());
        result.setStartTime(System.currentTimeMillis());

        try {
            switch (request.getServiceType()) {
                case OCR:
                    result = ocrService.recognizeText(request);
                    break;
                case ID_CARD:
                    result = ocrService.recognizeIdCard(request);
                    break;
                case INVOICE:
                    result = ocrService.recognizeInvoice(request);
                    break;
                case BANK_CARD:
                    result = ocrService.recognizeBankCard(request);
                    break;
                case CAPTCHA:
                    result = captchaService.recognize(request);
                    break;
                case TABLE:
                    result = ocrService.recognizeTable(request);
                    break;
                case NLP:
                    result = nlpService.process(request);
                    break;
                case TRANSLATE:
                    result = nlpService.translate(request);
                    break;
                default:
                    result.setSuccess(false);
                    result.setErrorMessage("不支持的服务类型: " + request.getServiceType());
            }
        } catch (Exception e) {
            log.error("AI识别异常: type={}", request.getServiceType(), e);
            result.setSuccess(false);
            result.setErrorMessage("识别异常: " + e.getMessage());
        }

        result.setEndTime(System.currentTimeMillis());
        result.setDuration(result.getEndTime() - result.getStartTime());

        return result;
    }

    /**
     * 处理图片文件
     */
    public AiResult processImage(MultipartFile file, AiServiceType serviceType, Map<String, Object> params) {
        try {
            // 保存临时文件
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = UUID.randomUUID().toString() + ".png";
            Path tempPath = Path.of(tempDir, fileName);
            
            file.transferTo(tempPath.toFile());
            
            AiRequest request = new AiRequest();
            request.setServiceType(serviceType);
            request.setImagePath(tempPath.toString());
            request.setParams(params);
            
            AiResult result = recognize(request);
            
            // 清理临时文件
            Files.deleteIfExists(tempPath);
            
            return result;
        } catch (Exception e) {
            AiResult result = new AiResult();
            result.setSuccess(false);
            result.setErrorMessage("图片处理失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 处理Base64图片
     */
    public AiResult processBase64Image(String base64Image, AiServiceType serviceType, Map<String, Object> params) {
        try {
            // 解码Base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            
            // 保存临时文件
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = UUID.randomUUID().toString() + ".png";
            Path tempPath = Path.of(tempDir, fileName);
            
            Files.write(tempPath, imageBytes);
            
            AiRequest request = new AiRequest();
            request.setServiceType(serviceType);
            request.setImagePath(tempPath.toString());
            request.setParams(params);
            
            AiResult result = recognize(request);
            
            // 清理临时文件
            Files.deleteIfExists(tempPath);
            
            return result;
        } catch (Exception e) {
            AiResult result = new AiResult();
            result.setSuccess(false);
            result.setErrorMessage("Base64图片处理失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 获取支持的识别语言
     */
    public Map<String, Object> getSupportedLanguages() {
        Map<String, Object> languages = new HashMap<>();
        languages.put("ocr", new String[]{"chi_sim", "chi_tra", "eng", "jpn", "kor", "fra", "deu", "spa", "por", "rus"});
        languages.put("translate", new String[]{"zh", "en", "ja", "ko", "fr", "de", "es", "ru", "ar"});
        return languages;
    }

    // ==================== 内部类定义 ====================

    /**
     * AI识别请求
     */
    @Data
    public static class AiRequest {
        private AiServiceType serviceType;  // 服务类型
        private String imagePath;            // 图片路径
        private String imageUrl;             // 图片URL
        private String base64Image;          // Base64图片
        private Map<String, Object> params; // 额外参数
        private String language = "chi_sim+eng";  // 识别语言
        private Double confidence = 0.9;    // 置信度阈值
    }

    /**
     * AI识别结果
     */
    @Data
    public static class AiResult {
        private AiServiceType serviceType;
        private boolean success;
        private Map<String, Object> data;       // 识别结果数据
        private String text;                     // 识别的文本
        private Double confidence;               // 置信度
        private long duration;                   // 处理时长
        private long startTime;
        private long endTime;
        private String errorMessage;
        
        // 身份证专用字段
        private String name;
        private String idNumber;
        private String gender;
        private String ethnicity;
        private String birthDate;
        private String address;
        private String issueAuthority;
        private String validDate;
        private String side;  // front / back
        
        // 发票专用字段
        private String invoiceCode;
        private String invoiceNumber;
        private String invoiceDate;
        private String invoiceAmount;
        private String taxAmount;
        private String sellerName;
        private String buyerName;
        private String invoiceType;
    }
}