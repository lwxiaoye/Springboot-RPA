package rpa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rpa.entity.Announcement;
import rpa.entity.AnnouncementReadRecord;
import rpa.entity.User;
import rpa.repository.AnnouncementReadRecordRepository;
import rpa.repository.AnnouncementRepository;
import rpa.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRecordRepository readRecordRepository;
    private final UserRepository userRepository;

    /**
     * 获取所有已发布的公告（包含用户的真实阅读状态）
     */
    public List<Map<String, Object>> getPublishedAnnouncements(Long userId) {
        List<Announcement> announcements = announcementRepository.findByStatusOrderByPublishTimeDesc("published");

        // 获取用户已读的公告ID列表
        Set<Long> readAnnouncementIds;
        if (userId != null) {
            List<Long> readIds = readRecordRepository.findReadAnnouncementIdsByUserId(userId);
            readAnnouncementIds = new HashSet<>(readIds);
        } else {
            readAnnouncementIds = new HashSet<>();
        }

        final Set<Long> finalReadIds = readAnnouncementIds;
        return announcements.stream().map(a -> {
            Map<String, Object> map = convertToMap(a);
            map.put("status", finalReadIds.contains(a.getId()) ? "read" : "unread");
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取草稿列表
     */
    public List<Announcement> getDrafts() {
        return announcementRepository.findByStatusOrderByPublishTimeDesc("draft");
    }

    /**
     * 获取公告详情
     */
    public Announcement getAnnouncementById(Long id) {
        return announcementRepository.findById(id).orElse(null);
    }

    /**
     * 发布公告
     */
    @Transactional
    public Announcement publishAnnouncement(Announcement announcement) {
        announcement.setStatus("published");
        if (announcement.getPublishTime() == null) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcement.setReadCount(0);
        Announcement saved = announcementRepository.save(announcement);

        // 为所有目标用户创建阅读记录
        createReadRecordsForAnnouncement(saved);

        return saved;
    }

    /**
     * 为公告创建阅读记录
     */
    private void createReadRecordsForAnnouncement(Announcement announcement) {
        List<User> targetUsers = getTargetUsers(announcement);

        for (User user : targetUsers) {
            // 检查是否已存在记录
            Optional<AnnouncementReadRecord> existing = readRecordRepository
                    .findByAnnouncementIdAndUserId(announcement.getId(), user.getId());

            if (existing.isEmpty()) {
                AnnouncementReadRecord record = new AnnouncementReadRecord();
                record.setAnnouncementId(announcement.getId());
                record.setUserId(user.getId());
                record.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                record.setDepartment(user.getDepartment());
                record.setIsRead(false);
                readRecordRepository.save(record);
            }
        }

        log.info("为公告[{}]创建了{}条阅读记录", announcement.getTitle(), targetUsers.size());
    }

    /**
     * 获取目标用户列表
     */
    private List<User> getTargetUsers(Announcement announcement) {
        String scope = announcement.getScope();
        String targetDepartments = announcement.getTargetDepartments();
        String targetUserIds = announcement.getTargetUsers();

        List<User> users;

        switch (scope) {
            case "all":
                // 全平台用户
                users = userRepository.findAll().stream()
                        .filter(u -> u.getStatus() == 1)
                        .collect(Collectors.toList());
                break;
            case "enterprise":
                // 企业内所有部门用户（根据目标部门选择）
                if (targetDepartments != null && !targetDepartments.isEmpty()) {
                    List<String> deptList = Arrays.asList(targetDepartments.split(","));
                    users = userRepository.findAll().stream()
                            .filter(u -> u.getStatus() == 1 && u.getDepartment() != null)
                            .filter(u -> {
                                for (String dept : deptList) {
                                    if (u.getDepartment().contains(dept.trim())) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .collect(Collectors.toList());
                } else {
                    // 未选择部门则发送给所有企业用户
                    users = userRepository.findAll().stream()
                            .filter(u -> u.getStatus() == 1)
                            .collect(Collectors.toList());
                }
                break;
            case "specific":
                // 指定用户
                if (targetUserIds != null && !targetUserIds.isEmpty()) {
                    List<Long> userIdList = Arrays.stream(targetUserIds.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Long::parseLong)
                            .collect(Collectors.toList());
                    users = userRepository.findAll().stream()
                            .filter(u -> userIdList.contains(u.getId()))
                            .collect(Collectors.toList());
                } else {
                    users = new ArrayList<>();
                }
                break;
            default:
                users = new ArrayList<>();
        }

        return users;
    }

    /**
     * 保存草稿
     */
    @Transactional
    public Announcement saveDraft(Announcement announcement) {
        announcement.setStatus("draft");
        announcement.setUpdateTime(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    /**
     * 更新公告
     */
    @Transactional
    public Announcement updateAnnouncement(Announcement announcement) {
        announcement.setUpdateTime(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    /**
     * 删除公告
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        // 同时删除关联的阅读记录
        List<AnnouncementReadRecord> records = readRecordRepository.findByAnnouncementId(id);
        readRecordRepository.deleteAll(records);
        announcementRepository.deleteById(id);
    }

    /**
     * 获取统计信息
     */
    public Object getStats() {
        LocalDateTime now = LocalDateTime.now();
        return new Object() {
            public final Long urgent = announcementRepository.countByPriority("urgent");
            public final Long important = announcementRepository.countByPriority("important");
            public final Long normal = announcementRepository.countByPriority("normal");
            public final Long total = announcementRepository.countActiveAnnouncements(now);
        };
    }

    /**
     * 标记公告已读
     */
    @Transactional
    public boolean markAsRead(Long announcementId, Long userId) {
        Optional<AnnouncementReadRecord> recordOpt = readRecordRepository
                .findByAnnouncementIdAndUserId(announcementId, userId);

        AnnouncementReadRecord record;
        if (recordOpt.isPresent()) {
            record = recordOpt.get();
        } else {
            // 如果没有阅读记录，先创建一条
            record = new AnnouncementReadRecord();
            record.setAnnouncementId(announcementId);
            record.setUserId(userId);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                record.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                record.setDepartment(user.getDepartment());
            }
            record.setIsRead(false);
            record = readRecordRepository.save(record);
        }

        if (!record.getIsRead()) {
            record.setIsRead(true);
            record.setReadTime(LocalDateTime.now());
            readRecordRepository.save(record);

            // 更新公告阅读计数
            Announcement announcement = announcementRepository.findById(announcementId).orElse(null);
            if (announcement != null) {
                announcement.setReadCount(announcement.getReadCount() == null ? 1 : announcement.getReadCount() + 1);
                announcementRepository.save(announcement);
            }
        }
        return true;
    }

    /**
     * 获取公告的阅读统计（真实数据）
     */
    public Map<String, Object> getReadStats(Long announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId).orElse(null);
        if (announcement == null) {
            return null;
        }

        List<AnnouncementReadRecord> records = readRecordRepository.findByAnnouncementId(announcementId);

        long readCount = records.stream().filter(r -> r.getIsRead()).count();
        long totalCount = records.size();
        long unreadCount = totalCount - readCount;
        double rate = totalCount > 0 ? (double) readCount / totalCount * 100 : 0;

        List<Map<String, Object>> details = records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", r.getUserId());
            map.put("userName", r.getUserName());
            map.put("department", r.getDepartment());
            map.put("readTime", r.getReadTime());
            map.put("status", r.getIsRead() ? "read" : "unread");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("announcementId", announcementId);
        stats.put("title", announcement.getTitle());
        stats.put("total", totalCount);
        stats.put("read", readCount);
        stats.put("unread", unreadCount);
        stats.put("rate", Math.round(rate * 100) / 100.0);
        stats.put("details", details);

        return stats;
    }

    /**
     * 将实体转换为Map
     */
    private Map<String, Object> convertToMap(Announcement a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("title", a.getTitle());
        map.put("content", a.getContent());
        map.put("priority", a.getPriority());
        map.put("status", a.getStatus());
        map.put("publisherId", a.getPublisherId());
        map.put("publisherName", a.getPublisherName());
        map.put("department", a.getDepartment());
        map.put("publishTime", a.getPublishTime());
        map.put("expireTime", a.getExpireTime());
        map.put("readCount", a.getReadCount());
        map.put("scope", a.getScope());
        map.put("targetDepartments", a.getTargetDepartments());
        map.put("targetUsers", a.getTargetUsers());
        map.put("createTime", a.getCreateTime());
        map.put("updateTime", a.getUpdateTime());
        return map;
    }
}
