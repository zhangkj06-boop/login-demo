package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求模型类（DTO）。
 * <p>
 * 用于接收前端登录请求提交的 JSON 参数，
 * 字段上标注 Jakarta Validation 注解，由 Spring Boot 自动进行参数校验。
 * </p>
 * <p>
 * 校验规则：
 * <ul>
 *   <li>username：非空（@NotBlank）</li>
 *   <li>password：非空且长度不少于 6 位（@NotBlank + @Size）</li>
 *   <li>loginType：可选，默认为 "user"（管理员登录）</li>
 * </ul>
 * </p>
 */
public class LoginRequest {

    /** 用户名/学号/教师ID（根据 loginType 含义不同） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码（明文传输，由后端 BCrypt 匹配） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    /** 登录类型：user（管理员）/ student（学生）/ teacher（教师），默认为 user */
    private String loginType = "user";

    // ==================== Getter / Setter ====================

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

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }
}
