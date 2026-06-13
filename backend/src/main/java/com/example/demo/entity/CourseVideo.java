package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 微课视频实体类。
 * <p>
 * 对应数据库表 {@code course_videos}，用于存储微课学习视频信息。
 * </p>
 */
@Entity
@Table(name = "course_videos")
public class CourseVideo {

    /** 微课主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 微课标题 */
    @Column(name = "title", nullable = false)
    private String title;

    /** 视频地址（网页URL或本地路径） */
    @Column(name = "video_url", nullable = false, length = 2000)
    private String videoUrl;

    /** 视频来源：网络/本地 */
    @Column(name = "source_type", nullable = false)
    private String sourceType = "网络";

    /** 封面图地址（可选） */
    @Column(name = "cover_url", length = 2000)
    private String coverUrl;

    /** 课程分类/标签 */
    @Column(name = "category")
    private String category;

    /** 课程简介 */
    @Column(name = "description", length = 2000)
    private String description;

    /** 讲师 */
    @Column(name = "teacher")
    private String teacher;

    /** 状态：上线/下线 */
    @Column(name = "status", nullable = false)
    private String status = "上线";

    /** 创建时间 */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    public CourseVideo() {
    }

    public CourseVideo(String title, String videoUrl, String sourceType, String coverUrl, String category, String description, String teacher, LocalDateTime createTime) {
        this.title = title;
        this.videoUrl = videoUrl;
        this.sourceType = sourceType;
        this.coverUrl = coverUrl;
        this.category = category;
        this.description = description;
        this.teacher = teacher;
        this.createTime = createTime;
    }

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

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
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
}
