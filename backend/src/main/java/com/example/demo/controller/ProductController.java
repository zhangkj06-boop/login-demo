package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.entity.StockRecord;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.StockRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品（仓库）控制器。
 * <p>
 * 提供商品管理和库存操作的完整 REST API，包括商品的增删改查、入库、出库/领取及库存记录查询。
 * </p>
 * <p>
 * 库存操作设计：
 * <ul>
 *   <li>入库（stockIn）：增加商品 quantity，并在 stock_records 插入 type=in 的记录</li>
 *   <li>出库/领取（stockOut）：减少商品 quantity，并在 stock_records 插入 type=out 的记录；
 *       若库存不足则返回 400 错误，不会扣减库存</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    /** 商品数据访问接口 */
    private final ProductRepository productRepository;
    /** 库存记录数据访问接口 */
    private final StockRecordRepository stockRecordRepository;

    public ProductController(ProductRepository productRepository, StockRecordRepository stockRecordRepository) {
        this.productRepository = productRepository;
        this.stockRecordRepository = stockRecordRepository;
    }

    /**
     * 查询所有商品列表。
     *
     * @return 商品列表（包含当前库存 quantity）
     */
    @GetMapping
    public ResponseEntity<List<Product>> listProducts() {
        logger.info("查询所有商品列表");
        List<Product> products = productRepository.findAll();
        logger.info("查询商品列表成功，共 {} 条记录", products.size());
        return ResponseEntity.ok(products);
    }

    /**
     * 根据ID查询商品详情。
     *
     * @param id 商品主键
     * @return 商品详情；不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        logger.info("查询商品详情，ID: {}", id);
        return productRepository.findById(id)
                .map(product -> {
                    logger.info("查询商品详情成功，ID: {}", id);
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    logger.warn("查询商品详情失败，商品不存在，ID: {}", id);
                    return ResponseEntity.status(404).body((Product) null);
                });
    }

    /**
     * 创建新商品。
     *
     * @param product 商品实体（JSON 请求体）
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Product product) {
        logger.info("创建商品，名称: {}", product.getName());
        Map<String, Object> result = new HashMap<>();
        if (product.getName() == null || product.getName().isBlank()) {
            logger.warn("创建商品失败，商品名称不能为空");
            result.put("success", false);
            result.put("message", "商品名称不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("创建商品失败，价格不能为负数");
            result.put("success", false);
            result.put("message", "价格不能为负数");
            return ResponseEntity.badRequest().body(result);
        }
        if (product.getQuantity() == null || product.getQuantity() < 0) {
            logger.warn("创建商品失败，数量不能为负数");
            result.put("success", false);
            result.put("message", "数量不能为负数");
            return ResponseEntity.badRequest().body(result);
        }
        if (product.getType() == null || product.getType().isBlank()) {
            logger.warn("创建商品失败，类型不能为空");
            result.put("success", false);
            result.put("message", "类型不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        Product saved = productRepository.save(product);
        logger.info("创建商品成功，ID: {}", saved.getId());
        result.put("success", true);
        result.put("message", "创建成功");
        result.put("id", saved.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 更新商品信息（部分更新）。
     * <p>
     * 仅传入非 null 且合法的字段会被更新。
     * </p>
     *
     * @param id      商品主键
     * @param product 更新的内容
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        logger.info("更新商品，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("更新商品失败，商品不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "商品不存在");
            return ResponseEntity.status(404).body(result);
        }
        if (product.getName() != null && !product.getName().isBlank()) {
            existing.setName(product.getName());
        }
        if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
            existing.setPrice(product.getPrice());
        }
        if (product.getQuantity() != null && product.getQuantity() >= 0) {
            existing.setQuantity(product.getQuantity());
        }
        if (product.getType() != null && !product.getType().isBlank()) {
            existing.setType(product.getType());
        }
        productRepository.save(existing);
        logger.info("更新商品成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "更新成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除商品。
     *
     * @param id 商品主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        logger.info("删除商品，ID: {}", id);
        Map<String, Object> result = new HashMap<>();
        if (!productRepository.existsById(id)) {
            logger.warn("删除商品失败，商品不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "商品不存在");
            return ResponseEntity.status(404).body(result);
        }
        productRepository.deleteById(id);
        logger.info("删除商品成功，ID: {}", id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 商品入库。
     * <p>
     * 增加商品库存，并在库存记录表插入入库记录。
     * </p>
     *
     * @param id   商品主键
     * @param body 请求体，包含 amount（入库数量）和 operator（操作人，可选）
     * @return 入库结果，包含当前库存 quantity
     */
    @PostMapping("/{id}/stock/in")
    public ResponseEntity<Map<String, Object>> stockIn(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        logger.info("商品入库，ID: {}, 数量: {}", id, body.get("amount"));
        Map<String, Object> result = new HashMap<>();
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("商品入库失败，商品不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "商品不存在");
            return ResponseEntity.status(404).body(result);
        }
        Integer amount = parseInt(body.get("amount"));
        if (amount == null || amount <= 0) {
            logger.warn("商品入库失败，入库数量必须大于0");
            result.put("success", false);
            result.put("message", "入库数量必须大于0");
            return ResponseEntity.badRequest().body(result);
        }
        existing.setQuantity(existing.getQuantity() + amount);
        productRepository.save(existing);

        // 记录库存变动
        StockRecord record = new StockRecord();
        record.setProductId(id);
        record.setProductName(existing.getName());
        record.setAmount(amount);
        record.setOperator(body.get("operator") != null ? body.get("operator").toString() : null);
        record.setType("in");
        stockRecordRepository.save(record);

        logger.info("商品入库成功，ID: {}, 当前库存: {}", id, existing.getQuantity());
        result.put("success", true);
        result.put("message", "入库成功");
        result.put("quantity", existing.getQuantity());
        return ResponseEntity.ok(result);
    }

    /**
     * 商品出库/领取。
     * <p>
     * 减少商品库存，并在库存记录表插入出库记录。
     * 若库存不足则返回 400 错误，不会扣减库存（保证原子性逻辑）。
     * </p>
     *
     * @param id   商品主键
     * @param body 请求体，包含 amount（领取数量）和 operator（领取人）
     * @return 出库结果，包含当前库存 quantity
     */
    @PostMapping("/{id}/stock/out")
    public ResponseEntity<Map<String, Object>> stockOut(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        logger.info("商品出库，ID: {}, 数量: {}", id, body.get("amount"));
        Map<String, Object> result = new HashMap<>();
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            logger.warn("商品出库失败，商品不存在，ID: {}", id);
            result.put("success", false);
            result.put("message", "商品不存在");
            return ResponseEntity.status(404).body(result);
        }
        Integer amount = parseInt(body.get("amount"));
        if (amount == null || amount <= 0) {
            logger.warn("商品出库失败，领取数量必须大于0");
            result.put("success", false);
            result.put("message", "领取数量必须大于0");
            return ResponseEntity.badRequest().body(result);
        }
        if (existing.getQuantity() < amount) {
            logger.warn("商品出库失败，库存不足，当前库存: {}", existing.getQuantity());
            result.put("success", false);
            result.put("message", "库存不足，当前库存: " + existing.getQuantity());
            return ResponseEntity.badRequest().body(result);
        }
        existing.setQuantity(existing.getQuantity() - amount);
        productRepository.save(existing);

        // 记录库存变动
        StockRecord record = new StockRecord();
        record.setProductId(id);
        record.setProductName(existing.getName());
        record.setAmount(amount);
        record.setOperator(body.get("operator") != null ? body.get("operator").toString() : null);
        record.setType("out");
        stockRecordRepository.save(record);

        logger.info("商品出库成功，ID: {}, 当前库存: {}", id, existing.getQuantity());
        result.put("success", true);
        result.put("message", "领取成功");
        result.put("quantity", existing.getQuantity());
        return ResponseEntity.ok(result);
    }

    /**
     * 查询所有库存记录。
     *
     * @return 库存记录列表（按创建时间降序）
     */
    @GetMapping("/records")
    public ResponseEntity<List<StockRecord>> listRecords() {
        logger.info("查询所有库存记录");
        List<StockRecord> records = stockRecordRepository.findAllByOrderByCreatedAtDesc();
        logger.info("查询库存记录成功，共 {} 条记录", records.size());
        return ResponseEntity.ok(records);
    }

    /**
     * 导出库存记录为 Excel。
     * <p>
     * 下载内容包含：商品名称、操作类型（入库/出库）、变动数量、操作人员、操作时间。
     * </p>
     *
     * @return Excel 文件字节流
     */
    @GetMapping("/records/export")
    public ResponseEntity<byte[]> exportRecords() {
        logger.info("导出库存记录 Excel");
        try {
            List<StockRecord> records = stockRecordRepository.findAllByOrderByCreatedAtDesc();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("库存记录");

            // 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = { "商品名称", "操作类型", "变动数量", "操作人员", "操作时间" };
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据行
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowNum = 1;
            for (StockRecord record : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getProductName());
                row.createCell(1).setCellValue("in".equals(record.getType()) ? "入库" : "出库");
                row.createCell(2).setCellValue(record.getAmount());
                row.createCell(3).setCellValue(record.getOperator() != null ? record.getOperator() : "-");
                row.createCell(4).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "-");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            byte[] bytes = out.toByteArray();
            logger.info("导出库存记录成功，共 {} 条", records.size());

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=stock_records.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        } catch (Exception e) {
            logger.error("导出库存记录失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 通用整数解析工具方法。
     * <p>
     * 支持 Integer 类型直接返回，或从 String 解析。
     * 解析失败返回 null，由调用方做校验。
     * </p>
     *
     * @param value 待解析的值
     * @return Integer 或 null
     */
    private Integer parseInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("数字转换失败: {}", value);
            return null;
        }
    }
}
