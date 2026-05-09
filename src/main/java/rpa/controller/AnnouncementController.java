package rpa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rpa.entity.Announcement;
import rpa.service.AnnouncementService;

import java.util.*;

@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 获取公告列表（包含用户的真实阅读状态）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAnnouncementList(@RequestParam(required = false) Long userId) {
        List<Map<String, Object>> announcements = announcementService.getPublishedAnnouncements(userId);
        return ResponseEntity.ok(createSuccessResponse(announcements));
    }

    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAnnouncementDetail(@PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncementById(id);
        if (announcement != null) {
            return ResponseEntity.ok(createSuccessResponse(announcement));
        }
        return ResponseEntity.ok(createErrorResponse("公告不存在"));
    }

    /**
     * 发布公告
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishAnnouncement(@RequestBody Announcement announcement) {
        try {
            Announcement saved = announcementService.publishAnnouncement(announcement);
            return ResponseEntity.ok(createSuccessResponseWithMsg("发布成功", saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(createErrorResponse("发布失败: " + e.getMessage()));
        }
    }

    /**
     * 保存草稿
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody Announcement announcement) {
        try {
            Announcement saved = announcementService.saveDraft(announcement);
            return ResponseEntity.ok(createSuccessResponseWithMsg("草稿保存成功", saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(createErrorResponse("保存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取草稿列表
     */
    @GetMapping("/drafts")
    public ResponseEntity<Map<String, Object>> getDrafts() {
        try {
            List<Announcement> drafts = announcementService.getDrafts();
            return ResponseEntity.ok(createSuccessResponse(drafts));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(createErrorResponse("获取草稿失败: " + e.getMessage()));
        }
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        Announcement updated = announcementService.updateAnnouncement(announcement);
        return ResponseEntity.ok(createSuccessResponseWithMsg("更新成功", updated));
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(createSuccessResponseWithMsg("删除成功", null));
    }

    /**
     * 标记已读
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        boolean success = announcementService.markAsRead(id, userId);
        if (success) {
            return ResponseEntity.ok(createSuccessResponseWithMsg("已标记为已读", null));
        }
        return ResponseEntity.ok(createErrorResponse("标记失败"));
    }

    /**
     * 批量标记已读
     */
    @PutMapping("/readAll")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null) {
            return ResponseEntity.ok(createErrorResponse("用户ID不能为空"));
        }
        // 获取所有公告并标记已读
        List<Map<String, Object>> announcements = announcementService.getPublishedAnnouncements(userId);
        for (Map<String, Object> a : announcements) {
            if ("unread".equals(a.get("status"))) {
                announcementService.markAsRead((Long) a.get("id"), userId);
            }
        }
        return ResponseEntity.ok(createSuccessResponseWithMsg("全部已标记为已读", null));
    }

    /**
     * 获取统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(createSuccessResponse(announcementService.getStats()));
    }

    /**
     * 获取阅读统计（真实数据）
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getReadStats(@PathVariable Long id) {
        Map<String, Object> stats = announcementService.getReadStats(id);
        if (stats != null) {
            return ResponseEntity.ok(createSuccessResponse(stats));
        }
        return ResponseEntity.ok(createErrorResponse("公告不存在"));
    }

    /**
     * 发送提醒
     */
    @PostMapping("/sendReminder")
    public ResponseEntity<Map<String, Object>> sendReminder(@RequestBody Map<String, Object> params) {
        // TODO: 实现邮件/站内信提醒功能
        return ResponseEntity.ok(createSuccessResponseWithMsg("提醒已发送", null));
    }

    /**
     * 批量发送提醒
     */
    @PostMapping("/sendBatchReminder")
    public ResponseEntity<Map<String, Object>> sendBatchReminder(@RequestBody Map<String, Object> params) {
        // TODO: 实现批量邮件/站内信提醒功能
        return ResponseEntity.ok(createSuccessResponseWithMsg("已向所有未读人员发送提醒", null));
    }

    // ============ 辅助方法 ============

    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> createSuccessResponseWithMsg(String message, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        map.put("message", message);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 1);
        map.put("message", message);
        return map;
    }
}
