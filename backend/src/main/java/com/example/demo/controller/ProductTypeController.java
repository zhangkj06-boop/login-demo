package com.example.demo.controller;

import com.example.demo.entity.ProductType;
import com.example.demo.repository.ProductTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品类型控制器。
 * <p>
 * 提供商品类型的增删改查 REST API。
 * </p>
 * <p>
 * 业务约束：类型名称全局唯一，创建和更新时需做重复校验。
 * </p>
 */
@RestController
@RequestMapping("/api/product-types")
@CrossOrigin(origins = "*")
public class ProductTypeController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(ProductTypeController.class);

    /** 商品类型数据访问接口 */
    private final ProductTypeRepository productTypeRepository;

    public ProductTypeController(ProductTypeRepository productTypeRepository) {
        this.productTypeRepository = productTypeRepository;
    }

    /**
     * 查询所有商品类型列表。
     *
     * @return 类型列表
     */
    @GetMapping
    public ResponseEntity<List<ProductType>> listTypes() {
        logger.info("查询所有商品类型列表");
        List<ProductType> types = productTypeRepository.findAll();
        logger.info("查询商品类型列表成功，共 {} 条记录", types.size());
        return ResponseEntity.ok(types);
    }

    /**
     * 创建新商品类型。
     *
     * @param type 类型实体（包含 name）
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createType(@RequestBody ProductType type) {
        logger.info("创建商品类型，名称: {}", type.getName());
        Map<String, Object> result = new HashMap<>();
        if (type.getName() == null || type.getName().isBlank()) {
            logger.warn("创建商品类型失败，类型名称不能为空");
            result.put("success", false);
            result.put("message", "类型名称不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (productTypeRepository.existsByName(type.getName())) {
            logger.warn("创建商品类型失败，类型名称已存在: {}", type.getName());
            result.put("success", false);
            result.put("message", "类型名称已存在");
            return ResponseEntity.badRequest().body(result);
        }
        ProductType saved = productTypeRepository.save(type);
        logger.info("创建商品类型成功，ID: {}", saved.getId());
        result.put("success", true);
        result.put("message", "创建成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新商品类型。
     *
     * @param id   类型主键
     * @param type 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateType(@PathVariable Long id, @RequestBody ProductType type) {
        logger.info("更新商品类型，ID: {}, 名称: {}", id, type.getName());
        Map<String, Object> result = new HashMap<>();
        ProductType existing = productTypeRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新商品类型失败，类型不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "类型不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (type.getName() == null || type.getName().isBlank()) {
            logger.warn("更新商品类型失败，类型名称不能为空");
            result.put("success", false);
            result.put("message", "类型名称不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        // 若名称变更，需再次校验唯一性
        if (!type.getName().equals(existing.getName()) && productTypeRepository.existsByName(type.getName())) {
            logger.warn("更新商品类型失败，类型名称已存在: {}", type.getName());
            result.put("success", false);
            result.put("message", "类型名称已存在");
            return ResponseEntity.badRequest().body(result);
        }
        existing.setName(type.getName());
        productTypeRepository.save(existing);
        logger.info("更新商品类型成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除商品类型。
     *
     * @param id 类型主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteType(@PathVariable Long id) {
        logger.info("删除商品类型，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        if (!productTypeRepository.existsById(id)) {
            logger.warn("删除商品类型失败，类型不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "类型不存在");
            return ResponseEntity.status(404).body(result);
        }
        productTypeRepository.deleteById(id);
        logger.info("删除商品类型成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
