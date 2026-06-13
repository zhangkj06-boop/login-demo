package com.example.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 商品实体类。
 * <p>
 * 对应数据库表 {@code products}，用于存储仓库商品信息。
 * </p>
 * <p>
 * 字段说明：
 * <ul>
 *   <li>price 使用 {@link BigDecimal} 类型，避免浮点数精度问题（如 0.1 + 0.2 ≠ 0.3）</li>
 *   <li>quantity 表示当前库存数量，通过入库/领取接口进行增减</li>
 *   <li>type 为商品类型名称（关联 ProductType），非外键约束</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "products")
public class Product {

    /** 商品主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商品名称 */
    @Column(nullable = false)
    private String name;

    /** 商品单价，precision=10, scale=2 表示最多 10 位数字，其中小数点后 2 位 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 当前库存数量 */
    @Column(nullable = false)
    private Integer quantity;

    /** 商品类型名称（如"办公用品"、"教学设备"） */
    @Column(nullable = false)
    private String type;

    public Product() {
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
