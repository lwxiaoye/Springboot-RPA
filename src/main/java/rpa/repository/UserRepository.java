package rpa.repository;

import rpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层接口
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据关键字搜索用户（用户名或真实姓名）
     */
    @Query("SELECT u FROM User u WHERE u.status = 1 AND (u.username LIKE %:keyword% OR u.realName LIKE %:keyword%)")
    List<User> searchUsers(@Param("keyword") String keyword);

    /**
     * 根据部门查询用户
     */
    List<User> findByDepartmentAndStatus(String department, Integer status);

    /**
     * 获取所有启用的用户
     */
    List<User> findByStatus(Integer status);
}
