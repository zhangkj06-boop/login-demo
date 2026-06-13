package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * 游戏实体类。
 * <p>
 * 对应数据库表 {@code games}，存储系统中可玩的游戏信息。
 * </p>
 */
@Entity
@Table(name = "games")
public class Game {

    /** 游戏主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 游戏名称（如：坦克大战、弹珠游戏） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 游戏编码（英文标识，如：tank-battle、pinball） */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** 游戏描述 */
    @Column(length = 500)
    private String description;

    /** 游戏图标URL */
    @Column(length = 255)
    private String icon;

    /** 游戏状态（上线/下线） */
    @Column(nullable = false, length = 20)
    private String status = "上线";

    /** 排序序号（越小越靠前） */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Game() {
    }

    public Game(String name, String code, String description, String icon, Integer sortOrder) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
