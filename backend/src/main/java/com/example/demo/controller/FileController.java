package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器。
 * <p>
 * 提供文件上传相关的 REST API，当前主要支持头像上传。
 * </p>
 * <p>
 * 文件存储：保存到本地文件系统 {@code uploads/avatars/} 目录，
 * 并通过 {@link com.example.demo.config.WebConfig} 中的静态资源映射对外提供访问。
 * </p>
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    /** 头像文件上传目录（相对项目根目录） */
    private static final String UPLOAD_DIR = "uploads/avatars/";

    /**
     * 上传头像。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>校验文件非空</li>
     *   <li>提取原始扩展名（无扩展名则默认 .png）</li>
     *   <li>生成 UUID 作为文件名（避免冲突和暴露原始文件名）</li>
     *   <li>创建目录（如不存在）并保存文件</li>
     *   <li>返回相对路径 URL，前端可直接用于 img src</li>
     * </ol>
     * </p>
     *
     * @param file 上传的文件（MultipartFile）
     * @return 包含访问 URL 的成功响应，或错误信息
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        logger.info("上传头像，文件名: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            logger.warn("上传头像失败，文件为空");
            result.put("success", false);
            result.put("message", "文件为空");
            return ResponseEntity.badRequest().body(result);
        }

        // 提取原始扩展名
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".png";
        // 使用 UUID 生成唯一文件名，避免覆盖和暴露原始文件名
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        try {
            Path dirPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath); // 级联创建不存在的目录
            }
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            // 返回相对路径URL，自动匹配当前域名和端口
            String url = "/uploads/avatars/" + fileName;
            logger.info("上传头像成功，URL: {}", url);
            result.put("success", true);
            result.put("url", url);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("上传头像失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
