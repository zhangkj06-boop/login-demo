package com.example.demo.entity;

import jakarta.persistence.*;

/**
 * 商品类型实体类。
 * <p>
 * 对应数据库表 {@code product_types}，用于对商品进行分类管理。
 * </p>
 * <p>
 * 约束：类型名称全局唯一（{@code unique = true}），防止重复创建。
 * </p>
 */
@Entity
@Table(name = "product_types")
public class ProductType {

    /** 类型主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 类型名称（唯一，如"办公用品"、"教学设备"） */
    @Column(unique = true, nullable = false)
    private String name;

    public ProductType() {
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
}
