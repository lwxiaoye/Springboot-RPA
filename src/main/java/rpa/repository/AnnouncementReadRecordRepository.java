package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rpa.entity.AnnouncementReadRecord;
import java.util.List;
import java.util.Optional;

/**
 * 公告阅读记录数据访问层
 */
@Repository
public interface AnnouncementReadRecordRepository extends JpaRepository<AnnouncementReadRecord, Long> {

    /**
     * 根据公告ID和用户ID查询阅读记录
     */
    Optional<AnnouncementReadRecord> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    /**
     * 根据公告ID查询所有阅读记录
     */
    List<AnnouncementReadRecord> findByAnnouncementId(Long announcementId);

    /**
     * 统计公告已读人数
     */
    long countByAnnouncementIdAndIsReadTrue(Long announcementId);

    /**
     * 查询用户已读的公告ID列表
     */
    @Query("SELECT ar.announcementId FROM AnnouncementReadRecord ar WHERE ar.userId = :userId AND ar.isRead = true")
    List<Long> findReadAnnouncementIdsByUserId(Long userId);
}
