package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品信息数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，默认提供完整的 CRUD 支持。
 * </p>
 * <p>
 * 当前未定义自定义查询方法，所有商品查询通过 JpaRepository 默认方法完成。
 * 库存操作在 {@link com.example.demo.controller.ProductController} 中通过业务逻辑控制。
 * </p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
