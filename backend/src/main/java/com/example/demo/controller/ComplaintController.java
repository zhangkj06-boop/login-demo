package com.example.demo.controller;

import com.example.demo.entity.Complaint;
import com.example.demo.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 投诉控制器。
 * <p>
 * 提供投诉反馈的完整 REST API，包括投诉查询、提交、回复和解决状态标记。
 * </p>
 * <p>
 * 业务流程：学生提交投诉 → 管理员查看并回复 → 管理员标记解决。
 * 投诉记录通常不删除，保留用于审计追溯。
 * </p>
 */
@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(ComplaintController.class);

    /** 投诉数据访问接口 */
    private final ComplaintRepository complaintRepository;

    public ComplaintController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    /**
     * 查询所有投诉列表。
     *
     * @return 全部投诉记录
     */
    @GetMapping
    public ResponseEntity<List<Complaint>> listComplaints() {
        logger.info("查询所有投诉列表");
        List<Complaint> complaints = complaintRepository.findAll();
        logger.info("查询投诉列表成功，共 {} 条记录", complaints.size());
        return ResponseEntity.ok(complaints);
    }

    /**
     * 根据提交人查询投诉列表（"我的投诉"）。
     *
     * @param submitterName 提交人姓名
     * @return 该提交人的投诉记录列表（按创建时间降序）
     */
    @GetMapping("/my")
    public ResponseEntity<List<Complaint>> listMyComplaints(@RequestParam String submitterName) {
        logger.info("查询我的投诉列表，提交人: {}", submitterName);
        List<Complaint> complaints = complaintRepository.findBySubmitterNameOrderByCreatedAtDesc(submitterName);
        logger.info("查询我的投诉列表成功，共 {} 条记录", complaints.size());
        return ResponseEntity.ok(complaints);
    }

    /**
     * 提交新的投诉。
     *
     * @param complaint 投诉实体（包含 teacherName、description、submitterName）
     * @return 提交结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createComplaint(@RequestBody Complaint complaint) {
        logger.info("提交投诉，老师: {}, 提交人: {}", complaint.getTeacherName(), complaint.getSubmitterName());
        Map<String, Object> result = new HashMap<>();
        if (complaint.getTeacherName() == null || complaint.getTeacherName().isBlank()) {
            logger.warn("提交投诉失败，未选择投诉老师");
            result.put("success", false);
            result.put("message", "请选择投诉老师");
            return ResponseEntity.badRequest().body(result);
        }
        if (complaint.getDescription() == null || complaint.getDescription().isBlank()) {
            logger.warn("提交投诉失败，未填写意见描述");
            result.put("success", false);
            result.put("message", "请填写意见描述");
            return ResponseEntity.badRequest().body(result);
        }
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setResolved(false);
        Complaint saved = complaintRepository.save(complaint);
        logger.info("投诉提交成功");
        result.put("success", true);
        result.put("message", "投诉提交成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 回复投诉。
     *
     * @param id   投诉主键
     * @param body 请求体，包含 reply 字段
     * @return 回复结果
     */
    @PutMapping("/{id}/reply")
    public ResponseEntity<Map<String, Object>> replyComplaint(@PathVariable Long id, @RequestBody Map<String, String> body) {
        logger.info("回复投诉，投诉ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint == null) {
            logger.warn("回复投诉失败，投诉记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "投诉记录不存在");
            return ResponseEntity.status(404).body(result);
        }
        String reply = body.get("reply");
        if (reply == null || reply.isBlank()) {
            logger.warn("回复投诉失败，回复内容不能为空");
            result.put("success", false);
            result.put("message", "回复内容不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        complaint.setReply(reply);
        complaintRepository.save(complaint);
        logger.info("回复投诉成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "回复成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 标记投诉解决状态。
     *
     * @param id   投诉主键
     * @param body 请求体，包含 resolved 布尔字段
     * @return 操作结果
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveComplaint(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        logger.info("标记投诉解决状态，投诉ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint == null) {
            logger.warn("标记投诉状态失败，投诉记录不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "投诉记录不存在");
            return ResponseEntity.status(404).body(result);
        }
        Boolean resolved = body.get("resolved");
        if (resolved == null) {
            logger.warn("标记投诉状态失败，参数错误");
            result.put("success", false);
            result.put("message", "参数错误");
            return ResponseEntity.badRequest().body(result);
        }
        complaint.setResolved(resolved);
        complaintRepository.save(complaint);
        logger.info("标记投诉状态成功，ID: {}, 状态: {}", id, resolved ? "已解决" : "未解决");
        result.put("success", true);
        result.put("message", resolved ? "已标记为已解决" : "已标记为未解决");
        return ResponseEntity.ok(result);
    }
}
