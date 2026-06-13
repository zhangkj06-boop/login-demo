package com.example.demo.controller;

import com.example.demo.entity.Announcement;
import com.example.demo.repository.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公告控制器。
 * <p>
 * 提供公告管理的完整 REST API，包括查询、新增、编辑、删除、发布和停发等功能。
 * </p>
 * <p>
 * 业务规则：
 * <ul>
 *   <li>公告创建后默认为"草稿"状态，只有发布后才对目标用户可见</li>
 *   <li>非管理员角色只能看到推送对象匹配的公告（学生看"学生"/"家长"/"全部"，教师看"教师"/"全部"）</li>
 *   <li>支持多条件组合筛选（标题、推送对象、发布人、状态、范围、时间区间）</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);

    /** 公告数据访问接口 */
    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /**
     * 查询所有公告列表（支持多条件筛选）。
     * <p>
     * 筛选逻辑：先全量查询再内存过滤（适合数据量不大的场景）。
     * 权限过滤：非 ADMIN 角色只能看到与自己角色匹配的公告。
     * </p>
     *
     * @param title     标题模糊筛选（可选）
     * @param target    推送对象筛选（可选）
     * @param publisher 发布人模糊筛选（可选）
     * @param status    状态筛选（可选）
     * @param scope     推送范围筛选（可选）
     * @param dateFrom  开始日期（可选，格式 yyyy-MM-dd）
     * @param dateTo    结束日期（可选，格式 yyyy-MM-dd）
     * @param role      当前用户角色（用于权限过滤）
     * @return 筛选后的公告列表
     */
    @GetMapping
    public ResponseEntity<List<Announcement>> listAnnouncements(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String role) {

        logger.info("查询公告列表，筛选条件 - 标题: {}, 推送对象: {}, 发布人: {}, 状态: {}, 角色: {}",
                title, target, publisher, status, role);

        // 先按创建时间降序全量查询（数据量不大时内存过滤即可）
        List<Announcement> list = announcementRepository.findAllByOrderByCreateTimeDesc();

        // 组合条件内存过滤
        if (title != null && !title.isBlank()) {
            list = list.stream()
                    .filter(a -> a.getTitle() != null && a.getTitle().contains(title))
                    .collect(Collectors.toList());
        }
        if (target != null && !target.isBlank()) {
            list = list.stream()
                    .filter(a -> a.getTarget() != null && a.getTarget().contains(target))
                    .collect(Collectors.toList());
        }
        if (publisher != null && !publisher.isBlank()) {
            list = list.stream()
                    .filter(a -> a.getPublisher() != null && a.getPublisher().contains(publisher))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            list = list.stream()
                    .filter(a -> status.equals(a.getStatus()))
                    .collect(Collectors.toList());
        }
        if (scope != null && !scope.isBlank()) {
            list = list.stream()
                    .filter(a -> scope.equals(a.getScope()))
                    .collect(Collectors.toList());
        }
        // 日期范围过滤：优先使用 publishTime，若为空则使用 createTime
        if (dateFrom != null && !dateFrom.isBlank()) {
            LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
            list = list.stream()
                    .filter(a -> {
                        LocalDateTime dt = a.getPublishTime() != null ? a.getPublishTime() : a.getCreateTime();
                        return dt != null && !dt.isBefore(from);
                    })
                    .collect(Collectors.toList());
        }
        if (dateTo != null && !dateTo.isBlank()) {
            LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
            list = list.stream()
                    .filter(a -> {
                        LocalDateTime dt = a.getPublishTime() != null ? a.getPublishTime() : a.getCreateTime();
                        return dt != null && !dt.isAfter(to);
                    })
                    .collect(Collectors.toList());
        }

        // 权限控制：非管理员只能看到推送对象匹配的公告
        if (role != null && !role.isBlank() && !"ADMIN".equals(role)) {
            final String userRole = role;
            list = list.stream().filter(a -> {
                String t = a.getTarget();
                if (t == null) return false;
                if (t.contains("全部")) return true;
                if ("STUDENT".equals(userRole) && (t.contains("学生") || t.contains("家长"))) return true;
                if ("TEACHER".equals(userRole) && t.contains("教师")) return true;
                return false;
            }).collect(Collectors.toList());
        }

        logger.info("查询公告列表成功，共 {} 条记录", list.size());
        return ResponseEntity.ok(list);
    }

    /**
     * 根据ID查询公告详情。
     *
     * @param id 公告主键
     * @return 公告详情；若不存在返回 404 及错误信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnnouncement(@PathVariable Long id) {
        logger.info("查询公告详情，ID: {}", id);
        Announcement announcement = announcementRepository.findById(id).orElse(null);
        if (announcement == null) {
            logger.warn("查询公告详情失败，记录不存在，ID: {}", id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "公告不存在");
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(announcement);
    }

    /**
     * 新增公告。
     * <p>
     * 创建时自动设置 createTime 为当前时间；
     * 若状态为"已发布"，则同时设置 publishTime。
     * </p>
     *
     * @param announcement 公告实体（JSON 请求体）
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAnnouncement(@RequestBody Announcement announcement) {
        logger.info("新增公告，标题: {}", announcement.getTitle());
        Map<String, Object> result = new HashMap<>();

        // 必填字段校验
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            result.put("success", false);
            result.put("message", "请输入公告标题");
            return ResponseEntity.badRequest().body(result);
        }
        if (announcement.getTarget() == null || announcement.getTarget().isBlank()) {
            result.put("success", false);
            result.put("message", "请选择推送对象");
            return ResponseEntity.badRequest().body(result);
        }
        if (announcement.getScope() == null || announcement.getScope().isBlank()) {
            result.put("success", false);
            result.put("message", "请选择推送范围");
            return ResponseEntity.badRequest().body(result);
        }
        if (announcement.getContent() == null || announcement.getContent().isBlank()) {
            result.put("success", false);
            result.put("message", "请输入公告内容");
            return ResponseEntity.badRequest().body(result);
        }

        announcement.setCreateTime(LocalDateTime.now());
        if ("已发布".equals(announcement.getStatus())) {
            announcement.setPublishTime(LocalDateTime.now());
        }

        Announcement saved = announcementRepository.save(announcement);
        logger.info("新增公告成功，ID: {}", saved.getId());
        result.put("success", true);
        result.put("message", "公告已保存");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新公告。
     * <p>
     * 若原状态为草稿且更新后要求发布，则自动设置 publishTime。
     * </p>
     *
     * @param id           公告主键
     * @param announcement 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        logger.info("更新公告，ID: {}", id);
        Map<String, Object> result = new HashMap<>();

        Announcement existing = announcementRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新公告失败，记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "公告不存在");
            return ResponseEntity.status(404).body(result);
        }

        // 必填字段校验
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            result.put("success", false);
            result.put("message", "请输入公告标题");
            return ResponseEntity.badRequest().body(result);
        }
        if (announcement.getTarget() == null || announcement.getTarget().isBlank()) {
            result.put("success", false);
            result.put("message", "请选择推送对象");
            return ResponseEntity.badRequest().body(result);
        }
        if (announcement.getScope() == null || announcement.getScope().isBlank()) {
            result.put("success", false);
            result.put("message", "请选择推送范围");
            return ResponseEntity.badRequest().body(result);
        }
        if (announcement.getContent() == null || announcement.getContent().isBlank()) {
            result.put("success", false);
            result.put("message", "请输入公告内容");
            return ResponseEntity.badRequest().body(result);
        }

        existing.setTitle(announcement.getTitle());
        existing.setTarget(announcement.getTarget());
        existing.setScope(announcement.getScope());
        existing.setContent(announcement.getContent());
        existing.setPublisher(announcement.getPublisher());

        // 如果是草稿状态且请求中要求发布，则设置发布时间
        if ("已发布".equals(announcement.getStatus()) && !"已发布".equals(existing.getStatus())) {
            existing.setStatus("已发布");
            existing.setPublishTime(LocalDateTime.now());
        }

        announcementRepository.save(existing);
        logger.info("更新公告成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "公告已更新");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除公告。
     *
     * @param id 公告主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Long id) {
        logger.info("删除公告，ID: {}", id);
        Map<String, Object> result = new HashMap<>();

        Announcement existing = announcementRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("删除公告失败，记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "公告不存在");
            return ResponseEntity.status(404).body(result);
        }

        announcementRepository.deleteById(id);
        logger.info("删除公告成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "公告已删除");
        return ResponseEntity.ok(result);
    }

    /**
     * 发布公告（草稿 → 已发布）。
     *
     * @param id 公告主键
     * @return 发布结果
     */
    @PutMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishAnnouncement(@PathVariable Long id) {
        logger.info("发布公告，ID: {}", id);
        Map<String, Object> result = new HashMap<>();

        Announcement existing = announcementRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("发布公告失败，记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "公告不存在");
            return ResponseEntity.status(404).body(result);
        }

        existing.setStatus("已发布");
        existing.setPublishTime(LocalDateTime.now());
        announcementRepository.save(existing);
        logger.info("发布公告成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "公告已发布");
        return ResponseEntity.ok(result);
    }

    /**
     * 停发公告（已发布 → 草稿）。
     *
     * @param id 公告主键
     * @return 停发结果
     */
    @PutMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopAnnouncement(@PathVariable Long id) {
        logger.info("停发公告，ID: {}", id);
        Map<String, Object> result = new HashMap<>();

        Announcement existing = announcementRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("停发公告失败，记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "公告不存在");
            return ResponseEntity.status(404).body(result);
        }

        existing.setStatus("草稿");
        existing.setPublishTime(null); // 清空发布时间
        announcementRepository.save(existing);
        logger.info("停发公告成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "公告已停发");
        return ResponseEntity.ok(result);
    }
}
