package rpa.repository;

import rpa.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {
    Optional<Robot> findByName(String name);
    List<Robot> findByStatus(String status);
}
