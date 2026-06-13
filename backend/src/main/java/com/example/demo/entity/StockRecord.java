package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 库存记录实体类。
 * <p>
 * 对应数据库表 {@code stock_records}，用于记录商品的每一次库存变动（入库/出库）。
 * </p>
 * <p>
 * 与 {@link Product} 的关系：通过 productId 进行逻辑关联（非外键约束），
 * 即使商品被删除，历史库存记录仍可保留用于审计追溯。
 * </p>
 * <p>
 * 自动时间戳：通过 {@link PrePersist} 注解在持久化前自动设置 createdAt 字段。
 * </p>
 */
@Entity
@Table(name = "stock_records")
public class StockRecord {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(StockRecord.class);

    /** 记录主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的商品ID（逻辑外键） */
    @Column(nullable = false)
    private Long productId;

    /** 商品名称（快照，记录变动时的名称，避免商品改名后历史记录失真） */
    @Column(nullable = false)
    private String productName;

    /** 变动数量（正整数） */
    @Column(nullable = false)
    private Integer amount;

    /** 操作人姓名（如"张老师"），可为空 */
    @Column
    private String operator;

    /** 记录类型：in（入库）/ out（出库/领取） */
    @Column(nullable = false)
    private String type;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 持久化前回调方法。
     * <p>
     * 在 JPA 执行 INSERT 前自动调用，为 createdAt 赋予当前系统时间，
     * 避免业务代码手动设置，减少遗漏。
     * </p>
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
