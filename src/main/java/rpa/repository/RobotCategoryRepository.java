package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rpa.entity.RobotCategory;
import java.util.Optional;

@Repository
public interface RobotCategoryRepository extends JpaRepository<RobotCategory, Long> {
    Optional<RobotCategory> findByCode(String code);
    boolean existsByCode(String code);
}
