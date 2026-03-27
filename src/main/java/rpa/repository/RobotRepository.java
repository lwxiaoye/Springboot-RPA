package rpa.repository;

import rpa.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * 机器人数据访问层接口
 * <p>
 * 提供机器人实体的数据库操作方法。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface RobotRepository extends JpaRepository<Robot, Long> {
    
    /**
     * 根据机器人名称查询
     *
     * @param name 机器人名称
     * @return Optional<Robot> 机器人信息
     */
    Optional<Robot> findByName(String name);
    
    /**
     * 根据状态查询机器人列表
     *
     * @param status 状态（idle/busy/offline）
     * @return List<Robot> 机器人列表
     */
    List<Robot> findByStatus(String status);
}
