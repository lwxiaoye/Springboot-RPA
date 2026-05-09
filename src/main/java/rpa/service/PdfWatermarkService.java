package rpa.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF水印服务
 * <p>
 * 为PDF文件添加水印功能，包括：
 * - 文字水印（对角线平铺）
 * - 页眉页脚水印
 * - 半透明水印
 * </p>
 *
 * @author RPA Security Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfWatermarkService {

    private static final String DEFAULT_SAVE_PATH = "D:/rpa/pdf";

    /**
     * 水印配置
     */
    @lombok.Data
    public static class WatermarkConfig {
        private float opacity = 0.15f;
        private String fontFamily = "Microsoft YaHei";
        private float fontSize = 10f;
        private String color = "#808080";
        private BaseColor baseColor;
        private float rotation = -45f;
        private int xPattern = 150;
        private int yPattern = 150;

        public WatermarkConfig() {
            this.baseColor = new BaseColor(128, 128, 128);
        }
    }

    /**
     * 为PDF添加水印
     *
     * @param inputPdfBytes 输入PDF字节数组
     * @param userName 用户名
     * @param phoneMasked 脱敏手机号
     * @param timestamp 导出时间戳
     * @param config 水印配置（可选）
     * @return 带水印的PDF字节数组
     */
    public byte[] addWatermark(byte[] inputPdfBytes, String userName, String phoneMasked,
                               String timestamp, WatermarkConfig config) {
        if (config == null) {
            config = new WatermarkConfig();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(inputPdfBytes);
            PdfStamper stamper = new PdfStamper(reader, baos);

            int totalPages = reader.getNumberOfPages();

            // 为每一页添加水印
            for (int i = 1; i <= totalPages; i++) {
                PdfContentByte canvas = stamper.getOverContent(i);
                addDiagonalWatermark(canvas, userName, phoneMasked, timestamp, config);
            }

            // 添加页眉页脚
            addHeaderFooter(stamper, totalPages, userName, phoneMasked, timestamp);

            stamper.close();
            reader.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("添加PDF水印失败", e);
            throw new RuntimeException("添加PDF水印失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加对角线平铺水印
     */
    private void addDiagonalWatermark(PdfContentByte canvas, String userName,
                                      String phoneMasked, String timestamp,
                                      WatermarkConfig config) throws DocumentException {
        // 创建水印文字
        String watermarkText = String.format("%s %s %s", userName, phoneMasked, timestamp);

        // 设置透明度
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(config.getOpacity());
        canvas.setGState(gs);

        // 设置字体
        Font font = FontFactory.getFont(config.getFontFamily(), BaseFont.IDENTITY_H,
            BaseFont.NOT_EMBEDDED, config.getFontSize());

        // 创建水印文字块
        Phrase phrase = new Phrase(watermarkText, font);
        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, phrase,
            config.getXPattern(), config.getYPattern(), config.getRotation(),
            PdfWriter.RUN_DIRECTION_RTL, 0);

        // 在多个位置平铺水印
        addTiledWatermark(canvas, watermarkText, font, config);
    }

    /**
     * 平铺水印
     */
    private void addTiledWatermark(PdfContentByte canvas, String text, Font font,
                                   WatermarkConfig config) {
        try {
            Rectangle pageSize = canvas.getPdfDocument().getPageSize();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            // 设置透明度
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(config.getOpacity() * 0.5f);
            canvas.setGState(gs);

            // 计算平铺数量
            int cols = (int) Math.ceil(pageWidth / config.getXPattern()) + 1;
            int rows = (int) Math.ceil(pageHeight / config.getYPattern()) + 1;

            // 平铺绘制
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    float x = col * config.getXPattern();
                    float y = pageHeight - row * config.getYPattern();

                    canvas.saveState();
                    canvas.beginText();
                    canvas.setFontAndSize(BaseFont.createFont(), config.getFontSize());
                    canvas.setColorFill(config.getBaseColor());

                    // 旋转45度
                    canvas.concatCTM(
                        (float) Math.cos(Math.toRadians(config.getRotation())),
                        (float) Math.sin(Math.toRadians(config.getRotation())),
                        -(float) Math.sin(Math.toRadians(config.getRotation())),
                        (float) Math.cos(Math.toRadians(config.getRotation())),
                        x, y
                    );

                    canvas.showTextAligned(Element.ALIGN_CENTER, text, 0, 0, 0);
                    canvas.endText();
                    canvas.restoreState();
                }
            }
        } catch (Exception e) {
            log.warn("平铺水印绘制失败: {}", e.getMessage());
        }
    }

    /**
     * 添加页眉页脚
     */
    private void addHeaderFooter(PdfStamper stamper, int totalPages,
                                 String userName, String phoneMasked, String timestamp) {
        for (int i = 1; i <= totalPages; i++) {
            PdfContentByte canvas = stamper.getOverContent(i);

            // 页脚水印
            String footerText = String.format("[水印: %s %s %s] - 受银保监会数据安全法规保护",
                userName, phoneMasked, timestamp);

            try {
                Font footerFont = FontFactory.getFont("Microsoft YaHei",
                    BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED, 8);

                Phrase footer = new Phrase(footerText, footerFont);

                // 页脚位置
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, footer,
                    300, 20, 0);

                // 页码
                Font pageFont = FontFactory.getFont("Microsoft YaHei",
                    BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED, 8);
                Phrase pageNum = new Phrase(String.format("第 %d 页 / 共 %d 页", i, totalPages), pageFont);
                ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, pageNum,
                    550, 20, 0);

            } catch (Exception e) {
                log.warn("添加页眉页脚失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建带水印的PDF并保存到文件
     */
    public String createWatermarkedPdf(byte[] inputPdfBytes, String fileName,
                                       String userName, String phoneMasked, String timestamp) {
        try {
            Path saveDir = Paths.get(DEFAULT_SAVE_PATH);
            Files.createDirectories(saveDir);

            String outputFileName = "watermark_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "_" + fileName;
            Path outputPath = saveDir.resolve(outputFileName);

            byte[] watermarkedPdf = addWatermark(inputPdfBytes, userName, phoneMasked, timestamp, null);
            Files.write(outputPath, watermarkedPdf);

            log.info("带水印PDF已保存: {}", outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("创建带水印PDF失败", e);
            throw new RuntimeException("创建带水印PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为响应输出带水印PDF
     */
    public void writeWatermarkedPdfToResponse(byte[] inputPdfBytes, String fileName,
                                              String userName, String phoneMasked,
                                              String timestamp, HttpServletResponse response) {
        try {
            byte[] watermarkedPdf = addWatermark(inputPdfBytes, userName, phoneMasked, timestamp, null);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.setContentLength(watermarkedPdf.length);

            OutputStream out = response.getOutputStream();
            out.write(watermarkedPdf);
            out.flush();
            out.close();

            log.info("带水印PDF已响应: {}", fileName);

        } catch (Exception e) {
            log.error("输出带水印PDF失败", e);
            throw new RuntimeException("输出带水印PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建纯水印PDF（用于打印保护）
     */
    public byte[] createWatermarkOnlyPdf(String userName, String phoneMasked, String timestamp) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();

            WatermarkConfig config = new WatermarkConfig();

            // 在PDF上添加水印
            PdfContentByte canvas = writer.getDirectContentUnder();
            addTiledWatermark(canvas, String.format("%s %s %s", userName, phoneMasked, timestamp),
                FontFactory.getFont("Microsoft YaHei", BaseFont.IDENTITY_H,
                    BaseFont.NOT_EMBEDDED, 50), config);

            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("创建水印PDF失败", e);
            throw new RuntimeException("创建水印PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成水印文本（用于其他文件格式）
     */
    public String generateWatermarkText(String userName, String phoneMasked, String timestamp) {
        return String.format("%s %s %s", userName, phoneMasked, timestamp);
    }

    /**
     * 生成水印版权声明
     */
    public String generateCopyrightNotice() {
        return "【重要声明】本文件包含敏感数据，受银保监会数据安全法规保护。任何未经授权的复制、传播将承担法律责任。";
    }
}
