package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片爬虫响应结果模型类（DTO）。
 * <p>
 * 封装爬虫任务的执行结果，包括下载成功/失败数量、保存路径、图片列表等。
 * </p>
 */
public class CrawlerResult {

    /** 是否执行成功（指任务完成，不代表所有图片都下载成功） */
    private boolean success;

    /** 提示信息 */
    private String message;

    /** 成功下载的图片数量 */
    private int downloadedCount;

    /** 下载失败的图片数量 */
    private int failedCount;

    /** 文件保存的相对路径（如 "/uploads/crawler/20250609_143022/"） */
    private String savePath;

    /** 下载成功的图片 URL 列表 */
    private List<String> images = new ArrayList<>();

    /** 下载失败的图片 URL 列表 */
    private List<String> failedImages = new ArrayList<>();

    /** 接口返回的原始响应内容（HTML 或 JSON 文本，用于调试） */
    private String responseBody;

    public CrawlerResult() {
    }

    public CrawlerResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // ==================== Getter / Setter ====================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getDownloadedCount() {
        return downloadedCount;
    }

    public void setDownloadedCount(int downloadedCount) {
        this.downloadedCount = downloadedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getFailedImages() {
        return failedImages;
    }

    public void setFailedImages(List<String> failedImages) {
        this.failedImages = failedImages;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * 添加一条成功下载记录。
     */
    public void addImage(String imageUrl) {
        this.images.add(imageUrl);
        this.downloadedCount = this.images.size();
    }

    /**
     * 添加一条失败下载记录。
     */
    public void addFailedImage(String imageUrl) {
        this.failedImages.add(imageUrl);
        this.failedCount = this.failedImages.size();
    }
}
