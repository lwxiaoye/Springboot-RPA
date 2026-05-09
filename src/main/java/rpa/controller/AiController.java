package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import rpa.ai.AiService;
import rpa.dto.AiCodeGenerationRequest;
import rpa.service.AiCodeGenerationService;
import rpa.service.SystemConfigService;

import java.util.*;

/**
 * AI代码生成控制器
 * 支持从数据库读取配置，调用智谱AI生成RPA机器人代码
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final SystemConfigService configService;
    private final RestTemplate restTemplate;
    private final AiService aiService;
    private final AiCodeGenerationService aiCodeGenerationService;

    // 配置键名
    private static final String KEY_LLM_PROVIDER = "llm_provider";
    private static final String KEY_LLM_API_URL = "llm_api_url";
    private static final String KEY_LLM_API_KEY = "llm_api_key";
    private static final String KEY_LLM_MODEL = "llm_model";

    /**
     * 生成RPA机器人代码
     * 调用真实AI API，根据用户需求生成可执行的机器人代码
     */
    @PostMapping("/generate-robot-code")
    public ResponseEntity<Map<String, Object>> generateRobotCode(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String robotType = request.getOrDefault("robotType", "DATA_COLLECT");
            String url = request.get("url");
            String targetTable = request.getOrDefault("targetTable", "data_table");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "提示词不能为空"
                ));
            }

            // 验证机器人类型
            if (!isValidRobotType(robotType)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "无效的机器人类型: " + robotType + "，可选值: DATA_COLLECT, DATA_PARSE, DATA_PROCESS, DATA_STORE"
                ));
            }

            // 从数据库获取AI配置
            String provider = configService.getConfig(KEY_LLM_PROVIDER);
            String apiUrl = configService.getConfig(KEY_LLM_API_URL);
            String apiKey = configService.getConfig(KEY_LLM_API_KEY);
            String model = configService.getConfig(KEY_LLM_MODEL);

            log.info("=== AI代码生成 ===");
            log.info("从数据库加载LLM配置: provider={}, model={}", provider, model);
            log.info("prompt: {}", prompt);
            log.info("robotType: {}", robotType);
            log.info("url: {}", url);
            log.info("targetTable: {}", targetTable);

            // 检查API配置
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", -1);
                result.put("message", "请先在系统设置 - 集成中心配置智谱AI API Key");
                return ResponseEntity.ok(result);
            }

            // 构建用户提示词 - 包含完整的上下文信息
            String userPrompt = buildUserPrompt(prompt, robotType, url, targetTable);

            // 调用AI API
            String systemPrompt = buildAiSystemPrompt(robotType);
            String generatedCode = callAiApiDirect(systemPrompt, userPrompt, provider, apiUrl, apiKey, model);

            // 规范化生成的代码（移除多余的引号等）
            String normalizedCode = aiCodeGenerationService.normalizeGeneratedCode(generatedCode);

            log.info("=== AI生成完成，代码长度: {} ===", normalizedCode.length());

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of(
                "code", normalizedCode,
                "robotType", robotType,
                "robotTypeName", getRobotTypeName(robotType),
                "targetTable", targetTable,
                "rawCode", generatedCode // 保留原始返回用于调试
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI代码生成失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "AI生成失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(String prompt, String robotType, String url, String targetTable) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求：").append(prompt).append("\n\n");

        if ("DATA_COLLECT".equals(robotType)) {
            sb.append("任务类型：数据采集机器人\n");
            if (url != null && !url.isEmpty()) {
                sb.append("采集目标URL：").append(url).append("\n");
            }
            sb.append("请生成数据采集机器人代码，采集网页HTML内容并存入Redis上下文。\n");
        } else if ("DATA_PARSE".equals(robotType)) {
            sb.append("任务类型：数据解析机器人\n");
            if (url != null && !url.isEmpty()) {
                sb.append("数据来源URL：").append(url).append("\n");
            }
            sb.append("请生成数据解析机器人代码，从Redis读取rawHtml并解析为结构化数据。\n");
        } else if ("DATA_PROCESS".equals(robotType)) {
            sb.append("任务类型：数据加工机器人\n");
            sb.append("请生成数据加工机器人代码，对parsedData进行清洗、转换、校验。\n");
        } else if ("DATA_STORE".equals(robotType)) {
            sb.append("任务类型：数据落库机器人\n");
            sb.append("目标表：").append(targetTable).append("\n");
            sb.append("请生成数据落库机器人代码，将processedData保存到数据库。\n");
        }

        sb.append("\n请只返回机器人代码命令，不要返回其他内容。");
        return sb.toString();
    }

    /**
     * 构建AI系统提示词
     */
    private String buildAiSystemPrompt(String robotType) {
        String typeName = getRobotTypeName(robotType);

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的RPA机器人代码生成助手，专门生成").append(typeName).append("。\n\n");
        sb.append("[核心要求]\n");
        sb.append("生成的代码必须能被RobotCodeExecutor正确解析执行，且必须能产生实际效果（采集数据、解析数据、加工数据、落库数据）。\n\n");
        sb.append("[必须遵循的命令格式]\n\n");
        sb.append("1. @collect URL\n");
        sb.append("   - 功能：从URL采集HTML内容\n");
        sb.append("   - 格式：@collect 后面直接跟URL，不要加引号、不要有空格\n");
        sb.append("   - 示例：@collect http://example.com/data\n\n");
        sb.append("2. @parse\n");
        sb.append("   - 功能：解析HTML/JSON/XML为结构化数据\n");
        sb.append("   - 格式：单独一行，不要加引号\n");
        sb.append("   - 示例：@parse\n\n");
        sb.append("3. @table_selector CSS选择器\n");
        sb.append("   - 功能：指定表格行的CSS选择器\n");
        sb.append("   - 格式：直接跟选择器，不要加引号\n");
        sb.append("   - 示例：@table_selector table tbody tr\n\n");
        sb.append("4. @columns 列名1,列名2,列名3\n");
        sb.append("   - 功能：配置表格列名\n");
        sb.append("   - 格式：用逗号分隔，不要加引号，不要用JSON数组\n");
        sb.append("   - 示例：@columns 发票号码,金额,日期\n\n");
        sb.append("5. @process 步骤1,步骤2\n");
        sb.append("   - 功能：数据处理步骤（清洗、转换、校验、去重）\n");
        sb.append("   - 格式：用逗号分隔\n");
        sb.append("   - 示例：@process clean,transform,validate\n\n");
        sb.append("6. @store 表名\n");
        sb.append("   - 功能：存储到数据库\n");
        sb.append("   - 格式：直接跟表名，不要加引号\n");
        sb.append("   - 示例：@store invoice_data\n\n");
        sb.append("7. @log 消息\n");
        sb.append("   - 功能：日志输出\n");
        sb.append("   - 格式：直接跟消息，不要加引号\n");
        sb.append("   - 示例：@log 采集完成\n\n");
        sb.append("[错误示例 - 禁止生成这种格式]\n");
        sb.append("@collect \"http://xxx.com\"        X 不能加引号\n");
        sb.append("@store \"table_name\"             X 不能加引号\n");
        sb.append("@columns [\"a\",\"b\"]              X 不能用JSON数组\n");
        sb.append("```python ... ```              X 不能生成Python代码\n\n");
        sb.append("[正确示例]\n");
        sb.append("@collect http://localhost:8081/spider_target\n");
        sb.append("@parse\n");
        sb.append("@table_selector table tbody tr\n");
        sb.append("@columns 号码,金额,日期,类型\n");
        sb.append("@process clean,transform,validate\n");
        sb.append("@store invoice_data\n");
        sb.append("@log 发票数据采集完成\n\n");
        sb.append("[重要] 对于解析机器人，必须包含：\n");
        sb.append("- @table_selector 选择器（指定要解析的表格行）\n");
        sb.append("- @columns 列名（指定要提取的列）\n");
        sb.append("- @parse 命令（触发解析操作）\n\n");
        sb.append("请生成完整、可执行的机器人代码。");

        return sb.toString();
    }

    /**
     * 批量生成分布式流水线机器人代码
     * 调用真实AI，一次生成4个阶段的机器人代码
     */
    @PostMapping("/generate-pipeline")
    public ResponseEntity<Map<String, Object>> generatePipeline(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String url = request.get("url");
            String targetTable = request.getOrDefault("targetTable", "data_table");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "提示词不能为空"
                ));
            }

            // 从数据库获取AI配置
            String provider = configService.getConfig(KEY_LLM_PROVIDER);
            String apiUrl = configService.getConfig(KEY_LLM_API_URL);
            String apiKey = configService.getConfig(KEY_LLM_API_KEY);
            String model = configService.getConfig(KEY_LLM_MODEL);

            log.info("=== AI批量生成流水线 ===");
            log.info("从数据库加载LLM配置: provider={}, model={}", provider, model);
            log.info("prompt: {}", prompt);
            log.info("url: {}", url);
            log.info("targetTable: {}", targetTable);

            // 检查API配置
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", -1);
                result.put("message", "请先在系统设置 - 集成中心配置智谱AI API Key");
                return ResponseEntity.ok(result);
            }

            // 为每个阶段生成代码
            List<Map<String, String>> robots = new ArrayList<>();
            String[] types = {"DATA_COLLECT", "DATA_PARSE", "DATA_PROCESS", "DATA_STORE"};
            String[] typeNames = {"数据采集", "数据解析", "数据加工", "数据落库"};

            for (int i = 0; i < types.length; i++) {
                try {
                    String stagePrompt = prompt + " - " + typeNames[i] + "阶段";
                    String userPrompt = buildUserPrompt(stagePrompt, types[i], url, targetTable);
                    String systemPrompt = buildAiSystemPrompt(types[i]);

                    String code = callAiApiDirect(systemPrompt, userPrompt, provider, apiUrl, apiKey, model);
                    String normalizedCode = aiCodeGenerationService.normalizeGeneratedCode(code);

                    Map<String, String> robot = new HashMap<>();
                    robot.put("type", types[i]);
                    robot.put("name", typeNames[i]);
                    robot.put("code", normalizedCode);
                    robots.add(robot);

                    log.info(">>> 阶段 {} 代码生成完成，长度: {}", typeNames[i], normalizedCode.length());
                } catch (Exception e) {
                    log.error(">>> 阶段 {} 代码生成失败: {}", typeNames[i], e.getMessage());
                    Map<String, String> robot = new HashMap<>();
                    robot.put("type", types[i]);
                    robot.put("name", typeNames[i]);
                    robot.put("code", "// 生成失败: " + e.getMessage());
                    robots.add(robot);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of(
                "robots", robots,
                "count", robots.size(),
                "targetTable", targetTable
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI流水线生成失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "AI生成失败: " + e.getMessage());
            result.put("data", null);
            return ResponseEntity.ok(result);
        }
    }

    private boolean isValidRobotType(String robotType) {
        return "DATA_COLLECT".equals(robotType) ||
               "DATA_PARSE".equals(robotType) ||
               "DATA_PROCESS".equals(robotType) ||
               "DATA_STORE".equals(robotType);
    }

    private String getRobotTypeName(String robotType) {
        switch (robotType) {
            case "DATA_COLLECT": return "数据采集机器人";
            case "DATA_PARSE": return "数据解析机器人";
            case "DATA_PROCESS": return "数据加工机器人";
            case "DATA_STORE": return "数据落库机器人";
            default: return "数据采集机器人";
        }
    }

    /**
     * 测试AI连接
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            String provider = configService.getConfig(KEY_LLM_PROVIDER);
            String apiUrl = configService.getConfig(KEY_LLM_API_URL);
            String apiKey = configService.getConfig(KEY_LLM_API_KEY);
            String model = configService.getConfig(KEY_LLM_MODEL);

            log.info("测试AI连接 - provider: {}, apiUrl: {}, model: {}", provider, apiUrl, model);

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "code", -1,
                    "message", "API Key未配置"
                ));
            }

            // 发送测试请求
            String testPrompt = "你好，请回复OK";
            callAiApiDirect("你是一个测试助手，请简单回复OK", testPrompt, provider, apiUrl, apiKey, model);

            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "AI连接成功"
            ));

        } catch (Exception e) {
            log.error("AI连接测试失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "code", -1,
                "message", "AI连接失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 优化提示词
     */
    @PostMapping("/optimize-prompt")
    public ResponseEntity<Map<String, Object>> optimizePrompt(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String category = request.getOrDefault("category", "GENERAL");

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "提示词不能为空"
                ));
            }

            // 从数据库获取大模型配置
            String provider = configService.getConfig(KEY_LLM_PROVIDER);
            String apiUrl = configService.getConfig(KEY_LLM_API_URL);
            String apiKey = configService.getConfig(KEY_LLM_API_KEY);
            String model = configService.getConfig(KEY_LLM_MODEL);

            // 检查API配置
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", -1);
                result.put("message", "请先在系统设置 - 集成中心配置智谱AI API Key");
                return ResponseEntity.ok(result);
            }

            // 调用智谱AI优化提示词
            String systemPrompt = "你是一个专业的RPA机器人代码助手。请根据用户的需求描述，优化和补充细节，使其更加清晰和具体。返回优化后的需求描述，保持简洁。";
            String userPrompt = "原始需求：" + prompt + "\n机器人分类：" + getCategoryName(category);

            String optimizedPrompt = callAiApiDirect(systemPrompt, userPrompt, provider, apiUrl, apiKey, model);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "success");
            result.put("data", Map.of("optimizedPrompt", optimizedPrompt));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI提示词优化失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("message", "优化失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * OCR文字识别
     */
    @PostMapping("/ocr")
    public ResponseEntity<Map<String, Object>> ocr(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "language", defaultValue = "chi_sim+eng") String language) {
        try {
            log.info("OCR识别请求 - 语言: {}, 文件大小: {} bytes", language, file.getSize());

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.OCR, language, null);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("text", result.getText() != null ? result.getText() : "");
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 0.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "识别成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("OCR识别失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "OCR识别失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 身份证识别
     */
    @PostMapping("/idcard")
    public ResponseEntity<Map<String, Object>> idCard(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "side", defaultValue = "front") String side) {
        try {
            log.info("身份证识别请求 - 方向: {}, 文件大小: {} bytes", side, file.getSize());

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.ID_CARD, side, side);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", result.getName() != null ? result.getName() : "");
                data.put("idNumber", result.getIdNumber() != null ? result.getIdNumber() : "");
                data.put("gender", result.getGender() != null ? result.getGender() : "");
                data.put("address", result.getAddress() != null ? result.getAddress() : "");
                data.put("birthDate", result.getBirthDate() != null ? result.getBirthDate() : "");
                data.put("ethnicity", result.getEthnicity() != null ? result.getEthnicity() : "");
                data.put("issueAuthority", result.getIssueAuthority() != null ? result.getIssueAuthority() : "");
                data.put("validDate", result.getValidDate() != null ? result.getValidDate() : "");
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 0.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "识别成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("身份证识别失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "身份证识别失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 发票识别
     */
    @PostMapping("/invoice")
    public ResponseEntity<Map<String, Object>> invoice(@RequestParam("file") MultipartFile file) {
        try {
            log.info("发票识别请求 - 文件大小: {} bytes", file.getSize());

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.INVOICE, null, null);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("invoiceCode", result.getInvoiceCode() != null ? result.getInvoiceCode() : "");
                data.put("invoiceNumber", result.getInvoiceNumber() != null ? result.getInvoiceNumber() : "");
                data.put("invoiceDate", result.getInvoiceDate() != null ? result.getInvoiceDate() : "");
                data.put("invoiceAmount", result.getInvoiceAmount() != null ? result.getInvoiceAmount() : "");
                data.put("taxAmount", result.getTaxAmount() != null ? result.getTaxAmount() : "");
                data.put("sellerName", result.getSellerName() != null ? result.getSellerName() : "");
                data.put("buyerName", result.getBuyerName() != null ? result.getBuyerName() : "");
                data.put("invoiceType", result.getInvoiceType() != null ? result.getInvoiceType() : "");
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 0.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "识别成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("发票识别失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "发票识别失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 验证码识别
     */
    @PostMapping("/captcha")
    public ResponseEntity<Map<String, Object>> captcha(@RequestParam("file") MultipartFile file) {
        try {
            log.info("验证码识别请求 - 文件大小: {} bytes", file.getSize());

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.CAPTCHA, null, null);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("text", result.getText() != null ? result.getText() : "");
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 0.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "识别成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("验证码识别失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "验证码识别失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 表格识别
     */
    @PostMapping("/table")
    public ResponseEntity<Map<String, Object>> table(@RequestParam("file") MultipartFile file) {
        try {
            log.info("表格识别请求 - 文件大小: {} bytes", file.getSize());

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.TABLE, null, null);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("text", result.getText() != null ? result.getText() : "");
                data.put("rows", result.getRows() != null ? result.getRows() : new java.util.ArrayList<>());
                data.put("data", result.getData() != null ? result.getData() : new HashMap<>());
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 0.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "识别成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("表格识别失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "表格识别失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 文档处理（PDF、Word）
     */
    @PostMapping("/document")
    public ResponseEntity<Map<String, Object>> document(@RequestParam("file") MultipartFile file) {
        try {
            log.info("文档处理请求 - 文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            // 检查文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".pdf")
                    && !filename.toLowerCase().endsWith(".docx")
                    && !filename.toLowerCase().endsWith(".doc"))) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", -1);
                response.put("message", "不支持的文件格式，仅支持PDF、DOC、DOCX格式");
                return ResponseEntity.ok(response);
            }

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.DOCUMENT, null, null);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("text", result.getText() != null ? result.getText() : "");
                data.put("pages", result.getPages() != null ? result.getPages() : 1);
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 0.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "文档处理成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("文档处理失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "文档处理失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 条码识别
     */
    @PostMapping("/barcode")
    public ResponseEntity<Map<String, Object>> barcode(@RequestParam("file") MultipartFile file) {
        try {
            log.info("条码识别请求 - 文件大小: {} bytes", file.getSize());

            AiService.AiResult result = aiService.processFile(file, AiService.AiServiceType.BARCODE, null, null);

            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("text", result.getText() != null ? result.getText() : "");
                data.put("format", result.getBarcodeFormat() != null ? result.getBarcodeFormat() : "UNKNOWN");
                data.put("confidence", result.getConfidence() != null ? result.getConfidence() : 1.0);
                data.put("duration", result.getDuration());

                response.put("code", 0);
                response.put("message", "识别成功");
                response.put("data", data);
            } else {
                response.put("code", -1);
                response.put("message", result.getErrorMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("条码识别失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", -1);
            response.put("message", "条码识别失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 调用AI API生成代码
     */
    private String callAiApi(String userPrompt, String category, String provider, String apiUrl, String apiKey, String model) {
        String systemPrompt = buildSystemPrompt(category);
        return callAiApiDirect(systemPrompt, userPrompt, provider, apiUrl, apiKey, model);
    }

    /**
     * 直接调用AI API
     */
    private String callAiApiDirect(String systemPrompt, String userPrompt, String provider, String apiUrl, String apiKey, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 根据不同提供商设置认证方式
        if ("zhipu".equalsIgnoreCase(provider) || "智谱".equals(provider)) {
            headers.setBearerAuth(apiKey);
            // 智谱的API地址需要确保以/chat/completions结尾
            if (apiUrl != null && !apiUrl.endsWith("/chat/completions")) {
                apiUrl = apiUrl.endsWith("/") ? apiUrl + "chat/completions" : apiUrl + "/chat/completions";
            }
            if (model == null || model.isEmpty()) {
                model = "glm-4-flash";
            }
        } else if ("openai".equalsIgnoreCase(provider)) {
            headers.setBearerAuth(apiKey);
            if (model == null || model.isEmpty()) {
                model = "gpt-3.5-turbo";
            }
        } else if ("qwen".equalsIgnoreCase(provider) || "通义千问".equals(provider)) {
            headers.set("Authorization", "Bearer " + apiKey);
            if (model == null || model.isEmpty()) {
                model = "qwen-turbo";
            }
        } else {
            // 默认使用智谱格式
            headers.setBearerAuth(apiKey);
            if (model == null || model.isEmpty()) {
                model = "glm-4-flash";
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 2000);
        requestBody.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("调用AI API - provider: {}, url: {}, model: {}", provider, apiUrl, model);

        ResponseEntity<Map> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            entity,
            Map.class
        );

        if (response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");
                log.info("AI返回内容长度: {}", content != null ? content.length() : 0);
                return content;
            }
        }

        throw new RuntimeException("AI返回内容为空");
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String category) {
        String categoryDesc = getCategoryName(category);

        return """
            你是一个专业的RPA机器人代码生成助手。请根据用户的需求生成RPA机器人执行代码。

            机器人分类：""" + categoryDesc + """

            【重要】请严格遵循以下命令格式，生成的代码必须能被RPA解析器正确执行：

            1. @collect URL
               - 功能：采集网页内容
               - 格式：@collect 后面直接跟URL，不要加引号
               - 示例：@collect http://example.com/data

            2. @parse
               - 功能：解析HTML/JSON/XML内容
               - 格式：单独一行，不要加引号
               - 示例：@parse

            3. @columns 列名1,列名2,列名3
               - 功能：配置表格列名
               - 格式：用逗号分隔，不要加引号，不要用JSON数组格式
               - 示例：@columns 发票号码,金额,日期,税率

            4. @store 表名
               - 功能：存储到数据库
               - 格式：直接跟表名，不要加引号
               - 示例：@store invoice_data

            5. @process 步骤1,步骤2
               - 功能：数据处理步骤
               - 格式：用逗号分隔，支持：clean,transform,validate,dedup
               - 示例：@process clean,transform,validate

            6. @log 消息内容
               - 功能：日志输出
               - 格式：直接跟消息，不要加引号
               - 示例：@log 采集完成

            【错误示例 - 不要生成这种格式】：
            @collect "http://xxx.com"      ❌ 不要加引号
            @store "table_name"           ❌ 不要加引号
            @columns ["a","b","c"]         ❌ 不要用JSON数组
            @parse_rule "xxx"             ❌ 不要加引号

            【正确示例】：
            @collect http://xxx.com/data
            @parse
            @columns 发票号码,金额,日期
            @store invoice_data
            @process clean,transform
            @log 数据采集完成

            请生成简洁的RPA机器人代码，只返回命令，不要生成Python代码或其他编程语言代码。
            """;
    }

    /**
     * 获取分类名称
     */
    private String getCategoryName(String category) {
        Map<String, String> categories = Map.of(
            "DATA_COLLECT", "数据采集",
            "DATA_PARSE", "数据解析",
            "DATA_PROCESS", "数据加工",
            "GENERAL", "通用执行"
        );
        return categories.getOrDefault(category, "通用执行");
    }

    /**
     * 备用代码（当API不可用时）
     */
    private String getFallbackCode(String prompt, String category) {
        String categoryDesc = getCategoryName(category);
        return "// AI生成的RPA机器人\n" +
               "// 分类: " + categoryDesc + "\n" +
               "// 需求: " + prompt + "\n\n" +
               "@collect http://example.com/data\n" +
               "@table_selector table.data-list tbody tr\n" +
               "@columns 列1,列2,列3\n" +
               "@process clean,transform\n" +
               "@store report_data\n" +
               "@log 任务执行完成\n\n" +
               "// 注意：请先在系统设置 - 集成中心配置智谱AI API Key以获得AI生成的代码";
    }
}
