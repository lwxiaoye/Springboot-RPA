package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.Robot;
import rpa.repository.RobotRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 机器人服务类
 * <p>
 * 提供机器人相关的业务逻辑处理，包括机器人CRUD和状态管理。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository repository;

    /**
     * 查询所有机器人
     */
    public List<Robot> findAll() {
        return repository.findAll();
    }

    /**
     * 查询空闲状态的机器人
     */
    public List<Robot> findIdleRobots() {
        return repository.findByStatus("idle");
    }

    /**
     * 根据分类查询机器人
     */
    public List<Robot> findByCategory(String category) {
        return repository.findByRobotCategory(category);
    }

    /**
     * 查询指定分类的空闲机器人
     */
    public List<Robot> findIdleRobotsByCategory(String category) {
        return repository.findByRobotCategoryAndStatus(category, "idle");
    }

    /**
     * 根据绑定的流程ID查询机器人
     */
    public List<Robot> findByBoundProcessId(Long processId) {
        return repository.findByBoundProcessId(processId);
    }

    /**
     * 根据ID查询机器人
     */
    public Optional<Robot> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 创建机器人
     *
     * @param name 名称
     * @param robotCategory 分类
     * @param capabilities 能力
     * @param ip IP地址
     * @param hostname 主机名
     * @param port 端口
     * @param description 描述
     * @param boundProcessId 绑定的流程ID
     * @param boundProcessName 绑定的流程名称
     * @param robotCode 机器人执行代码
     * @param status 状态
     * @return 创建的机器人
     */
    public Robot create(String name, String robotCategory, String capabilities, String ip, String hostname, Integer port, String description, Long boundProcessId, String boundProcessName, String robotCode, String status) {
        // 检查名称是否已存在
        if (repository.findByName(name).isPresent()) {
            throw new RuntimeException("机器人名称已存在，请使用其他名称");
        }

        Robot robot = new Robot();
        robot.setName(name);
        robot.setRobotCategory(robotCategory);
        robot.setCapabilities(capabilities);
        robot.setIp(ip);
        robot.setHostname(hostname);
        robot.setPort(port);
        robot.setDescription(description);
        robot.setBoundProcessId(boundProcessId);
        robot.setBoundProcessName(boundProcessName);
        robot.setRobotCode(robotCode);
        // 如果传入了状态则使用，否则默认为idle
        robot.setStatus(status != null ? status : "idle");
        robot.setCpuUsage(0);
        robot.setMemoryUsage(0);
        robot.setLastHeartbeat(LocalDateTime.now());
        robot.setCreateTime(LocalDateTime.now());
        return repository.save(robot);
    }

    /**
     * 更新机器人状态
     *
     * @param id 机器人ID
     * @param status 状态（idle/busy/offline）
     * @return 更新后的机器人
     */
    public Robot updateStatus(Long id, String status) {
        return repository.findById(id).map(robot -> {
            robot.setStatus(status);
            robot.setLastHeartbeat(LocalDateTime.now());
            return repository.save(robot);
        }).orElseThrow(() -> new RuntimeException("机器人不存在"));
    }

    /**
     * 更新机器人信息
     *
     * @param id 机器人ID
     * @param name 名称
     * @param robotCategory 分类
     * @param capabilities 能力
     * @param ip IP地址
     * @param hostname 主机名
     * @param port 端口
     * @param description 描述
     * @param boundProcessId 绑定的流程ID
     * @param boundProcessName 绑定的流程名称
     * @param robotCode 机器人执行代码
     * @param status 状态
     * @param enabled 是否启用
     * @return 更新后的机器人
     */
    public Robot update(Long id, String name, String robotCategory, String capabilities, String ip, String hostname, Integer port, String description, Long boundProcessId, String boundProcessName, String robotCode, String status, Boolean enabled) {
        return repository.findById(id).map(robot -> {
            if (name != null) {
                robot.setName(name);
            }
            if (robotCategory != null) {
                robot.setRobotCategory(robotCategory);
            }
            if (capabilities != null) {
                robot.setCapabilities(capabilities);
            }
            if (ip != null) {
                robot.setIp(ip);
            }
            if (hostname != null) {
                robot.setHostname(hostname);
            }
            if (port != null) {
                robot.setPort(port);
            }
            if (description != null) {
                robot.setDescription(description);
            }
            if (boundProcessId != null) {
                robot.setBoundProcessId(boundProcessId);
            }
            if (boundProcessName != null) {
                robot.setBoundProcessName(boundProcessName);
            }
            if (robotCode != null) {
                robot.setRobotCode(robotCode);
            }
            if (status != null) {
                robot.setStatus(status);
                robot.setLastHeartbeat(LocalDateTime.now());
            }
            if (enabled != null) {
                robot.setEnabled(enabled);
            }
            return repository.save(robot);
        }).orElseThrow(() -> new RuntimeException("机器人不存在"));
    }

    /**
     * 删除机器人
     */
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
