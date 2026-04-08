package rpa.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rpa.service.SystemConfigService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR识别服务
 *
 * @author RPA System
 */
@Slf4j
@Service
public class OcrService {

    @Value("${rpa.ai.tesseract.path:/usr/bin/tesseract}")
    private String tesseractPath;

    @Value("${rpa.ai.mode:local}")
    private String mode;

    private final SystemConfigService systemConfigService;

    public OcrService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 通用文字识别
     */
    public AiService.AiResult recognizeText(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            String provider = systemConfigService.getConfig(SystemConfigService.KEY_OCR_PROVIDER, "baidu");

            if ("tesseract".equals(provider)) {
                return recognizeTextByLocal(request);
            } else {
                // 默认使用百度OCR
                return recognizeTextByBaidu(request);
            }
        } catch (Exception e) {
            log.error("OCR文字识别失败", e);
            result.setSuccess(false);
            result.setErrorMessage("识别失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 百度OCR识别
     */
    private AiService.AiResult recognizeTextByBaidu(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            String apiKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_API_KEY, "");
            String secretKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_SECRET_KEY, "");

            log.info("OCR识别开始 - API Key长度: {}, Secret Key长度: {}",
                    apiKey != null ? apiKey.length() : 0,
                    secretKey != null ? secretKey.length() : 0);

            if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR API密钥未配置，请在系统设置 → OCR服务中配置");
                log.error("OCR API密钥为空 - API Key: {}, Secret Key: {}", apiKey, secretKey);
                return result;
            }

            // 获取access token
            log.info("正在获取百度Access Token...");
            String accessToken = getBaiduAccessToken(apiKey, secretKey);
            log.info("获取Access Token结果: {}", accessToken != null ? "成功，长度=" + accessToken.length() : "失败");

            if (accessToken == null || accessToken.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("获取百度OCR access_token失败，请检查API密钥是否正确");
                return result;
            }

            // 读取图片文件
            File imageFile = new File(request.getImagePath());
            log.info("读取图片文件: {}, 大小: {} bytes", imageFile.getAbsolutePath(), imageFile.length());
            byte[] imageData = Files.readAllBytes(imageFile.toPath());

            // 调用百度通用文字识别API
            String baiduUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic"
                    + "?access_token=" + accessToken;

            URL url = new URL(baiduUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            // 将图片转换为Base64并发送
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);
            String params = "image=" + URLEncoder.encode(imageBase64, "UTF-8");

            conn.getOutputStream().write(params.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR API返回错误码: " + responseCode);
                return result;
            }

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 解析响应JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response.toString(), Map.class);

            // 检查错误
            if (responseMap.containsKey("error_code")) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR错误: " + responseMap.get("error_msg"));
                return result;
            }

            // 解析文字结果
            List<Map<String, Object>> wordsResult = (List<Map<String, Object>>) responseMap.get("words_result");
            StringBuilder textBuilder = new StringBuilder();
            double totalConfidence = 0;
            int validWords = 0;

            if (wordsResult != null) {
                for (Map<String, Object> wordInfo : wordsResult) {
                    String words = (String) wordInfo.get("words");
                    textBuilder.append(words).append("\n");

                    if (wordInfo.containsKey("probability")) {
                        Map<String, Object> prob = (Map<String, Object>) wordInfo.get("probability");
                        double avg = ((Number) prob.get("average")).doubleValue();
                        totalConfidence += avg;
                        validWords++;
                    }
                }
            }

            String text = textBuilder.toString().trim();
            double confidence = validWords > 0 ? totalConfidence / validWords / 100.0 : 0.85;

            result.setSuccess(true);
            result.setText(text);
            result.setConfidence(confidence);

            Map<String, Object> data = new HashMap<>();
            data.put("text", text);
            data.put("confidence", confidence);
            data.put("language", request.getLanguage());
            data.put("wordsCount", wordsResult != null ? wordsResult.size() : 0);
            result.setData(data);

            log.info("百度OCR识别成功，识别文字{}个字符", text.length());

        } catch (Exception e) {
            log.error("百度OCR识别异常", e);
            result.setSuccess(false);
            result.setErrorMessage("OCR识别异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取百度Access Token
     */
    private String getBaiduAccessToken(String apiKey, String secretKey) {
        try {
            String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token"
                    + "?grant_type=client_credentials"
                    + "&client_id=" + apiKey
                    + "&client_secret=" + secretKey;

            URL url = new URL(tokenUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.error("获取百度token失败，HTTP状态码: {}", responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(response.toString(), Map.class);

            if (result.containsKey("access_token")) {
                return (String) result.get("access_token");
            } else {
                log.error("获取百度token失败: {}", result.get("error_description"));
                return null;
            }

        } catch (Exception e) {
            log.error("获取百度Access Token异常", e);
            return null;
        }
    }

    /**
     * 本地Tesseract识别（需要Windows上安装Tesseract）
     */
    private AiService.AiResult recognizeTextByLocal(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            String imagePath = request.getImagePath();
            String language = request.getLanguage();

            // 检查Tesseract是否存在
            File tesseractExe = new File(tesseractPath + (tesseractPath.contains("tesseract") ? "" : "/tesseract"));
            if (!tesseractExe.exists() && !new File(tesseractPath).exists()) {
                result.setSuccess(false);
                result.setErrorMessage("未��测到Tesseract OCR，请配置百度OCR或在Windows上安装Tesseract");
                log.error("Tesseract未安装或路径错误: {}", tesseractPath);
                return result;
            }

            // 预处理图片
            String processedImage = preprocessImage(imagePath);

            // 构建Tesseract命令
            List<String> command = new ArrayList<>();
            command.add(tesseractPath);
            command.add(processedImage);
            command.add("stdout");
            command.add("-l");
            command.add(language);
            command.add("--oem");
            command.add("1");
            command.add("--psm");
            command.add("3");

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            String output = readStream(process.getInputStream());
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                result.setSuccess(true);
                result.setText(output.trim());

                double confidence = calculateConfidence(output);
                result.setConfidence(confidence);

                Map<String, Object> data = new HashMap<>();
                data.put("text", output.trim());
                data.put("language", language);
                data.put("confidence", confidence);
                result.setData(data);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("Tesseract识别失败，请检查图片质量或配置百度OCR");
            }

            // 清理预处理文件
            if (!processedImage.equals(imagePath)) {
                Files.deleteIfExists(Path.of(processedImage));
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("本地OCR识别异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 图片预处理（去噪、锐化）
     */
    private String preprocessImage(String imagePath) throws Exception {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new FileNotFoundException("图片文件不存在: " + imagePath);
        }

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            return imagePath;
        }

        // 灰度化处理
        BufferedImage grayImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);

        int threshold = 128;
        for (int y = 0; y < grayImage.getHeight(); y++) {
            for (int x = 0; x < grayImage.getWidth(); x++) {
                int gray = new Color(image.getRGB(x, y)).getRed();
                grayImage.setRGB(x, y, gray > threshold ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        g.dispose();

        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFile = tempDir + "/rpa_preprocessed_" + System.currentTimeMillis() + ".png";

        ImageIO.write(grayImage, "PNG", new File(tempFile));

        return tempFile;
    }

    /**
     * 计算识别置信度
     */
    private double calculateConfidence(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        int validChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                validChars++;
            }
        }

        return Math.min(0.99, validChars / (double) text.length());
    }

    /**
     * 读取流内容
     */
    private String readStream(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    /**
     * 身份证识别
     */
    public AiService.AiResult recognizeIdCard(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            String apiKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_API_KEY, "");
            String secretKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_SECRET_KEY, "");

            log.info("身份证识别开始 - API Key长度: {}, Secret Key长度: {}",
                    apiKey != null ? apiKey.length() : 0,
                    secretKey != null ? secretKey.length() : 0);

            if (apiKey.isEmpty() || secretKey.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR API密钥未配置");
                return result;
            }

            // 获取access token
            log.info("正在获取百度Access Token...");
            String accessToken = getBaiduAccessToken(apiKey, secretKey);
            log.info("获取Access Token结果: {}", accessToken != null ? "成功" : "失败");

            if (accessToken == null) {
                result.setSuccess(false);
                result.setErrorMessage("获取access_token失败");
                return result;
            }

            // 读取图片
            log.info("读取图片文件...");
            byte[] imageData = Files.readAllBytes(Path.of(request.getImagePath()));
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);

            String side = request.getParams() != null ?
                    (String) request.getParams().getOrDefault("side", "front") : "front";

            log.info("调用百度身份证识别API - side: {}", side);

            // 调用百度身份证识别API
            String baiduUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/idcard"
                    + "?access_token=" + accessToken;

            URL url = new URL(baiduUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            String params = "image=" + URLEncoder.encode(imageBase64, "UTF-8")
                    + "&id_card_side=" + side;

            conn.getOutputStream().write(params.getBytes());

            int responseCode = conn.getResponseCode();
            log.info("百度身份证API响应码: {}", responseCode);

            if (responseCode != 200) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR API返回错误码: " + responseCode);
                return result;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            log.info("百度身份证API响应: {}", response);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response.toString(), Map.class);

            // 检查错误
            if (responseMap.containsKey("error_code")) {
                result.setSuccess(false);
                result.setErrorMessage("百度身份证识别错误: " + responseMap.get("error_msg"));
                return result;
            }

            // 解析身份证结果 - 新版API直接返回 words_result（旧版是 idcard_side1/idcard_side2）
            Map<String, Object> idCardResult = null;
            if (responseMap.containsKey("idcard_side1")) {
                // 旧版API格式
                idCardResult = (Map<String, Object>) responseMap.get(side.equals("front") ? "idcard_side1" : "idcard_side2");
            } else if (responseMap.containsKey("words_result")) {
                // 新版API格式
                idCardResult = (Map<String, Object>) responseMap.get("words_result");
            }

            log.info("身份证{}识别结果: {}", side, idCardResult);

            if (idCardResult != null) {
                if ("front".equals(side)) {
                    result.setName(getFieldValue(idCardResult, "姓名"));
                    result.setGender(getFieldValue(idCardResult, "性别"));
                    result.setEthnicity(getFieldValue(idCardResult, "民族"));
                    result.setBirthDate(getFieldValue(idCardResult, "出生"));
                    result.setAddress(getFieldValue(idCardResult, "住址"));
                    result.setIdNumber(getFieldValue(idCardResult, "公民身份号码"));
                } else {
                    result.setIssueAuthority(getFieldValue(idCardResult, "签发机关"));
                    result.setValidDate(getFieldValue(idCardResult, "有效期限"));
                }
                result.setSide(side);
            }

            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.ID_CARD);

            log.info("身份证识别完成 - 姓名: {}, 身份证号: {}",
                    result.getName(), result.getIdNumber() != null ? "已识别" : "未识别");

            Map<String, Object> data = new HashMap<>();
            data.put("name", result.getName());
            data.put("idNumber", result.getIdNumber());
            data.put("gender", result.getGender());
            data.put("ethnicity", result.getEthnicity());
            data.put("birthDate", result.getBirthDate());
            data.put("address", result.getAddress());
            data.put("issueAuthority", result.getIssueAuthority());
            data.put("validDate", result.getValidDate());
            data.put("side", result.getSide());
            result.setData(data);

        } catch (Exception e) {
            log.error("身份证识别异常", e);
            result.setSuccess(false);
            result.setErrorMessage("身份证识别异常: " + e.getMessage());
        }

        return result;
    }

    private String getFieldValue(Map<String, Object> data, String field) {
        if (data.containsKey(field)) {
            Map<String, Object> fieldData = (Map<String, Object>) data.get(field);
            return (String) fieldData.get("words");
        }
        return "";
    }

    /**
     * 发票识别
     */
    public AiService.AiResult recognizeInvoice(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            String apiKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_API_KEY, "");
            String secretKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_SECRET_KEY, "");

            if (apiKey.isEmpty() || secretKey.isEmpty()) {
                // 降级到通用OCR
                return recognizeText(request);
            }

            String accessToken = getBaiduAccessToken(apiKey, secretKey);
            if (accessToken == null) {
                return recognizeText(request);
            }

            byte[] imageData = Files.readAllBytes(Path.of(request.getImagePath()));
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);

            String baiduUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice"
                    + "?access_token=" + accessToken;

            URL url = new URL(baiduUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "image=" + URLEncoder.encode(imageBase64, "UTF-8");
            conn.getOutputStream().write(params.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return recognizeText(request);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response.toString(), Map.class);

            if (responseMap.containsKey("error_code")) {
                return recognizeText(request);
            }

            result.setInvoiceNumber(getFieldValue(responseMap, "invoice_number"));
            result.setInvoiceCode(getFieldValue(responseMap, "invoice_code"));
            result.setInvoiceDate(getFieldValue(responseMap, "invoice_date"));
            result.setInvoiceAmount(getFieldValue(responseMap, "total_amount"));
            result.setTaxAmount(getFieldValue(responseMap, "tax_amount"));
            result.setSellerName(getFieldValue(responseMap, "seller_name"));
            result.setBuyerName(getFieldValue(responseMap, "buyer_name"));
            result.setInvoiceType("vat_invoice");

            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.INVOICE);

            Map<String, Object> data = new HashMap<>();
            data.put("invoiceCode", result.getInvoiceCode());
            data.put("invoiceNumber", result.getInvoiceNumber());
            data.put("invoiceDate", result.getInvoiceDate());
            data.put("invoiceAmount", result.getInvoiceAmount());
            data.put("taxAmount", result.getTaxAmount());
            data.put("sellerName", result.getSellerName());
            data.put("buyerName", result.getBuyerName());
            data.put("invoiceType", result.getInvoiceType());
            result.setData(data);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("发票识别异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 银行卡识别
     */
    public AiService.AiResult recognizeBankCard(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            String apiKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_API_KEY, "");
            String secretKey = systemConfigService.getConfig(SystemConfigService.KEY_OCR_SECRET_KEY, "");

            if (apiKey.isEmpty() || secretKey.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR API密钥未配置");
                return result;
            }

            String accessToken = getBaiduAccessToken(apiKey, secretKey);
            if (accessToken == null) {
                result.setSuccess(false);
                result.setErrorMessage("获取access_token失败");
                return result;
            }

            byte[] imageData = Files.readAllBytes(Path.of(request.getImagePath()));
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);

            String baiduUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/bank_card"
                    + "?access_token=" + accessToken;

            URL url = new URL(baiduUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "image=" + URLEncoder.encode(imageBase64, "UTF-8");
            conn.getOutputStream().write(params.getBytes());

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                result.setSuccess(false);
                result.setErrorMessage("百度OCR API返回错误码: " + responseCode);
                return result;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(response.toString(), Map.class);

            String cardNumber = (String) responseMap.get("bank_card_number");
            String cardType = (String) responseMap.get("bank_name");

            Map<String, Object> data = new HashMap<>();
            data.put("cardNumber", cardNumber);
            data.put("cardNumberMasked", cardNumber != null ?
                    "*".repeat(cardNumber.length() - 8) + cardNumber.substring(cardNumber.length() - 4) : null);
            data.put("cardType", cardType);

            result.setSuccess(true);
            result.setData(data);
            result.setServiceType(AiService.AiServiceType.BANK_CARD);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("银行卡识别异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 表格识别
     */
    public AiService.AiResult recognizeTable(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();

        try {
            AiService.AiResult ocrResult = recognizeText(request);
            if (!ocrResult.isSuccess()) {
                return ocrResult;
            }

            String text = ocrResult.getText();

            String[] lines = text.split("\n");
            List<Map<String, String>> tableData = new ArrayList<>();

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                String[] cells = line.split("[\\s\\t]+");
                if (cells.length > 1) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < cells.length; i++) {
                        row.put("col" + (i + 1), cells[i].trim());
                    }
                    tableData.add(row);
                }
            }

            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.TABLE);

            Map<String, Object> data = new HashMap<>();
            data.put("rows", tableData.size());
            data.put("data", tableData);
            result.setData(data);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("表格识别异常: " + e.getMessage());
        }

        return result;
    }
}
