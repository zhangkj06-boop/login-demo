package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * 用户实体类（管理员账号）。
 * <p>
 * 对应数据库表 {@code users}，存储系统管理员的基本信息及登录凭证。
 * </p>
 * <p>
 * 安全设计：
 * <ul>
 *   <li>密码字段使用 {@code @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)}，
 *       确保密码在序列化响应时不会被暴露给前端</li>
 *   <li>用户名全局唯一，作为登录标识</li>
 *   <li>role 默认为 "ADMIN"，当前系统用户即管理员</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "users")
public class User {

    /** 用户主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（全局唯一，登录标识） */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 登录密码（BCrypt 哈希值）。
     * <p>
     * WRITE_ONLY：JSON 序列化时忽略此字段，防止密码泄露；
     * 反序列化（接收前端请求）时仍允许写入。
     * </p>
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    /** 用户头像 URL */
    @Column
    private String avatar;

    /** 用户角色，默认为 ADMIN */
    @Column(nullable = false)
    private String role = "ADMIN";

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ==================== Getter / Setter ====================

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
