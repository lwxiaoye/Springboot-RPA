package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rpa.entity.Announcement;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * 查询已发布的公告列表（按发布时间倒序）
     */
    List<Announcement> findByStatusOrderByPublishTimeDesc(String status);

    /**
     * 查询指定用户可见的公告
     */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'published' " +
           "AND (a.scope = 'all' OR a.scope = :scope) " +
           "AND (a.expireTime IS NULL OR a.expireTime > :now) " +
           "ORDER BY CASE a.priority WHEN 'urgent' THEN 0 WHEN 'important' THEN 1 ELSE 2 END, a.publishTime DESC")
    List<Announcement> findVisibleAnnouncements(String scope, LocalDateTime now);

    /**
     * 根据优先级查询未读公告数量
     */
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.status = 'published' AND a.priority = :priority")
    Long countByPriority(String priority);

    /**
     * 统计未过期公告数量
     */
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.status = 'published' AND (a.expireTime IS NULL OR a.expireTime > :now)")
    Long countActiveAnnouncements(LocalDateTime now);
}
