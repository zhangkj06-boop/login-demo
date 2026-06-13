package com.example.demo.repository;

import com.example.demo.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 商品类型数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供商品类型的基础 CRUD 支持。
 * </p>
 */
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    /**
     * 根据类型名称判断是否存在。
     * <p>
     * 用于创建/更新类型时的唯一性校验，
     * 避免数据库抛出唯一约束冲突异常。
     * </p>
     *
     * @param name 类型名称
     * @return true 表示已存在，false 表示不存在
     */
    boolean existsByName(String name);
}
