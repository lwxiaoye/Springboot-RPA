package rpa.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR识别服务
 * 
 * 功能说明：
 * - 通用文字识别（支持多语言）
 * - 身份证识别（正反面）
 * - 发票识别
 * - 银行卡识别
 * - 表格结构化识别
 * - 支持本地Tesseract和云端API
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

    /**
     * 通用文字识别
     */
    public AiService.AiResult recognizeText(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            if ("cloud".equals(mode)) {
                return recognizeTextByCloud(request);
            } else {
                return recognizeTextByLocal(request);
            }
        } catch (Exception e) {
            log.error("OCR文字识别失败", e);
            result.setSuccess(false);
            result.setErrorMessage("识别失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 本地Tesseract识别
     */
    private AiService.AiResult recognizeTextByLocal(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            String imagePath = request.getImagePath();
            String language = request.getLanguage();
            
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
            command.add("1");  // LSTM OCR引擎
            command.add("--psm");
            command.add("3");  // 自动分段
            
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            
            Process process = builder.start();
            String output = readStream(process.getInputStream());
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                result.setSuccess(true);
                result.setText(output.trim());
                
                // 计算置信度（根据字符可信度）
                double confidence = calculateConfidence(output);
                result.setConfidence(confidence);
                
                // 解析结果为结构化数据
                Map<String, Object> data = new HashMap<>();
                data.put("text", output.trim());
                data.put("language", language);
                data.put("confidence", confidence);
                result.setData(data);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("Tesseract识别失败，退出码: " + exitCode);
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
     * 云端API识别（百度OCR等）
     */
    private AiService.AiResult recognizeTextByCloud(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        // 这里可以集成百度、腾讯、阿里云等OCR API
        // 暂时返回本地识别结果作为降级
        log.warn("云端OCR未配置，使用本地识别作为降级");
        return recognizeTextByLocal(request);
    }

    /**
     * 身份证识别
     */
    public AiService.AiResult recognizeIdCard(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            // 先进行文字识别
            AiService.AiResult ocrResult = recognizeText(request);
            if (!ocrResult.isSuccess()) {
                return ocrResult;
            }
            
            String text = ocrResult.getText();
            String side = request.getParams() != null ? 
                (String) request.getParams().getOrDefault("side", "front") : "front";
            
            if ("front".equals(side)) {
                // 正面：姓名、性别、民族、出生日期、地址、身份证号
                result.setName(extractField(text, "姓名[\\s:：]*(.{2,8})"));
                result.setGender(extractField(text, "[性性别][别]?[\\s:：]*(男|女)"));
                result.setEthnicity(extractField(text, "[民民民族][族]?[\\s:：]*(.{2,4})"));
                result.setBirthDate(extractField(text, "[出出生日日期期]*[\\s:：]*(19|20)\\d{2}[年/-]\\d{1,2}[月/-]\\d{1,2}日?"));
                result.setAddress(extractField(text, "[住住址址]?[\\s:：]*(.+)"));
                result.setIdNumber(extractIdNumber(text));
                result.setSide("front");
            } else {
                // 背面：签发机关、有效期
                result.setIssueAuthority(extractField(text, "[签发机关]*[\\s:：]*(.+)"));
                result.setValidDate(extractField(text, "[有效有效期期]*[\\s:：]*(\\d{4}.*\\d{4})"));
                result.setSide("back");
            }
            
            result.setSuccess(true);
            result.setServiceType(AiService.AiServiceType.ID_CARD);
            
            // 构建结构化数据
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
            result.setSuccess(false);
            result.setErrorMessage("身份证识别异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 发票识别
     */
    public AiService.AiResult recognizeInvoice(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        
        try {
            AiService.AiResult ocrResult = recognizeText(request);
            if (!ocrResult.isSuccess()) {
                return ocrResult;
            }
            
            String text = ocrResult.getText();
            
            // 发票号码（10位或12位数字）
            result.setInvoiceNumber(extractField(text, "[发发票票号号码]*[\\s:：]*([A-Z0-9]{10,12})"));
            
            // 发票代码（12位数字）
            result.setInvoiceCode(extractField(text, "[发发票票代码]*[\\s:：]*(\\d{12})"));
            
            // 开票日期
            result.setInvoiceDate(extractField(text, "[开开票票日期]*[\\s:：]*(\\d{4}[年/-]\\d{1,2}[月/-]\\d{1,2})"));
            
            // 金额（带小数）
            result.setInvoiceAmount(extractField(text, "[金金额合计]*[\\s:：¥￥]*([\\d,]+(?:\\.\\d{2})?)"));
            
            // 税额
            result.setTaxAmount(extractField(text, "[税税额]*[\\s:：¥￥]*([\\d,]+(?:\\.\\d{2})?)"));
            
            // 销售方
            result.setSellerName(extractField(text, "[销销售方名称]*[\\s:：]*(.+)"));
            
            // 购买方
            result.setBuyerName(extractField(text, "[购购买方名称]*[\\s:：]*(.+)"));
            
            // 发票类型判断
            if (text.contains("增值税") || text.contains("专用发票")) {
                result.setInvoiceType("vat_special");
            } else if (text.contains("普通") || text.contains("电子")) {
                result.setInvoiceType("normal");
            } else {
                result.setInvoiceType("unknown");
            }
            
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
            AiService.AiResult ocrResult = recognizeText(request);
            if (!ocrResult.isSuccess()) {
                return ocrResult;
            }
            
            String text = ocrResult.getText();
            
            // 提取卡号（16-19位数字）
            Pattern pattern = Pattern.compile("\\b(\\d{16,19})\\b");
            Matcher matcher = pattern.matcher(text.replace(" ", "").replace("-", ""));
            
            if (matcher.find()) {
                String cardNumber = matcher.group(1);
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);
                data.put("cardNumberMasked", maskCardNumber(cardNumber));
                data.put("cardType", detectCardType(cardNumber));
                
                result.setSuccess(true);
                result.setData(data);
                result.setServiceType(AiService.AiServiceType.BANK_CARD);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("未识别到银行卡号");
            }
            
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
            // 先进行通用文字识别
            AiService.AiResult ocrResult = recognizeText(request);
            if (!ocrResult.isSuccess()) {
                return ocrResult;
            }
            
            String text = ocrResult.getText();
            
            // 简单按行分割，实际生产中应该使用专门的表格识别库
            String[] lines = text.split("\n");
            List<Map<String, String>> tableData = new ArrayList<>();
            
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                // 尝试按空格或制表符分割
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

    // ==================== 辅助方法 ====================

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
            return imagePath;  // 无法读取时返回原路径
        }
        
        // 灰度化处理
        BufferedImage grayImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        
        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        
        // 应用阈值二值化
        int threshold = 128;
        for (int y = 0; y < grayImage.getHeight(); y++) {
            for (int x = 0; x < grayImage.getWidth(); x++) {
                int gray = new Color(image.getRGB(x, y)).getRed();
                grayImage.setRGB(x, y, gray > threshold ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        g.dispose();
        
        // 保存预处理后的图片
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
        
        // 简单置信度计算：基于有效字符比例
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
     * 提取字段（正则匹配）
     */
    private String extractField(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("字段提取失败: pattern={}", pattern);
        }
        return "";
    }

    /**
     * 提取身份证号码
     */
    private String extractIdNumber(String text) {
        // 身份证号码：15位或18位
        Pattern pattern = Pattern.compile("\\b(\\d{15}|\\d{17}[\\dXx])\\b");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 掩码银行卡号
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber;
        }
        int maskLength = cardNumber.length() - 8;
        return "*".repeat(maskLength) + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * 识别卡类型
     */
    private String detectCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "unknown";
        }
        
        String firstDigit = cardNumber.substring(0, 1);
        String firstTwo = cardNumber.substring(0, 2);
        
        // 简单卡BIN判断
        if (firstDigit.equals("4")) {
            return "Visa";
        } else if (firstTwo.compareTo("51") >= 0 && firstTwo.compareTo("55") <= 0) {
            return "MasterCard";
        } else if (firstTwo.equals("62") || firstTwo.equals("60")) {
            return "银联";
        } else if (firstTwo.equals("35")) {
            return "JCB";
        } else if (firstTwo.equals("34") || firstTwo.equals("37")) {
            return "American Express";
        }
        
        return "unknown";
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
}