package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报表图表生成服务
 * <p>
 * 生成各种类型的图表数据，用于在邮件中内嵌预览。
 * 支持折线图、柱状图、饼图、仪表盘等。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportChartService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 图表类型枚举
    public enum ChartType {
        LINE("line", "折线图"),
        BAR("bar", "柱状图"),
        PIE("pie", "饼图"),
        GAUGE("gauge", "仪表盘"),
        AREA("area", "面积图"),
        DONUT("donut", "环形图");

        private final String code;
        private final String name;

        ChartType(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
    }

    // 图表配置
    @lombok.Data
    public static class ChartConfig {
        private String type;
        private String title;
        private Map<String, Object> data;
        private Map<String, String> colors;
        private Map<String, Object> options;
        private int width;
        private int height;

        public ChartConfig() {
            this.width = 600;
            this.height = 400;
            this.colors = new HashMap<>();
            this.options = new HashMap<>();
        }
    }

    // 图表数据响应
    @lombok.Data
    public static class ChartResponse {
        private String chartId;
        private String svg; // SVG格式的图表
        private String html; // HTML内嵌格式
        private String base64; // Base64编码的图片
        private String json; // JSON数据格式
        private long generatedAt;

        public ChartResponse() {
            this.generatedAt = System.currentTimeMillis();
        }
    }

    /**
     * 生成任务执行趋势图
     */
    public ChartResponse generateExecutionTrendChart(Map<String, Object> data) {
        ChartResponse response = new ChartResponse();
        response.setChartId(UUID.randomUUID().toString());

        List<String> dates = (List<String>) data.getOrDefault("dates", new ArrayList<>());
        List<Integer> successData = (List<Integer>) data.getOrDefault("success", new ArrayList<>());
        List<Integer> failedData = (List<Integer>) data.getOrDefault("failed", new ArrayList<>());

        // 生成SVG图表
        String svg = generateSVGLineChart(dates, successData, failedData);
        response.setSvg(svg);

        // 生成HTML内嵌代码
        response.setHtml(generateHTMLEmbed(svg, "任务执行趋势"));

        return response;
    }

    /**
     * 生成任务成功率饼图
     */
    public ChartResponse generateSuccessRatePieChart(int successCount, int failedCount) {
        ChartResponse response = new ChartResponse();
        response.setChartId(UUID.randomUUID().toString());

        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("成功", successCount);
        data.put("失败", failedCount);

        String svg = generateSVGPieChart(data);
        response.setSvg(svg);
        response.setHtml(generateHTMLEmbed(svg, "任务成功率"));

        return response;
    }

    /**
     * 生成RPA效率柱状图
     */
    public ChartResponse generateRobotEfficiencyChart(List<Map<String, Object>> robotData) {
        ChartResponse response = new ChartResponse();
        response.setChartId(UUID.randomUUID().toString());

        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (Map<String, Object> item : robotData) {
            labels.add((String) item.getOrDefault("name", ""));
            values.add(((Number) item.getOrDefault("executions", 0)).intValue());
        }

        String svg = generateSVGBarChart(labels, values, "#409eff");
        response.setSvg(svg);
        response.setHtml(generateHTMLEmbed(svg, "RPA执行效率"));

        return response;
    }

    /**
     * 生成ROI趋势图
     */
    public ChartResponse generateROITrendChart(List<String> months, List<Double> roiValues) {
        ChartResponse response = new ChartResponse();
        response.setChartId(UUID.randomUUID().toString());

        List<Integer> intValues = new ArrayList<>();
        for (Double v : roiValues) {
            intValues.add(v.intValue());
        }

        String svg = generateSVGLineChart(months, intValues, new ArrayList<>());
        response.setSvg(svg);
        response.setHtml(generateHTMLEmbed(svg, "ROI投资回报率"));

        return response;
    }

    /**
     * 生成仪表盘图
     */
    public ChartResponse generateGaugeChart(String label, double value, double min, double max) {
        ChartResponse response = new ChartResponse();
        response.setChartId(UUID.randomUUID().toString());

        String svg = generateSVGGauge(label, value, min, max);
        response.setSvg(svg);
        response.setHtml(generateHTMLEmbed(svg, label));

        return response;
    }

    /**
     * 生成每日执行汇总HTML（用于邮件内嵌）
     */
    public String generateDailyReportHTML(Map<String, Object> reportData) {
        StringBuilder html = new StringBuilder();

        html.append("<div style='font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto;'>");

        // 标题
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; border-radius: 10px 10px 0 0;'>");
        html.append("<h2 style='color: white; margin: 0;'>📊 RPA日报 - ").append(reportData.getOrDefault("date", LocalDate.now().format(DATE_FORMATTER))).append("</h2>");
        html.append("</div>");

        // 统计卡片
        html.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 0 0 10px 10px;'>");

        // 四个统计卡片
        html.append("<div style='display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-bottom: 20px;'>");

        addStatCard(html, "总执行次数", String.valueOf(reportData.getOrDefault("totalExecutions", 0)), "#409eff");
        addStatCard(html, "成功率", String.valueOf(reportData.getOrDefault("successRate", "0%")), "#67c23a");
        addStatCard(html, "节省工时", String.valueOf(reportData.getOrDefault("savedHours", 0)) + "h", "#e6a23c");
        addStatCard(html, "失败任务", String.valueOf(reportData.getOrDefault("failedTasks", 0)), "#f56c6c");

        html.append("</div>");

        // 趋势图表
        if (reportData.containsKey("trendChart")) {
            html.append("<div style='background: white; padding: 15px; border-radius: 8px; margin-bottom: 15px;'>");
            html.append("<h4 style='margin: 0 0 10px 0;'>执行趋势</h4>");
            html.append(reportData.get("trendChart"));
            html.append("</div>");
        }

        // 机器人执行排行
        if (reportData.containsKey("robotChart")) {
            html.append("<div style='background: white; padding: 15px; border-radius: 8px;'>");
            html.append("<h4 style='margin: 0 0 10px 0;'>机器人执行排行</h4>");
            html.append(reportData.get("robotChart"));
            html.append("</div>");
        }

        // 页脚
        html.append("<div style='margin-top: 20px; padding-top: 15px; border-top: 1px solid #ddd; color: #999; font-size: 11px;'>");
        html.append("<p>此报告由RPA系统自动生成 | <a href='#' style='color:#667eea;'>查看更多 →</a></p>");
        html.append("</div>");

        html.append("</div></div>");

        return html.toString();
    }

    private void addStatCard(StringBuilder html, String label, String value, String color) {
        html.append("<div style='background: white; padding: 20px; border-radius: 8px; text-align: center;'>");
        html.append("<div style='font-size: 28px; font-weight: bold; color: ").append(color).append(";'>").append(value).append("</div>");
        html.append("<div style='color: #666; font-size: 12px; margin-top: 5px;'>").append(label).append("</div>");
        html.append("</div>");
    }

    // ==================== SVG图表生成器 ====================

    /**
     * 生成SVG折线图
     */
    private String generateSVGLineChart(List<String> labels, List<Integer> data1, List<Integer> data2) {
        int width = 600;
        int height = 300;
        int padding = 50;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width).append(" ").append(height).append("' style='font-family: Arial, sans-serif;'>");

        // 背景
        svg.append("<rect width='100%' height='100%' fill='white'/>");

        // 标题
        svg.append("<text x='").append(width / 2).append("' y='25' text-anchor='middle' font-size='16' font-weight='bold' fill='#333'>执行趋势</text>");

        // 计算坐标范围
        int maxValue = 1;
        for (int v : data1) maxValue = Math.max(maxValue, v);
        for (int v : data2) maxValue = Math.max(maxValue, v);

        // 绘制网格
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int y = padding + (height - 2 * padding) * i / gridLines;
            svg.append("<line x1='").append(padding).append("' y1='").append(y).append("' x2='").append(width - padding).append("' y2='").append(y).append("' stroke='#eee' stroke-dasharray='3,3'/>");
            svg.append("<text x='").append(padding - 10).append("' y='").append(y + 4).append("' text-anchor='end' font-size='10' fill='#999'>").append(maxValue * (gridLines - i) / gridLines).append("</text>");
        }

        // 绘制数据线1
        if (!data1.isEmpty()) {
            String path = buildLinePath(labels.size(), data1, width, height, padding, maxValue);
            svg.append("<path d='").append(path).append("' fill='none' stroke='#67c23a' stroke-width='3' stroke-linecap='round'/>");
            // 数据点
            for (int i = 0; i < data1.size(); i++) {
                int[] pos = getDataPointPosition(i, labels.size(), data1.get(i), width, height, padding, maxValue);
                svg.append("<circle cx='").append(pos[0]).append("' cy='").append(pos[1]).append("' r='4' fill='#67c23a'/>");
            }
        }

        // 绘制数据线2
        if (!data2.isEmpty()) {
            String path = buildLinePath(labels.size(), data2, width, height, padding, maxValue);
            svg.append("<path d='").append(path).append("' fill='none' stroke='#f56c6c' stroke-width='3' stroke-linecap='round'/>");
            // 数据点
            for (int i = 0; i < data2.size(); i++) {
                int[] pos = getDataPointPosition(i, labels.size(), data2.get(i), width, height, padding, maxValue);
                svg.append("<circle cx='").append(pos[0]).append("' cy='").append(pos[1]).append("' r='4' fill='#f56c6c'/>");
            }
        }

        // X轴标签
        for (int i = 0; i < labels.size(); i++) {
            int x = padding + (width - 2 * padding) * i / Math.max(1, labels.size() - 1);
            svg.append("<text x='").append(x).append("' y='").append(height - 15).append("' text-anchor='middle' font-size='10' fill='#666'>").append(labels.get(i)).append("</text>");
        }

        // 图例
        if (!data1.isEmpty()) {
            svg.append("<rect x='").append(width - 150).append("' y='35' width='12' height='12' fill='#67c23a' rx='2'/>");
            svg.append("<text x='").append(width - 133).append("' y='46' font-size='11' fill='#666'>成功</text>");
        }
        if (!data2.isEmpty()) {
            svg.append("<rect x='").append(width - 90).append("' y='35' width='12' height='12' fill='#f56c6c' rx='2'/>");
            svg.append("<text x='").append(width - 73).append("' y='46' font-size='11' fill='#666'>失败</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 生成SVG饼图
     */
    private String generateSVGPieChart(Map<String, Integer> data) {
        int width = 400;
        int height = 300;
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = 100;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width).append(" ").append(height).append("' style='font-family: Arial, sans-serif;'>");

        svg.append("<rect width='100%' height='100%' fill='white'/>");

        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) total = 1;

        String[] colors = {"#67c23a", "#f56c6c", "#e6a23c", "#409eff", "#909399"};
        int i = 0;
        double startAngle = -90;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double angle = (entry.getValue() * 360.0) / total;
            String color = colors[i % colors.length];

            int[] path = describeArc(centerX, centerY, radius, startAngle, startAngle + angle);

            svg.append("<path d='M ").append(centerX).append(" ").append(centerY)
               .append(" L ").append(path[0]).append(" ").append(path[1])
               .append(" A ").append(radius).append(" ").append(radius).append(" 0 ")
               .append(angle > 180 ? 1 : 0).append(" 1 ")
               .append(path[2]).append(" ").append(path[3])
               .append(" Z' fill='").append(color).append("' stroke='white' stroke-width='2'/>");

            // 标签
            double midAngle = Math.toRadians(startAngle + angle / 2);
            int labelX = centerX + (radius + 30) * (int) Math.cos(midAngle);
            int labelY = centerY + (radius + 30) * (int) Math.sin(midAngle);
            double percentage = (entry.getValue() * 100.0) / total;

            svg.append("<text x='").append(labelX).append("' y='").append(labelY).append("' text-anchor='middle' font-size='12' fill='#333' font-weight='bold'>")
               .append(String.format("%.1f%%", percentage)).append("</text>");

            svg.append("<text x='").append(labelX).append("' y='").append(labelY + 15).append("' text-anchor='middle' font-size='10' fill='#666'>")
               .append(entry.getKey()).append("</text>");

            startAngle += angle;
            i++;
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 生成SVG柱状图
     */
    private String generateSVGBarChart(List<String> labels, List<Integer> values, String color) {
        int width = 600;
        int height = 300;
        int padding = 50;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width).append(" ").append(height).append("' style='font-family: Arial, sans-serif;'>");

        svg.append("<rect width='100%' height='100%' fill='white'/>");
        svg.append("<text x='").append(width / 2).append("' y='25' text-anchor='middle' font-size='16' font-weight='bold' fill='#333'>执行统计</text>");

        int maxValue = values.isEmpty() ? 1 : values.stream().mapToInt(Integer::intValue).max().orElse(1);

        // 绘制网格
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int y = padding + (height - 2 * padding) * i / gridLines;
            svg.append("<line x1='").append(padding).append("' y1='").append(y).append("' x2='").append(width - padding).append("' y2='").append(y).append("' stroke='#eee' stroke-dasharray='3,3'/>");
            svg.append("<text x='").append(padding - 10).append("' y='").append(y + 4).append("' text-anchor='end' font-size='10' fill='#999'>").append(maxValue * (gridLines - i) / gridLines).append("</text>");
        }

        // 绘制柱状图
        int barWidth = (width - 2 * padding) / Math.max(1, labels.size()) - 20;
        for (int i = 0; i < values.size(); i++) {
            int barHeight = (values.get(i) * (height - 2 * padding)) / maxValue;
            int x = padding + 10 + i * ((width - 2 * padding) / Math.max(1, labels.size()));
            int y = height - padding - barHeight;

            svg.append("<rect x='").append(x).append("' y='").append(y).append("' width='").append(barWidth).append("' height='").append(barHeight).append("' fill='").append(color).append("' rx='4'/>");
            svg.append("<text x='").append(x + barWidth / 2).append("' y='").append(y - 5).append("' text-anchor='middle' font-size='10' fill='#333'>").append(values.get(i)).append("</text>");
            svg.append("<text x='").append(x + barWidth / 2).append("' y='").append(height - padding + 15).append("' text-anchor='middle' font-size='10' fill='#666'>").append(truncateLabel(labels.get(i), 8)).append("</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * 生成SVG仪表盘
     */
    private String generateSVGGauge(String label, double value, double min, double max) {
        int width = 300;
        int height = 200;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(width).append(" ").append(height).append("' style='font-family: Arial, sans-serif;'>");

        svg.append("<rect width='100%' height='100%' fill='white'/>");

        int centerX = width / 2;
        int centerY = height - 30;
        int radius = 120;

        // 背景弧
        svg.append("<path d='").append(describeArcPath(centerX, centerY, radius, -180, 0)).append("' fill='none' stroke='#eee' stroke-width='20' stroke-linecap='round'/>");

        // 彩色分段
        String[] colors = {"#f56c6c", "#e6a23c", "#67c23a"};
        int[] ranges = {33, 33, 34};
        int startAngle = -180;

        for (int i = 0; i < colors.length; i++) {
            int angle = ranges[i] * 180 / 100;
            svg.append("<path d='").append(describeArcPath(centerX, centerY, radius, startAngle, startAngle + angle)).append("' fill='none' stroke='").append(colors[i]).append("' stroke-width='16' stroke-linecap='butt'/>");
            startAngle += angle;
        }

        // 指针
        double percentage = Math.min(1, Math.max(0, (value - min) / (max - min)));
        double angle = -180 + percentage * 180;
        double rad = Math.toRadians(angle);
        int pointerLength = radius - 25;
        int pointerX = centerX + (int) (pointerLength * Math.cos(rad));
        int pointerY = centerY + (int) (pointerLength * Math.sin(rad));

        svg.append("<line x1='").append(centerX).append("' y1='").append(centerY).append("' x2='").append(pointerX).append("' y2='").append(pointerY).append("' stroke='#333' stroke-width='3' stroke-linecap='round'/>");
        svg.append("<circle cx='").append(centerX).append("' cy='").append(centerY).append("' r='8' fill='#333'/>");

        // 数值
        svg.append("<text x='").append(centerX).append("' y='").append(centerY - 20).append("' text-anchor='middle' font-size='24' font-weight='bold' fill='#333'>").append(String.format("%.1f", value)).append("</text>");
        svg.append("<text x='").append(centerX).append("' y='").append(centerY + 45).append("' text-anchor='middle' font-size='12' fill='#666'>").append(label).append("</text>");

        svg.append("</svg>");
        return svg.toString();
    }

    // ==================== 辅助方法 ====================

    private String buildLinePath(int count, List<Integer> data, int width, int height, int padding, int maxValue) {
        if (data.isEmpty()) return "";

        StringBuilder path = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            int[] pos = getDataPointPosition(i, count, data.get(i), width, height, padding, maxValue);
            if (i == 0) {
                path.append("M ").append(pos[0]).append(" ").append(pos[1]);
            } else {
                path.append(" L ").append(pos[0]).append(" ").append(pos[1]);
            }
        }
        return path.toString();
    }

    private int[] getDataPointPosition(int index, int count, int value, int width, int height, int padding, int maxValue) {
        int x = padding + (width - 2 * padding) * index / Math.max(1, count - 1);
        int y = height - padding - (value * (height - 2 * padding)) / maxValue;
        return new int[]{x, y};
    }

    private int[] describeArc(int cx, int cy, int r, double startAngle, double endAngle) {
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);

        int x1 = cx + r * (int) Math.cos(startRad);
        int y1 = cy + r * (int) Math.sin(startRad);
        int x2 = cx + r * (int) Math.cos(endRad);
        int y2 = cy + r * (int) Math.sin(endRad);

        return new int[]{x1, y1, x2, y2};
    }

    private String describeArcPath(int cx, int cy, int r, double startAngle, double endAngle) {
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(endAngle);

        int x1 = cx + r * (int) Math.cos(startRad);
        int y1 = cy + r * (int) Math.sin(startRad);
        int x2 = cx + r * (int) Math.cos(endRad);
        int y2 = cy + r * (int) Math.sin(endRad);

        int largeArcFlag = (endAngle - startAngle) > 180 ? 1 : 0;

        return String.format("M %d %d A %d %d 0 %d 1 %d %d", x1, y1, r, r, largeArcFlag, x2, y2);
    }

    private String truncateLabel(String label, int maxLength) {
        if (label.length() <= maxLength) return label;
        return label.substring(0, maxLength - 1) + "…";
    }

    private String generateHTMLEmbed(String svg, String title) {
        return "<div style='text-align:center;'>" +
               "<h4 style='margin:0 0 10px 0;color:#333;'>" + title + "</h4>" +
               svg +
               "</div>";
    }
}
