package com.example.demo.repository;

import com.example.demo.entity.StockRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 库存记录数据访问接口。
 * <p>
 * 继承 {@link JpaRepository}，提供库存记录的增删改查及排序查询能力。
 * </p>
 */
public interface StockRecordRepository extends JpaRepository<StockRecord, Long> {

    /**
     * 根据商品ID查询库存记录，按创建时间降序排列。
     * <p>
     * 用于查看某个商品的历史出入库明细。
     * </p>
     *
     * @param productId 商品ID
     * @return 该商品的库存变动记录列表（最新的在前）
     */
    List<StockRecord> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * 查询所有库存记录，按创建时间降序排列。
     * <p>
     * 用于仓库管理后台的总览页面。
     * </p>
     *
     * @return 全部库存记录列表（最新的在前）
     */
    List<StockRecord> findAllByOrderByCreatedAtDesc();
}
