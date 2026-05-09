package rpa.ai;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 条码识别服务
 * 支持二维码、条形码等多种条码格式的识别和生成
 */
@Slf4j
@Service
public class BarcodeService {

    // 支持的条码格式
    private static final Map<String, Result> FORMAT_MAP = new HashMap<>();

    /**
     * 识别条码图片
     */
    public AiService.AiResult recognizeBarcode(AiService.AiRequest request) {
        AiService.AiResult result = new AiService.AiResult();
        long startTime = System.currentTimeMillis();

        try {
            String filePath = request.getImagePath();
            log.info("开始识别条码: {}", filePath);

            // 读取图片
            BufferedImage image = ImageIO.read(new File(filePath));
            if (image == null) {
                result.setSuccess(false);
                result.setErrorMessage("无法读取图片文件");
                return result;
            }

            // 转换为ZXing需要的格式
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // 尝试多种格式识别
            String barcodeText = null;
            String formatName = null;
            double confidence = 1.0;

            // 优先尝试二维码
            try {
                QRCodeReader qrReader = new QRCodeReader();
                Result qrResult = qrReader.decode(bitmap);
                if (qrResult != null) {
                    barcodeText = qrResult.getText();
                    formatName = "QR_CODE";
                    log.info("识别到二维码，内容: {}", barcodeText);
                }
            } catch (Exception e) {
                log.debug("非二维码格式，尝试其他格式: {}", e.getMessage());
            }

            // 如果不是二维码，尝试其他格式
            if (barcodeText == null) {
                // 尝试Code128
                try {
                    com.google.zxing.oned.Code128Reader code128Reader = new com.google.zxing.oned.Code128Reader();
                    Result code128Result = code128Reader.decode(bitmap);
                    if (code128Result != null) {
                        barcodeText = code128Result.getText();
                        formatName = "CODE_128";
                        log.info("识别到Code128条码，内容: {}", barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("非Code128格式: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                // 尝试Code39
                try {
                    com.google.zxing.oned.Code39Reader code39Reader = new com.google.zxing.oned.Code39Reader();
                    Result code39Result = code39Reader.decode(bitmap);
                    if (code39Result != null) {
                        barcodeText = code39Result.getText();
                        formatName = "CODE_39";
                        log.info("识别到Code39条码，内容: {}", barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("非Code39格式: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                // 尝试EAN-13
                try {
                    com.google.zxing.oned.EAN13Reader ean13Reader = new com.google.zxing.oned.EAN13Reader();
                    Result ean13Result = ean13Reader.decode(bitmap);
                    if (ean13Result != null) {
                        barcodeText = ean13Result.getText();
                        formatName = "EAN_13";
                        log.info("识别到EAN-13条码，内容: {}", barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("非EAN-13格式: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                // 尝试EAN-8
                try {
                    com.google.zxing.oned.EAN8Reader ean8Reader = new com.google.zxing.oned.EAN8Reader();
                    Result ean8Result = ean8Reader.decode(bitmap);
                    if (ean8Result != null) {
                        barcodeText = ean8Result.getText();
                        formatName = "EAN_8";
                        log.info("识别到EAN-8条码，内容: {}", barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("非EAN-8格式: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                // 尝试ITF
                try {
                    com.google.zxing.oned.ITFReader itfReader = new com.google.zxing.oned.ITFReader();
                    Result itfResult = itfReader.decode(bitmap);
                    if (itfResult != null) {
                        barcodeText = itfResult.getText();
                        formatName = "ITF";
                        log.info("识别到ITF条码，内容: {}", barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("非ITF格式: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                // 尝试DataMatrix
                try {
                    com.google.zxing.datamatrix.DataMatrixReader dmReader = new com.google.zxing.datamatrix.DataMatrixReader();
                    Result dmResult = dmReader.decode(bitmap);
                    if (dmResult != null) {
                        barcodeText = dmResult.getText();
                        formatName = "DATA_MATRIX";
                        log.info("识别到DataMatrix，内容: {}", barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("非DataMatrix格式: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                // 最后尝试通用MultiFormatReader
                try {
                    MultiFormatReader formatReader = new MultiFormatReader();
                    Result genericResult = formatReader.decode(bitmap);
                    if (genericResult != null) {
                        barcodeText = genericResult.getText();
                        formatName = genericResult.getBarcodeFormat().toString();
                        log.info("识别到条码，格式: {}，内容: {}", formatName, barcodeText);
                    }
                } catch (Exception e) {
                    log.debug("通用识别失败: {}", e.getMessage());
                }
            }

            if (barcodeText == null) {
                result.setSuccess(false);
                result.setErrorMessage("无法识别条码格式，请确保图片清晰且包含有效的条码");
                return result;
            }

            // 设置结果
            result.setSuccess(true);
            result.setText(barcodeText);
            result.setBarcodeText(barcodeText);
            result.setBarcodeFormat(formatName);
            result.setConfidence(confidence);
            result.setServiceType(AiService.AiServiceType.BARCODE);

            Map<String, Object> data = new HashMap<>();
            data.put("text", barcodeText);
            data.put("format", formatName);
            data.put("confidence", confidence);
            data.put("duration", System.currentTimeMillis() - startTime);
            result.setData(data);

            log.info("条码识别成功，内容: {}，格式: {}", barcodeText, formatName);

        } catch (Exception e) {
            log.error("条码识别失败", e);
            result.setSuccess(false);
            result.setErrorMessage("条码识别失败: " + e.getMessage());
        }

        result.setDuration(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 生成二维码
     */
    public byte[] generateQRCode(String content, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ImageIO.write(image, "PNG", baos);

        return baos.toByteArray();
    }

    /**
     * 生成条形码（Code128）
     */
    public byte[] generateBarcode(String content, int width, int height) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ImageIO.write(image, "PNG", baos);

        return baos.toByteArray();
    }

    /**
     * 获取支持的条码格式列表
     */
    public Map<String, String> getSupportedFormats() {
        Map<String, String> formats = new HashMap<>();
        formats.put("QR_CODE", "二维码");
        formats.put("CODE_128", "Code128条形码");
        formats.put("CODE_39", "Code39条形码");
        formats.put("EAN_13", "EAN-13商品码");
        formats.put("EAN_8", "EAN-8商品码");
        formats.put("DATA_MATRIX", "DataMatrix矩阵码");
        formats.put("ITF", "ITF-14交叉25码");
        return formats;
    }
}
