package com.example.demo.controller;

import com.example.demo.entity.CourseVideo;
import com.example.demo.repository.CourseVideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 微课视频控制器。
 * <p>
 * 提供微课视频的完整 REST API，包括查询列表、新增、编辑、删除等功能。
 * </p>
 */
@RestController
@RequestMapping("/api/course-videos")
@CrossOrigin(origins = "*")
public class CourseVideoController {

    private static final Logger logger = LoggerFactory.getLogger(CourseVideoController.class);
    private final CourseVideoRepository courseVideoRepository;

    public CourseVideoController(CourseVideoRepository courseVideoRepository) {
        this.courseVideoRepository = courseVideoRepository;
    }

    /**
     * 查询微课视频列表（支持按标题和分类筛选）。
     */
    @GetMapping
    public ResponseEntity<List<CourseVideo>> listCourseVideos(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        logger.info("查询微课视频列表，筛选条件 - 标题: {}, 分类: {}, 状态: {}", title, category, status);

        List<CourseVideo> list = courseVideoRepository.findAllByOrderByCreateTimeDesc();

        if (title != null && !title.isBlank()) {
            list = list.stream()
                    .filter(v -> v.getTitle() != null && v.getTitle().contains(title))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            list = list.stream()
                    .filter(v -> category.equals(v.getCategory()))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            list = list.stream()
                    .filter(v -> status.equals(v.getStatus()))
                    .collect(Collectors.toList());
        } else {
            // 默认只显示上线的微课
            list = list.stream()
                    .filter(v -> "上线".equals(v.getStatus()))
                    .collect(Collectors.toList());
        }

        logger.info("查询微课视频列表成功，共 {} 条记录", list.size());
        return ResponseEntity.ok(list);
    }

    /**
     * 根据ID查询微课视频详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseVideo(@PathVariable Long id) {
        logger.info("查询微课视频详情，ID: {}", id);
        CourseVideo video = courseVideoRepository.findById(id).orElse(null);
        if (video == null) {
            logger.warn("查询微课视频详情失败，记录不存在，ID: {}", id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "微课视频不存在");
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(video);
    }

    /**
     * 上传视频文件。
     * <p>
     * 根据微课标题在 uploads/videos/ 下创建文件夹，将视频保存到该文件夹中。
     * </p>
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {
        logger.info("上传视频文件，标题: {}", title);
        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择视频文件");
            return ResponseEntity.badRequest().body(result);
        }
        if (title == null || title.isBlank()) {
            result.put("success", false);
            result.put("message", "请输入微课标题");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            // 根据标题创建文件夹（去除非法字符）
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            Path videoDir = Paths.get("uploads/videos", safeTitle);
            File dir = videoDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
                logger.info("创建视频文件夹: {}", dir.getAbsolutePath());
            }

            // 保存文件
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.lastIndexOf('.') > 0) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
            }
            String fileName = System.currentTimeMillis() + ext;
            File destFile = new File(dir, fileName);
            file.transferTo(destFile);
            logger.info("视频文件已保存: {}", destFile.getAbsolutePath());

            // 返回可访问的 URL
            String videoUrl = "/videos/" + safeTitle + "/" + fileName;
            result.put("success", true);
            result.put("message", "上传成功");
            result.put("videoUrl", videoUrl);
            result.put("fileName", fileName);
        } catch (IOException e) {
            logger.error("视频上传失败", e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 新增微课视频。
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCourseVideo(@RequestBody CourseVideo video) {
        logger.info("新增微课视频，标题: {}", video.getTitle());
        Map<String, Object> result = new HashMap<>();

        if (video.getTitle() == null || video.getTitle().isBlank()) {
            result.put("success", false);
            result.put("message", "请输入微课标题");
            return ResponseEntity.badRequest().body(result);
        }
        if (video.getVideoUrl() == null || video.getVideoUrl().isBlank()) {
            result.put("success", false);
            result.put("message", "请输入视频地址");
            return ResponseEntity.badRequest().body(result);
        }

        video.setCreateTime(LocalDateTime.now());
        if (video.getStatus() == null || video.getStatus().isBlank()) {
            video.setStatus("上线");
        }
        if (video.getSourceType() == null || video.getSourceType().isBlank()) {
            video.setSourceType("网络");
        }

        CourseVideo saved = courseVideoRepository.save(video);
        logger.info("新增微课视频成功，ID: {}", saved.getId());
        result.put("success", true);
        result.put("message", "微课视频已保存");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新微课视频。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCourseVideo(@PathVariable Long id, @RequestBody CourseVideo video) {
        logger.info("更新微课视频，ID: {}", id);
        Map<String, Object> result = new HashMap<>();

        CourseVideo existing = courseVideoRepository.findById(id).orElse(null);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "微课视频不存在");
            return ResponseEntity.status(404).body(result);
        }

        if (video.getTitle() != null && !video.getTitle().isBlank()) {
            existing.setTitle(video.getTitle());
        }
        if (video.getVideoUrl() != null && !video.getVideoUrl().isBlank()) {
            existing.setVideoUrl(video.getVideoUrl());
        }
        if (video.getSourceType() != null && !video.getSourceType().isBlank()) {
            existing.setSourceType(video.getSourceType());
        }
        if (video.getCoverUrl() != null) {
            existing.setCoverUrl(video.getCoverUrl());
        }
        if (video.getCategory() != null) {
            existing.setCategory(video.getCategory());
        }
        if (video.getDescription() != null) {
            existing.setDescription(video.getDescription());
        }
        if (video.getTeacher() != null) {
            existing.setTeacher(video.getTeacher());
        }
        if (video.getStatus() != null && !video.getStatus().isBlank()) {
            existing.setStatus(video.getStatus());
        }

        courseVideoRepository.save(existing);
        logger.info("更新微课视频成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "微课视频已更新");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除微课视频。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCourseVideo(@PathVariable Long id) {
        logger.info("删除微课视频，ID: {}", id);
        Map<String, Object> result = new HashMap<>();

        CourseVideo existing = courseVideoRepository.findById(id).orElse(null);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "微课视频不存在");
            return ResponseEntity.status(404).body(result);
        }

        courseVideoRepository.deleteById(id);
        logger.info("删除微课视频成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "微课视频已删除");
        return ResponseEntity.ok(result);
    }
}
