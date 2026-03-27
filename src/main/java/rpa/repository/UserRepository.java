package rpa.repository;

import rpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 用户数据访问层接口
 * <p>
 * 提供用户实体的数据库操作方法。
 * 继承JpaRepository获得基本的CRUD功能。
 * </p>
 *
 * @author RPA System
 * @version 1.0.0
 * @since 2024-01-01
 * @see JpaRepository
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return Optional<User> 用户信息
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return Optional<User> 用户信息
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return Optional<User> 用户信息
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return boolean 是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return boolean 是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return boolean 是否存在
     */
    boolean existsByPhone(String phone);
}
