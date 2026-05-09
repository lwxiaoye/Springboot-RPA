package rpa.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档处理服务
 * 支持PDF和Word文档的文本提取
 */
@Slf4j
@Service
public class DocumentService {

    /**
     * 处理文档文件（PDF或Word）
     */
    public AiService.AiResult processDocument(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        long startTime = System.currentTimeMillis();

        try {
            String filePath = request.getImagePath();
            String fileName = filePath.toLowerCase();

            log.info("开始处理文档: {}", filePath);

            String text;
            int pages = 1;

            if (fileName.endsWith(".pdf")) {
                // 处理PDF
                Map<String, Object> pdfResult = extractPdfText(filePath);
                text = (String) pdfResult.get("text");
                pages = (Integer) pdfResult.get("pages");
            } else if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                // 处理Word
                text = extractWordText(filePath);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("不支持的文档格式，仅支持PDF、DOC、DOCX格式");
                return result;
            }

            result.setSuccess(true);
            result.setText(text);
            result.setDocumentText(text);
            result.setPages(pages);
            result.setServiceType(AiService.AiServiceType.DOCUMENT);
            result.setConfidence(0.95);

            Map<String, Object> data = new HashMap<>();
            data.put("text", text);
            data.put("pages", pages);
            data.put("duration", System.currentTimeMillis() - startTime);
            result.setData(data);

            log.info("文档处理成功，提取文字{}个字符，页数：{}", text.length(), pages);

        } catch (Exception e) {
            log.error("文档处理失败", e);
            result.setSuccess(false);
            result.setErrorMessage("文档处理失败: " + e.getMessage());
        }

        result.setDuration(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 从PDF提取文本
     */
    private Map<String, Object> extractPdfText(String filePath) throws Exception {
        Map<String, Object> result = new HashMap<>();
        StringBuilder textBuilder = new StringBuilder();

        try (PDDocument document = PDDocument.load(new File(filePath))) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            log.info("PDF文档共有 {} 页", totalPages);

            // 设置排序模式以确保页面按顺序提取
            stripper.setSortByPosition(true);

            for (int i = 1; i <= totalPages; i++) {
                try {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String pageText = stripper.getText(document);

                    if (pageText != null && !pageText.trim().isEmpty()) {
                        textBuilder.append(pageText);
                        textBuilder.append("\n\n--- 第 ").append(i).append(" 页结束 ---\n\n");
                    }
                } catch (Exception e) {
                    log.warn("提取第 {} 页失败: {}", i, e.getMessage());
                }
            }

            result.put("text", textBuilder.toString().trim());
            result.put("pages", totalPages);
        }

        return result;
    }

    /**
     * 从Word文档提取文本
     */
    private String extractWordText(String filePath) throws Exception {
        StringBuilder textBuilder = new StringBuilder();

        try (InputStream is = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            textBuilder.append(text);
        }

        return textBuilder.toString().trim();
    }

    /**
     * 处理Base64编码的文档
     */
    public AiService.AiResult processBase64Document(String base64Data, String fileExtension) {
        AiService.AiResult result = new AiService.AiResult();
        long startTime = System.currentTimeMillis();

        try {
            // 解码Base64
            byte[] documentBytes = Base64.getDecoder().decode(base64Data);

            // 保存为临时文件
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = "rpa_doc_" + System.currentTimeMillis() + fileExtension;
            Path tempPath = Path.of(tempDir, fileName);

            Files.write(tempPath, documentBytes);

            // 处理文档
            AiService.AiRequest request = new AiService.AiRequest();
            request.setImagePath(tempPath.toString());

            result = processDocument(request);

            // 清理临时文件
            Files.deleteIfExists(tempPath);

        } catch (Exception e) {
            log.error("Base64文档处理失败", e);
            result.setSuccess(false);
            result.setErrorMessage("文档处理失败: " + e.getMessage());
        }

        result.setDuration(System.currentTimeMillis() - startTime);
        return result;
    }
}
