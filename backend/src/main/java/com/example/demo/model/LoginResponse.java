package com.example.demo.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录响应模型类（DTO）。
 * <p>
 * 用于封装登录接口返回给前端的统一 JSON 数据结构，
 * 包含操作结果标识、提示消息、认证令牌及用户基本信息。
 * </p>
 * <p>
 * 提供多种静态工厂方法（success/error 重载），简化 Controller 层的响应构造。
 * </p>
 */
public class LoginResponse {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(LoginResponse.class);

    /** 操作是否成功 */
    private boolean success;

    /** 提示消息（如 "登录成功"、"密码错误"） */
    private String message;

    /** 登录令牌（UUID 格式），前端后续请求需携带此 Token */
    private String token;

    /** 用户名 */
    private String username;

    /** 用户头像 URL */
    private String avatar;

    /** 用户角色：ADMIN / STUDENT / TEACHER */
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String message, String token, String username) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.username = username;
    }

    public LoginResponse(boolean success, String message, String token, String username, String avatar) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.username = username;
        this.avatar = avatar;
    }

    /**
     * 创建成功响应（基础版：含用户名）。
     *
     * @param message  成功提示
     * @param token    认证令牌
     * @param username 用户名
     * @return LoginResponse 实例
     */
    public static LoginResponse success(String message, String token, String username) {
        return new LoginResponse(true, message, token, username);
    }

    /**
     * 创建成功响应（含头像）。
     *
     * @param message  成功提示
     * @param token    认证令牌
     * @param username 用户名
     * @param avatar   头像 URL
     * @return LoginResponse 实例
     */
    public static LoginResponse success(String message, String token, String username, String avatar) {
        return new LoginResponse(true, message, token, username, avatar);
    }

    /**
     * 创建成功响应（完整版：含头像和角色）。
     *
     * @param message  成功提示
     * @param token    认证令牌
     * @param username 用户名
     * @param avatar   头像 URL
     * @param role     用户角色
     * @return LoginResponse 实例
     */
    public static LoginResponse success(String message, String token, String username, String avatar, String role) {
        LoginResponse res = new LoginResponse(true, message, token, username, avatar);
        res.setRole(role);
        return res;
    }

    /**
     * 创建失败响应。
     *
     * @param message 错误提示
     * @return LoginResponse 实例（success = false）
     */
    public static LoginResponse error(String message) {
        return new LoginResponse(false, message);
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
