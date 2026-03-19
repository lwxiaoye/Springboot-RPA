package rpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rpa.entity.Robot;
import rpa.repository.RobotRepository;
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

    public Robot create(String name, String type, String capabilities) {
        Robot robot = new Robot();
        robot.setName(name);
        robot.setType(type);
        robot.setCapabilities(capabilities);
        return repository.save(robot);
    }

    public Robot updateStatus(Long id, String status) {
        return repository.findById(id).map(robot -> {
            robot.setStatus(status);
            robot.setLastHeartbeat(java.time.LocalDateTime.now());
            return repository.save(robot);
        }).orElseThrow(() -> new RuntimeException("机器人不存在"));
    }

    public Robot update(Long id, String name, String type, String capabilities) {
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
            return repository.save(robot);
        }).orElseThrow(() -> new RuntimeException("机器人不存在"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
