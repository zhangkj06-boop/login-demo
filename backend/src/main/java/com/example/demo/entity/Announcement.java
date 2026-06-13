package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 公告实体类。
 * <p>
 * 对应数据库表 {@code announcements}，用于存储系统公告/通知信息。
 * </p>
 * <p>
 * 业务规则：
 * <ul>
 *   <li>创建时默认为 "草稿" 状态，发布后才对学生/教师可见</li>
 *   <li>发布时会自动设置 publishTime；停发时清空 publishTime</li>
 *   <li>支持按推送对象（学生/教师/家长/全部）和推送范围（全局/指定群体）进行定向发送</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "announcements")
public class Announcement {

    /** 公告主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 公告标题 */
    @Column(name = "title", nullable = false)
    private String title;

    /** 推送对象：学生/教师/家长/全部 */
    @Column(name = "target", nullable = false)
    private String target;

    /** 推送范围：全局推送/指定群体 */
    @Column(name = "scope", nullable = false)
    private String scope;

    /** 公告正文内容，最大长度 4000 字符 */
    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    /** 发布人姓名 */
    @Column(name = "publisher", nullable = false)
    private String publisher;

    /** 发布状态：草稿/已发布，默认为草稿 */
    @Column(name = "status", nullable = false)
    private String status = "草稿";

    /** 创建时间（草稿保存时设置） */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /** 发布时间（正式对外推送时设置） */
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    public Announcement() {
    }

    public Announcement(String title, String target, String scope, String content, String publisher, LocalDateTime createTime) {
        this.title = title;
        this.target = target;
        this.scope = scope;
        this.content = content;
        this.publisher = publisher;
        this.createTime = createTime;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
}
