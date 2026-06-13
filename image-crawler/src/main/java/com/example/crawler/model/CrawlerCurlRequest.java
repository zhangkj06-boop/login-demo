package com.example.crawler.model;

/**
 * cURL 原始命令请求 DTO。
 * <p>
 * 接收前端粘贴的 "Copy as cURL (bash)" 原始文本，由后端解析为 {@link CrawlerRequest}。
 * </p>
 */
public class CrawlerCurlRequest {

    /** 原始 cURL 命令文本 */
    private String rawCurl;

    /** 可选：复用 CrawlerRequest 中的图片提取参数 */
    private String selector;
    private String urlPattern;
    private Integer maxCount;
    private String responseType;
    private String jsonPath;
    private String jsonImageUrlField;
    private String nameField1;
    private String nameField2;
    private String downloadPath;

    public String getRawCurl() {
        return rawCurl;
    }

    public void setRawCurl(String rawCurl) {
        this.rawCurl = rawCurl;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getJsonImageUrlField() {
        return jsonImageUrlField;
    }

    public void setJsonImageUrlField(String jsonImageUrlField) {
        this.jsonImageUrlField = jsonImageUrlField;
    }

    public String getNameField1() {
        return nameField1;
    }

    public void setNameField1(String nameField1) {
        this.nameField1 = nameField1;
    }

    public String getNameField2() {
        return nameField2;
    }

    public void setNameField2(String nameField2) {
        this.nameField2 = nameField2;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }
}
