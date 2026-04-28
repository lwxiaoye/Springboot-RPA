package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.service.ReportChartService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报表图表控制器
 * <p>
 * 提供图表数据生成接口，支持在邮件中内嵌预览。
 * </p>
 *
 * @author RPA System
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/report-chart")
@RequiredArgsConstructor
@CrossOrigin
public class ReportChartController {

    private final ReportChartService chartService;

    /**
     * 获取执行趋势图
     */
    @GetMapping("/trend")
    public Map<String, Object> getExecutionTrend(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = new HashMap<>();

            // 模拟数据
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = startDate != null ? LocalDate.parse(startDate, formatter) : LocalDate.now().minusDays(7);
            LocalDate end = endDate != null ? LocalDate.parse(endDate, formatter) : LocalDate.now();

            List<String> dates = new ArrayList<>();
            List<Integer> success = new ArrayList<>();
            List<Integer> failed = new ArrayList<>();

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                dates.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                success.add(50 + (int)(Math.random() * 30));
                failed.add((int)(Math.random() * 5));
            }

            data.put("dates", dates);
            data.put("success", success);
            data.put("failed", failed);

            var chartResponse = chartService.generateExecutionTrendChart(data);

            response.put("code", 0);
            response.put("data", Map.of(
                    "svg", chartResponse.getSvg(),
                    "html", chartResponse.getHtml()
            ));
        } catch (Exception e) {
            log.error("获取趋势图失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取成功率饼图
     */
    @GetMapping("/success-rate")
    public Map<String, Object> getSuccessRate(@RequestParam(defaultValue = "100") int successCount,
                                             @RequestParam(defaultValue = "5") int failedCount) {
        Map<String, Object> response = new HashMap<>();
        try {
            var chartResponse = chartService.generateSuccessRatePieChart(successCount, failedCount);

            response.put("code", 0);
            response.put("data", Map.of(
                    "svg", chartResponse.getSvg(),
                    "html", chartResponse.getHtml(),
                    "successRate", successCount * 100.0 / (successCount + failedCount)
            ));
        } catch (Exception e) {
            log.error("获取成功率图失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取机器人效率图
     */
    @GetMapping("/robot-efficiency")
    public Map<String, Object> getRobotEfficiency() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> robotData = new ArrayList<>();

            // 模拟机器人数据
            String[] robots = {"RPA-Robot-01", "RPA-Robot-02", "RPA-Robot-03", "RPA-Robot-04", "RPA-Robot-05"};
            for (String robot : robots) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", robot);
                item.put("executions", 50 + (int)(Math.random() * 100));
                robotData.add(item);
            }

            var chartResponse = chartService.generateRobotEfficiencyChart(robotData);

            response.put("code", 0);
            response.put("data", Map.of(
                    "svg", chartResponse.getSvg(),
                    "html", chartResponse.getHtml()
            ));
        } catch (Exception e) {
            log.error("获取机器人效率图失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取ROI趋势图
     */
    @GetMapping("/roi-trend")
    public Map<String, Object> getROITrend() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> months = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            // 模拟12个月的数据
            for (int i = 1; i <= 12; i++) {
                months.add(i + "月");
                values.add(100 + Math.random() * 50);
            }

            var chartResponse = chartService.generateROITrendChart(months, values);

            response.put("code", 0);
            response.put("data", Map.of(
                    "svg", chartResponse.getSvg(),
                    "html", chartResponse.getHtml()
            ));
        } catch (Exception e) {
            log.error("获取ROI趋势图失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取仪表盘图
     */
    @GetMapping("/gauge")
    public Map<String, Object> getGauge(@RequestParam(defaultValue = "85") double value,
                                       @RequestParam(defaultValue = "0") double min,
                                       @RequestParam(defaultValue = "100") double max) {
        Map<String, Object> response = new HashMap<>();
        try {
            var chartResponse = chartService.generateGaugeChart("效率指数", value, min, max);

            response.put("code", 0);
            response.put("data", Map.of(
                    "svg", chartResponse.getSvg(),
                    "html", chartResponse.getHtml()
            ));
        } catch (Exception e) {
            log.error("获取仪表盘失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取完整日报HTML（包含内嵌图表）
     */
    @GetMapping("/daily-report-html")
    public Map<String, Object> getDailyReportHTML() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            reportData.put("totalExecutions", 156);
            reportData.put("successRate", "96.8%");
            reportData.put("savedHours", 128);
            reportData.put("failedTasks", 5);

            // 生成趋势图
            Map<String, Object> trendData = new HashMap<>();
            List<String> dates = new ArrayList<>();
            List<Integer> success = new ArrayList<>();
            List<Integer> failed = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                dates.add(LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")));
                success.add(20 + (int)(Math.random() * 10));
                failed.add((int)(Math.random() * 3));
            }
            trendData.put("dates", dates);
            trendData.put("success", success);
            trendData.put("failed", failed);
            var trendChart = chartService.generateExecutionTrendChart(trendData);
            reportData.put("trendChart", trendChart.getSvg());

            // 生成机器人排行图
            List<Map<String, Object>> robotData = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> robot = new HashMap<>();
                robot.put("name", "Robot-" + i);
                robot.put("executions", 30 + (int)(Math.random() * 50));
                robotData.add(robot);
            }
            var robotChart = chartService.generateRobotEfficiencyChart(robotData);
            reportData.put("robotChart", robotChart.getSvg());

            String html = chartService.generateDailyReportHTML(reportData);

            response.put("code", 0);
            response.put("data", Map.of("html", html));
        } catch (Exception e) {
            log.error("获取日报HTML失败", e);
            response.put("code", -1);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
