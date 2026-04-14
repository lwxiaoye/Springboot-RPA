package rpa.recording;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * 图像识别定位服务
 *
 * 功能：
 * 1. 基于模板图像的UI元素定位
 * 2. 模糊匹配容错
 * 3. 多目标识别
 * 4. 屏幕截图与区域匹配
 *
 * @author RPA System
 */
@Slf4j
@Service
public class ImageLocatorService {

    @Value("${rpa.locator.match-threshold:0.8}")
    private double matchThreshold;

    @Value("${rpa.locator.search-timeout:5000}")
    private int searchTimeout;

    @Value("${rpa.locator.template-path:D:/rpa/templates}")
    private String templatePath;

    /**
     * 在屏幕上查找目标图像
     *
     * @param templateImage 模板图像路径
     * @return 匹配结果列表（按置信度排序）
     */
    public List<MatchResult> findOnScreen(String templateImage) {
        try {
            BufferedImage screen = new Robot().createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

            return findInImage(screen, templateImage);
        } catch (Exception e) {
            log.error("屏幕查找失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 在指定区域查找目标图像
     */
    public List<MatchResult> findInRegion(String templateImage, int x, int y, int width, int height) {
        try {
            Rectangle region = new Rectangle(x, y, width, height);
            BufferedImage screenRegion = new Robot().createScreenCapture(region);

            return findInImage(screenRegion, templateImage);
        } catch (Exception e) {
            log.error("区域查找失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 在图像中查找模板
     */
    public List<MatchResult> findInImage(BufferedImage source, String templateImagePath) {
        List<MatchResult> results = new ArrayList<>();

        try {
            BufferedImage template = ImageIO.read(new File(templateImagePath));
            int matchCount = 0;

            // 滑动窗口搜索
            int stepX = Math.max(template.getWidth() / 4, 5);
            int stepY = Math.max(template.getHeight() / 4, 5);

            for (int y = 0; y <= source.getHeight() - template.getHeight(); y += stepY) {
                for (int x = 0; x <= source.getWidth() - template.getWidth(); x += stepX) {
                    double similarity = compareImages(source, x, y, template);
                    if (similarity >= matchThreshold) {
                        results.add(new MatchResult(x, y, template.getWidth(), template.getHeight(), similarity));
                        matchCount++;
                        // 限制最大匹配数量
                        if (matchCount >= 100) {
                            break;
                        }
                    }
                }
                if (matchCount >= 100) break;
            }

            // 按置信度排序
            results.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        } catch (Exception e) {
            log.error("图像匹配失败", e);
        }

        return results;
    }

    /**
     * 等待图像出现
     */
    public MatchResult waitForImage(String templateImage, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        int interval = 200;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            List<MatchResult> results = findOnScreen(templateImage);
            if (!results.isEmpty()) {
                return results.get(0);
            }

            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        log.warn("等待图像超时: {}", templateImage);
        return null;
    }

    /**
     * 等待图像消失
     */
    public boolean waitForImageDisappear(String templateImage, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        int interval = 200;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            List<MatchResult> results = findOnScreen(templateImage);
            if (results.isEmpty()) {
                return true;
            }

            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * 查找并点击
     */
    public boolean findAndClick(String templateImage) {
        MatchResult result = waitForImage(templateImage, searchTimeout);
        if (result != null) {
            int centerX = result.getX() + result.getWidth() / 2;
            int centerY = result.getY() + result.getHeight() / 2;

            try {
                Robot robot = new Robot();
                robot.mouseMove(centerX, centerY);
                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                log.info("图像定位点击成功: ({}, {})", centerX, centerY);
                return true;
            } catch (Exception e) {
                log.error("点击失败", e);
            }
        }
        return false;
    }

    /**
     * 查找并双击
     */
    public boolean findAndDoubleClick(String templateImage) {
        MatchResult result = waitForImage(templateImage, searchTimeout);
        if (result != null) {
            int centerX = result.getX() + result.getWidth() / 2;
            int centerY = result.getY() + result.getHeight() / 2;

            try {
                Robot robot = new Robot();
                robot.mouseMove(centerX, centerY);
                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                Thread.sleep(50);
                robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
                return true;
            } catch (Exception e) {
                log.error("双击失败", e);
            }
        }
        return false;
    }

    /**
     * 查找并悬停
     */
    public boolean findAndHover(String templateImage) {
        MatchResult result = waitForImage(templateImage, searchTimeout);
        if (result != null) {
            int centerX = result.getX() + result.getWidth() / 2;
            int centerY = result.getY() + result.getHeight() / 2;

            try {
                Robot robot = new Robot();
                robot.mouseMove(centerX, centerY);
                return true;
            } catch (Exception e) {
                log.error("悬停失败", e);
            }
        }
        return false;
    }

    /**
     * 保存屏幕截图
     */
    public String saveScreenshot(String name) {
        try {
            BufferedImage screen = new Robot().createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

            Path templateDir = Paths.get(templatePath);
            Files.createDirectories(templateDir);

            String fileName = name + "_" + System.currentTimeMillis() + ".png";
            Path filePath = templateDir.resolve(fileName);

            ImageIO.write(screen, "PNG", filePath.toFile());
            log.info("截图已保存: {}", filePath);

            return filePath.toString();
        } catch (Exception e) {
            log.error("截图失败", e);
            return null;
        }
    }

    /**
     * 保存区域截图
     */
    public String saveRegionScreenshot(int x, int y, int width, int height, String name) {
        try {
            Rectangle region = new Rectangle(x, y, width, height);
            BufferedImage image = new Robot().createScreenCapture(region);

            Path templateDir = Paths.get(templatePath);
            Files.createDirectories(templateDir);

            String fileName = name + "_" + System.currentTimeMillis() + ".png";
            Path filePath = templateDir.resolve(fileName);

            ImageIO.write(image, "PNG", filePath.toFile());
            log.info("区域截图已保存: {}", filePath);

            return filePath.toString();
        } catch (Exception e) {
            log.error("区域截图失败", e);
            return null;
        }
    }

    /**
     * 截取图像区域并保存为模板
     */
    public String captureTemplate(int x, int y, int width, int height, String name) {
        return saveRegionScreenshot(x, y, width, height, "template_" + name);
    }

    /**
     * 调整匹配阈值
     */
    public void setMatchThreshold(double threshold) {
        this.matchThreshold = Math.max(0.0, Math.min(1.0, threshold));
        log.info("匹配阈值已调整为: {}", this.matchThreshold);
    }

    /**
     * 图像比较（基于像素比较）
     */
    private double compareImages(BufferedImage source, int sx, int sy, BufferedImage template) {
        int width = template.getWidth();
        int height = template.getHeight();

        int matchCount = 0;
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcRGB = source.getRGB(sx + x, sy + y);
                int tplRGB = template.getRGB(x, y);

                // 简化的RGB比较
                int srcR = (srcRGB >> 16) & 0xFF;
                int srcG = (srcRGB >> 8) & 0xFF;
                int srcB = srcRGB & 0xFF;

                int tplR = (tplRGB >> 16) & 0xFF;
                int tplG = (tplRGB >> 8) & 0xFF;
                int tplB = tplRGB & 0xFF;

                // 颜色差异容忍度
                int diff = Math.abs(srcR - tplR) + Math.abs(srcG - tplG) + Math.abs(srcB - tplB);
                if (diff < 50) {
                    matchCount++;
                }
            }
        }

        return (double) matchCount / totalPixels;
    }

    /**
     * 获取所有模板列表
     */
    public List<String> getTemplateList() {
        List<String> templates = new ArrayList<>();
        try {
            Path templateDir = Paths.get(templatePath);
            if (Files.exists(templateDir)) {
                Files.walk(templateDir)
                    .filter(p -> p.toString().endsWith(".png") || p.toString().endsWith(".jpg"))
                    .forEach(p -> templates.add(p.toString()));
            }
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
        }
        return templates;
    }

    /**
     * 删除模板
     */
    public boolean deleteTemplate(String templatePath) {
        try {
            Files.delete(Paths.get(templatePath));
            return true;
        } catch (Exception e) {
            log.error("删除模板失败", e);
            return false;
        }
    }

    // ==================== 内部类 ====================

    /**
     * 匹配结果
     */
    @Data
    public static class MatchResult {
        private int x;
        private int y;
        private int width;
        private int height;
        private double confidence;

        public MatchResult(int x, int y, int width, int height, double confidence) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.confidence = confidence;
        }

        public int getCenterX() { return x + width / 2; }
        public int getCenterY() { return y + height / 2; }
    }
}