package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rpa.entity.Credential;

import java.util.List;
import java.util.Optional;

/**
 * 凭据数据访问层
 */
@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /**
     * 根据名称查找凭据
     */
    Optional<Credential> findByName(String name);

    /**
     * 根据类型查找凭据
     */
    List<Credential> findByType(String type);

    /**
     * 根据状态查找凭据
     */
    List<Credential> findByStatus(String status);

    /**
     * 查找所有激活的凭据
     */
    List<Credential> findByStatusOrderByCreateTimeDesc(String status);

    /**
     * 搜索凭据（名称、用户名、描述）
     */
    @Query("SELECT c FROM Credential c WHERE c.status = 'active' AND " +
           "(c.name LIKE %:keyword% OR c.username LIKE %:keyword% OR c.description LIKE %:keyword%)")
    List<Credential> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
}