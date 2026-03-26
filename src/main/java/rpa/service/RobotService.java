package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.Robot;
import rpa.repository.RobotRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository repository;

    public List<Robot> findAll() {
        return repository.findAll();
    }

    public List<Robot> findIdleRobots() {
        return repository.findByStatus("idle");
    }

    public Optional<Robot> findById(Long id) {
        return repository.findById(id);
    }

    public Robot create(String name, String type, String capabilities, String ip, String hostname, Integer port, String description) {
        Robot robot = new Robot();
        robot.setName(name);
        robot.setType(type);
        robot.setCapabilities(capabilities);
        robot.setIp(ip);
        robot.setHostname(hostname);
        robot.setPort(port);
        robot.setDescription(description);
        robot.setStatus("idle");
        robot.setCpuUsage(0);
        robot.setMemoryUsage(0);
        robot.setLastHeartbeat(LocalDateTime.now());
        robot.setCreateTime(LocalDateTime.now());
        return repository.save(robot);
    }

    public Robot updateStatus(Long id, String status) {
        return repository.findById(id).map(robot -> {
            robot.setStatus(status);
            robot.setLastHeartbeat(LocalDateTime.now());
            return repository.save(robot);
        }).orElseThrow(() -> new RuntimeException("机器人不存在"));
    }

    public Robot update(Long id, String name, String type, String capabilities, String ip, String hostname, Integer port, String description) {
        return repository.findById(id).map(robot -> {
            if (name != null) {
                robot.setName(name);
            }
            if (type != null) {
                robot.setType(type);
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
            return repository.save(robot);
        }).orElseThrow(() -> new RuntimeException("机器人不存在"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
