package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 投诉实体类。
 * <p>
 * 对应数据库表 {@code complaints}，用于存储学生对教师的投诉反馈信息。
 * </p>
 * <p>
 * 业务流转：提交投诉（resolved=false） → 管理员回复 → 管理员标记解决（resolved=true）。
 * </p>
 */
@Entity
@Table(name = "complaints")
public class Complaint {

    /** 投诉主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 提交人姓名（投诉学生） */
    @Column(name = "submitter_name", nullable = false)
    private String submitterName;

    /** 被投诉教师姓名 */
    @Column(name = "teacher_name", nullable = false)
    private String teacherName;

    /** 投诉描述/意见内容，最大长度 2000 字符 */
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    /** 投诉创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 管理员回复内容 */
    @Column(name = "reply", length = 2000)
    private String reply;

    /** 是否已解决，默认 false */
    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    public Complaint() {
    }

    public Complaint(String submitterName, String teacherName, String description, LocalDateTime createdAt) {
        this.submitterName = submitterName;
        this.teacherName = teacherName;
        this.description = description;
        this.createdAt = createdAt;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }
}
