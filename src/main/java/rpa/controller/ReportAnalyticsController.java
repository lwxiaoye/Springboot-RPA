package rpa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import rpa.entity.CustomReport;
import rpa.entity.ReportSubscription;
import rpa.service.ReportAnalyticsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表分析控制器
 * <p>
 * 提供RPA系统报表分析相关的RESTful API接口，包括：
 * <ul>
 *   <li>概览统计：系统整体运行数据</li>
 *   <li>日报/月报：任务执行统计分析</li>
 *   <li>机器人利用率：工作负载分析</li>
 *   <li>流程效率：耗时排行和优化建议</li>
 *   <li>ROI分析：成本节省和投资回报计算</li>
 *   <li>趋势预测：智能任务量预测</li>
 *   <li>自定义报表：用户自定义报表管理</li>
 *   <li>报表订阅：定时推送配置管理</li>
 * </ul>
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@CrossOrigin
public class ReportAnalyticsController {

    private final ReportAnalyticsService reportService;

    // ==================== 概览统计 ====================

    /**
     * 获取系统概览统计数据
     *
     * @return 概览统计数据
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = reportService.getOverviewStats();
            response.put("code", 0);
            response.put("data", stats);
        } catch (Exception e) {
            log.error("获取概览统计失败", e);
            response.put("code", -1);
            response.put("message", "获取统计数据失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 日报统计 ====================

    /**
     * 获取指定日期的执行统计
     *
     * @param date 日期（格式：YYYY-MM-DD）
     * @return 日报统计数据
     */
    @GetMapping("/daily")
    public Map<String, Object> getDailyStats(@RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = reportService.getDailyStats(date);
            response.put("code", 0);
            response.put("data", stats);
        } catch (Exception e) {
            log.error("获取日报统计失败, date={}", date, e);
            response.put("code", -1);
            response.put("message", "获取日报数据失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 月报统计 ====================

    /**
     * 获取指定月份的执行统计
     *
     * @param yearMonth 年月（格式：YYYY-MM）
     * @return 月报统计数据
     */
    @GetMapping("/monthly")
    public Map<String, Object> getMonthlyStats(@RequestParam String yearMonth) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = reportService.getMonthlyStats(yearMonth);
            response.put("code", 0);
            response.put("data", stats);
        } catch (Exception e) {
            log.error("获取月报统计失败, yearMonth={}", yearMonth, e);
            response.put("code", -1);
            response.put("message", "获取月报数据失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 机器人利用率 ====================

    /**
     * 获取机器人利用率统计
     *
     * @return 机器人利用率数据
     */
    @GetMapping("/robot-utilization")
    public Map<String, Object> getRobotUtilization() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = reportService.getRobotUtilizationStats();
            response.put("code", 0);
            response.put("data", stats);
        } catch (Exception e) {
            log.error("获取机器人利用率失败", e);
            response.put("code", -1);
            response.put("message", "获取机器人数据失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 流程效率分析 ====================

    /**
     * 获取流程耗时排行
     *
     * @param topN 显示前N个流程（默认10）
     * @return 流程效率数据
     */
    @GetMapping("/process-efficiency")
    public Map<String, Object> getProcessEfficiency(
            @RequestParam(defaultValue = "10") int topN) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = reportService.getProcessEfficiencyStats(topN);
            response.put("code", 0);
            response.put("data", stats);
        } catch (Exception e) {
            log.error("获取流程效率分析失败", e);
            response.put("code", -1);
            response.put("message", "获取流程数据失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== ROI分析 ====================

    /**
     * 计算ROI投资回报率
     *
     * @param params ROI计算参数
     * @return ROI分析结果
     */
    @PostMapping("/roi/calculate")
    public Map<String, Object> calculateROI(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = reportService.calculateROI(params);
            response.put("code", 0);
            response.put("data", result);
        } catch (Exception e) {
            log.error("计算ROI失败", e);
            response.put("code", -1);
            response.put("message", "计算ROI失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 趋势预测 ====================

    /**
     * 预测未来任务量趋势
     *
     * @param days 预测天数（7/30/90）
     * @return 预测结果
     */
    @GetMapping("/forecast")
    public Map<String, Object> forecastTrend(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = reportService.forecastTrend(days);
            response.put("code", 0);
            response.put("data", result);
        } catch (Exception e) {
            log.error("趋势预测失败", e);
            response.put("code", -1);
            response.put("message", "趋势预测失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 自定义报表管理 ====================

    /**
     * 创建自定义报表
     *
     * @param report 报表配置
     * @return 创建的报表
     */
    @PostMapping("/custom")
    public Map<String, Object> createCustomReport(@RequestBody CustomReport report) {
        Map<String, Object> response = new HashMap<>();
        try {
            CustomReport saved = reportService.createCustomReport(report);
            response.put("code", 0);
            response.put("data", saved);
            response.put("message", "报表创建成功");
        } catch (Exception e) {
            log.error("创建自定义报表失败", e);
            response.put("code", -1);
            response.put("message", "创建报表失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 更新自定义报表
     *
     * @param id     报表ID
     * @param report 报表配置
     * @return 更新后的报表
     */
    @PutMapping("/custom/{id}")
    public Map<String, Object> updateCustomReport(
            @PathVariable Long id,
            @RequestBody CustomReport report) {
        Map<String, Object> response = new HashMap<>();
        try {
            CustomReport updated = reportService.updateCustomReport(id, report);
            response.put("code", 0);
            response.put("data", updated);
            response.put("message", "报表更新成功");
        } catch (Exception e) {
            log.error("更新自定义报表失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", "更新报表失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 删除自定义报表
     *
     * @param id 报表ID
     * @return 操作结果
     */
    @DeleteMapping("/custom/{id}")
    public Map<String, Object> deleteCustomReport(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            reportService.deleteCustomReport(id);
            response.put("code", 0);
            response.put("message", "报表删除成功");
        } catch (Exception e) {
            log.error("删除自定义报表失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", "删除报表失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 查询自定义报表列表
     *
     * @param userId 用户ID（可选）
     * @return 报表列表
     */
    @GetMapping("/custom")
    public Map<String, Object> listCustomReports(
            @RequestParam(required = false) Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CustomReport> reports = reportService.listCustomReports(userId);
            response.put("code", 0);
            response.put("data", reports);
        } catch (Exception e) {
            log.error("查询自定义报表列表失败", e);
            response.put("code", -1);
            response.put("message", "查询报表列表失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 运行自定义报表
     *
     * @param id 报表ID
     * @return 报表数据
     */
    @PostMapping("/custom/{id}/run")
    public Map<String, Object> runCustomReport(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = reportService.runCustomReport(id);
            response.put("code", 0);
            response.put("data", result);
            response.put("message", "报表运行成功");
        } catch (Exception e) {
            log.error("运行自定义报表失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", "运行报表失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 报表订阅管理 ====================

    /**
     * 创建报表订阅
     *
     * @param subscription 订阅配置
     * @return 创建的订阅
     */
    @PostMapping("/subscription")
    public Map<String, Object> createSubscription(@RequestBody ReportSubscription subscription) {
        Map<String, Object> response = new HashMap<>();
        try {
            ReportSubscription saved = reportService.createSubscription(subscription);
            response.put("code", 0);
            response.put("data", saved);
            response.put("message", "订阅创建成功");
        } catch (Exception e) {
            log.error("创建报表订阅失败", e);
            response.put("code", -1);
            response.put("message", "创建订阅失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 更新报表订阅
     *
     * @param id           订阅ID
     * @param subscription 订阅配置
     * @return 更新后的订阅
     */
    @PutMapping("/subscription/{id}")
    public Map<String, Object> updateSubscription(
            @PathVariable Long id,
            @RequestBody ReportSubscription subscription) {
        Map<String, Object> response = new HashMap<>();
        try {
            ReportSubscription updated = reportService.updateSubscription(id, subscription);
            response.put("code", 0);
            response.put("data", updated);
            response.put("message", "订阅更新成功");
        } catch (Exception e) {
            log.error("更新报表订阅失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", "更新订阅失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 删除报表订阅
     *
     * @param id 订阅ID
     * @return 操作结果
     */
    @DeleteMapping("/subscription/{id}")
    public Map<String, Object> deleteSubscription(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            reportService.deleteSubscription(id);
            response.put("code", 0);
            response.put("message", "订阅删除成功");
        } catch (Exception e) {
            log.error("删除报表订阅失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", "删除订阅失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 切换订阅状态
     *
     * @param id 订阅ID
     * @return 更新后的订阅
     */
    @PutMapping("/subscription/{id}/toggle")
    public Map<String, Object> toggleSubscription(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            ReportSubscription updated = reportService.toggleSubscription(id);
            response.put("code", 0);
            response.put("data", updated);
            response.put("message", updated.getEnabled() ? "订阅已启用" : "订阅已禁用");
        } catch (Exception e) {
            log.error("切换订阅状态失败, id={}", id, e);
            response.put("code", -1);
            response.put("message", "操作失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 查询订阅列表
     *
     * @param userId 用户ID（可选）
     * @return 订阅列表
     */
    @GetMapping("/subscription")
    public Map<String, Object> listSubscriptions(
            @RequestParam(required = false) Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ReportSubscription> subscriptions = reportService.listSubscriptions(userId);
            response.put("code", 0);
            response.put("data", subscriptions);
        } catch (Exception e) {
            log.error("查询订阅列表失败", e);
            response.put("code", -1);
            response.put("message", "查询订阅列表失败: " + e.getMessage());
        }
        return response;
    }
}
