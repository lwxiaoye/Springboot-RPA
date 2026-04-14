package rpa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rpa.entity.RobotHealth;
import rpa.entity.RobotBackup;
import rpa.repository.RobotHealthRepository;
import rpa.repository.RobotBackupRepository;
import rpa.repository.RobotRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/robot-health")
public class RobotHealthController {

    @Autowired
    private RobotHealthRepository robotHealthRepository;

    @Autowired
    private RobotBackupRepository robotBackupRepository;

    @Autowired
    private RobotRepository robotRepository;

    @GetMapping
    public Map<String, Object> getAllRobotHealth() {
        List<RobotHealth> healthList = robotHealthRepository.findAll();

        // 按机器人分组，取最新记录
        Map<Long, RobotHealth> latestHealth = healthList.stream()
            .collect(Collectors.toMap(
                RobotHealth::getRobotId,
                h -> h,
                (h1, h2) -> h1.getCheckTime().isAfter(h2.getCheckTime()) ? h1 : h2
            ));

        return success(latestHealth.values().stream().collect(Collectors.toList()));
    }

    @GetMapping("/{robotId}")
    public Map<String, Object> getRobotHealth(@PathVariable Long robotId) {
        return robotHealthRepository.findTopByRobotIdOrderByCheckTimeDesc(robotId)
            .map(this::success)
            .orElse(error("健康记录不存在"));
    }

    @GetMapping("/{robotId}/history")
    public Map<String, Object> getRobotHealthHistory(
            @PathVariable Long robotId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : LocalDateTime.now();

        List<RobotHealth> history = robotHealthRepository.findByRobotIdAndTimeRange(robotId, start, end);
        return success(history);
    }

    @GetMapping("/status/{status}")
    public Map<String, Object> getRobotsByStatus(@PathVariable String status) {
        List<RobotHealth> robots = robotHealthRepository.findByHealthStatus(status);
        return success(robots);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        List<RobotHealth> latestRecords = robotHealthRepository.findAll().stream()
            .collect(Collectors.toMap(
                RobotHealth::getRobotId,
                h -> h,
                (h1, h2) -> h1.getCheckTime().isAfter(h2.getCheckTime()) ? h1 : h2
            ))
            .values()
            .stream()
            .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("healthy", latestRecords.stream().filter(h -> "healthy".equals(h.getHealthStatus())).count());
        stats.put("warning", latestRecords.stream().filter(h -> "warning".equals(h.getHealthStatus())).count());
        stats.put("critical", latestRecords.stream().filter(h -> "critical".equals(h.getHealthStatus())).count());
        stats.put("offline", latestRecords.stream().filter(h -> "offline".equals(h.getHealthStatus())).count());

        double avgCpu = latestRecords.stream().mapToInt(h -> h.getCpuUsage() != null ? h.getCpuUsage() : 0).average().orElse(0);
        double avgMemory = latestRecords.stream().mapToInt(h -> h.getMemoryUsage() != null ? h.getMemoryUsage() : 0).average().orElse(0);
        stats.put("avgCpuUsage", Math.round(avgCpu * 10) / 10.0);
        stats.put("avgMemoryUsage", Math.round(avgMemory * 10) / 10.0);

        return success(stats);
    }

    @PostMapping
    public Map<String, Object> createHealthRecord(@RequestBody RobotHealth health) {
        try {
            health.setCheckTime(LocalDateTime.now());
            health.setCreateTime(LocalDateTime.now());

            // 计算健康评分
            int score = calculateHealthScore(health);
            health.setHealthScore(score);

            // 确定健康状态
            String status = determineHealthStatus(health);
            health.setHealthStatus(status);

            RobotHealth saved = robotHealthRepository.save(health);
            return success(saved);
        } catch (Exception e) {
            log.error("创建健康记录失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    private int calculateHealthScore(RobotHealth health) {
        int score = 100;

        // CPU扣分
        if (health.getCpuUsage() != null) {
            if (health.getCpuUsage() >= 95) score -= 40;
            else if (health.getCpuUsage() >= 85) score -= 25;
            else if (health.getCpuUsage() >= 70) score -= 10;
        }

        // 内存扣分
        if (health.getMemoryUsage() != null) {
            if (health.getMemoryUsage() >= 95) score -= 30;
            else if (health.getMemoryUsage() >= 85) score -= 20;
            else if (health.getMemoryUsage() >= 70) score -= 10;
        }

        // 磁盘扣分
        if (health.getDiskUsage() != null) {
            if (health.getDiskUsage() >= 90) score -= 20;
            else if (health.getDiskUsage() >= 80) score -= 10;
        }

        // 网络状态扣分
        if ("disconnected".equals(health.getNetworkStatus())) score -= 30;
        else if ("slow".equals(health.getNetworkStatus())) score -= 10;

        // 心跳超时扣分
        if (Boolean.TRUE.equals(health.getHeartbeatTimeout())) score -= 50;

        return Math.max(0, score);
    }

    private String determineHealthStatus(RobotHealth health) {
        if (Boolean.TRUE.equals(health.getHeartbeatTimeout())) {
            return "offline";
        }
        if (health.getHealthScore() != null && health.getHealthScore() < 50) {
            return "critical";
        }
        if (health.getHealthScore() != null && health.getHealthScore() < 70) {
            return "warning";
        }
        return "healthy";
    }

    // ==================== 机器人主备接口 ====================

    @GetMapping("/backup")
    public Map<String, Object> getAllBackups() {
        List<RobotBackup> backups = robotBackupRepository.findAll();
        return success(backups);
    }

    @GetMapping("/backup/{id}")
    public Map<String, Object> getBackup(@PathVariable Long id) {
        return robotBackupRepository.findById(id)
            .map(this::success)
            .orElse(error("主备关系不存在"));
    }

    @GetMapping("/backup/primary/{robotId}")
    public Map<String, Object> getBackupsByPrimary(@PathVariable Long robotId) {
        List<RobotBackup> backups = robotBackupRepository.findByPrimaryRobotId(robotId);
        return success(backups);
    }

    @GetMapping("/backup/backup/{robotId}")
    public Map<String, Object> getBackupsByBackup(@PathVariable Long robotId) {
        List<RobotBackup> backups = robotBackupRepository.findByBackupRobotId(robotId);
        return success(backups);
    }

    @GetMapping("/backup/process/{processId}")
    public Map<String, Object> getBackupsByProcess(@PathVariable Long processId) {
        List<RobotBackup> backups = robotBackupRepository.findByBoundProcessId(processId);
        return success(backups);
    }

    @PostMapping("/backup")
    public Map<String, Object> createBackup(@RequestBody RobotBackup backup) {
        try {
            backup.setCreateTime(LocalDateTime.now());
            backup.setUpdateTime(LocalDateTime.now());
            if (backup.getStatus() == null) {
                backup.setStatus("active");
            }
            RobotBackup saved = robotBackupRepository.save(backup);
            return success(saved);
        } catch (Exception e) {
            log.error("创建主备关系失败", e);
            return error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/backup/{id}")
    public Map<String, Object> updateBackup(@PathVariable Long id, @RequestBody RobotBackup backup) {
        try {
            backup.setId(id);
            backup.setUpdateTime(LocalDateTime.now());
            RobotBackup saved = robotBackupRepository.save(backup);
            return success(saved);
        } catch (Exception e) {
            log.error("更新主备关系失败", e);
            return error("更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/backup/{id}/switch")
    public Map<String, Object> switchBackup(@PathVariable Long id, @RequestBody Map<String, String> params) {
        try {
            RobotBackup backup = robotBackupRepository.findById(id).orElse(null);
            if (backup == null) {
                return error("主备关系不存在");
            }

            backup.setStatus("switched");
            backup.setLastSwitchTime(LocalDateTime.now());
            backup.setSwitchReason(params.getOrDefault("reason", "手动切换"));
            backup.setSwitchCount(backup.getSwitchCount() + 1);
            backup.setUpdateTime(LocalDateTime.now());

            robotBackupRepository.save(backup);

            // 更新机器人状态
            robotRepository.findById(backup.getPrimaryRobotId()).ifPresent(robot -> {
                robot.setStatus("offline");
                robotRepository.save(robot);
            });

            robotRepository.findById(backup.getBackupRobotId()).ifPresent(robot -> {
                robot.setStatus("idle");
                robotRepository.save(robot);
            });

            return success(backup);
        } catch (Exception e) {
            log.error("切换失败", e);
            return error("切换失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/backup/{id}")
    public Map<String, Object> deleteBackup(@PathVariable Long id) {
        try {
            robotBackupRepository.deleteById(id);
            return success("删除成功");
        } catch (Exception e) {
            log.error("删除主备关系失败", e);
            return error("删除失败: " + e.getMessage());
        }
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 1);
        result.put("message", message);
        return result;
    }
}
