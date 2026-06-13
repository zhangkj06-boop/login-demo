package com.example.crawler.model;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 图片爬虫请求模型类（DTO）。
 * <p>
 * 用于接收前端提交的爬虫任务参数，包括目标页面 URL、认证信息、图片筛选规则等。
 * 支持 GET / POST 两种请求方式访问目标网站，可自定义 Headers 和 JSON Body。
 * </p>
 */
public class CrawlerRequest {

    /** 目标页面 URL（必填） */
    @NotBlank(message = "目标 URL 不能为空")
    private String targetUrl;

    /** 目标网站 Cookie 字符串（可选，如 "sessionId=xxx; token=yyy"） */
    private String cookies;

    /** Authorization 请求头（可选，如 Bearer token 或 Basic base64） */
    private String authorization;

    /** Basic Auth 用户名（可选，与 cookies/authorization 三选一） */
    private String username;

    /** Basic Auth 密码（可选） */
    private String password;

    /** CSS 选择器，用于定位包含图片的元素，默认为 "img" */
    private String selector = "img";

    /**
     * 图片 URL 正则过滤规则。
     * <p>
     * 默认匹配常见图片后缀：jpg、jpeg、png、gif、webp、bmp。
     * </p>
     */
    private String urlPattern = ".*\\.(jpg|jpeg|png|gif|webp|bmp).*";

    /** 最大下载数量限制，默认 100 */
    private Integer maxCount = 100;

    /** 请求方式：GET 或 POST，默认 GET */
    private String method = "GET";

    /** 自定义请求头（可选），如 {"X-Custom-Header": "value"} */
    private Map<String, String> headers;

    /** POST 请求体（JSON 格式，可选） */
    private String body;

    /** Content-Type，默认 application/json */
    private String contentType = "application/json";

    /** 自定义 User-Agent（可选，覆盖默认值） */
    private String userAgent;

    /** 客户端标识（可选），非空时会作为请求头传递给目标网站 */
    private String clientId;

    /** ClientId 对应的 Header 名，默认 X-Client-Id */
    private String clientIdHeader = "X-Client-Id";

    /** 响应类型：html 或 json，默认 html */
    private String responseType = "html";

    /** JSON 路径，如 answerSheet，用于定位包含图片 URL 的数组字段 */
    private String jsonPath;

    /** JSON 数组元素中图片 URL 的字段名，默认 url */
    private String jsonImageUrlField = "url";

    /** 自定义图片名称字段 1（可选，从 JSON 中提取作为文件名前缀） */
    private String nameField1;

    /** 自定义图片名称字段 2（可选，从 JSON 中提取作为文件名前缀，与 nameField1 拼接） */
    private String nameField2;

    /** 自定义图片下载路径（可选，默认使用 uploads/crawler/时间戳/） */
    private String downloadPath;

    // ==================== Getter / Setter ====================

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientIdHeader() {
        return clientIdHeader;
    }

    public void setClientIdHeader(String clientIdHeader) {
        this.clientIdHeader = clientIdHeader;
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
