package com.example.autoframework.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 示例 Excel 生成器 —— 用于快速生成可供框架直接执行的测试数据。
 *
 * <p>本类的存在意义是降低框架上手门槛：新用户无需手动创建 Excel 文件，
 * 直接运行 {@code main} 方法即可在 {@code src/test/resources/cases/} 目录下
 * 生成一份符合框架规范的 sample_cases.xlsx。</p>
 *
 * <p>生成的用例数据与 {@code login-demo backend} 的真实接口对齐，包括：</p>
 * <ul>
 *   <li>POST /api/login — 管理员登录（成功 / 密码错误）</li>
 *   <li>GET /api/users — 查询用户列表</li>
 *   <li>GET /api/products — 查询商品列表</li>
 * </ul>
 *
 * <p><b>如何运行</b>：</p>
 * <pre>
 *   cd api-auto-framework
 *   mvn test-compile exec:java -Dexec.classpathScope=test \
 *       -Dexec.mainClass="com.example.autoframework.utils.DemoExcelGenerator"
 * </pre>
 *
 * @see com.example.autoframework.core.ExcelReader
 */
public class DemoExcelGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DemoExcelGenerator.class);

    /**
     * 程序入口：生成示例 Excel 并写入 {@code src/test/resources/cases/sample_cases.xlsx}。
     *
     * @param args 命令行参数（未使用）
     * @throws IOException 文件写入失败时抛出
     */
    public static void main(String[] args) throws IOException {
        LOG.info("[DemoExcelGenerator] 开始生成示例 Excel...");
        try (Workbook wb = new XSSFWorkbook()) {
            // 创建一个名为 "用户模块" 的 Sheet
            Sheet sheet = wb.createSheet("用户模块");
            LOG.info("[DemoExcelGenerator] 创建 Sheet: '用户模块'");

            // ========== 定义表头 ==========
            // 前 13 列为框架固定列，后续列为用户自定义变量列
            String[] headers = {
                    "caseId", "caseName", "apiName", "method", "url",
                    "templateFile", "headers", "encrypt", "decrypt", "extract",
                    "expectedCode", "expectedField", "expectedValue",
                    // 以下为自定义变量列，列名将作为模板变量名（${username} 等）
                    "username", "password", "loginType"
            };

            // ========== 表头样式 ==========
            // 设置灰色背景 + 加粗字体，使表头在 Excel 中更醒目
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            LOG.info("[DemoExcelGenerator] 表头写入完成，共 {} 列", headers.length);

            // ========== 示例数据 1：管理员正常登录 ==========
            // 预期：返回 200，响应体中 role=ADMIN，并提取 token 到上下文
            Row row1 = sheet.createRow(1);
            setRowValues(row1, new String[]{
                    "LOGIN_001", "管理员正常登录", "认证模块", "POST", "/api/login",
                    "login.json", "", "0", "0", "token=token",
                    "200", "role", "ADMIN",
                    "admin", "123456", "user"
            });
            LOG.info("[DemoExcelGenerator] 写入示例 1: LOGIN_001 管理员正常登录");

            // ========== 示例数据 2：密码错误 ==========
            // 预期：返回 401，message 字段提示密码错误
            Row row2 = sheet.createRow(2);
            setRowValues(row2, new String[]{
                    "LOGIN_002", "密码错误", "认证模块", "POST", "/api/login",
                    "login.json", "", "0", "0", "",
                    "401", "message", "密码错误",
                    "admin", "wrongPwd", "user"
            });
            LOG.info("[DemoExcelGenerator] 写入示例 2: LOGIN_002 密码错误");

            // ========== 示例数据 3：查询用户列表（GET 无模板，验证框架 GET 支持） ==========
            Row row3 = sheet.createRow(3);
            setRowValues(row3, new String[]{
                    "USER_001", "查询用户列表", "用户模块", "GET", "/api/users",
                    "empty.json", "", "0", "0", "",
                    "200", "[0].username", "admin",
                    "", "", ""
            });
            LOG.info("[DemoExcelGenerator] 写入示例 3: USER_001 查询用户列表");

            // ========== 示例数据 4：查询商品列表（验证 GET 无模板请求） ==========
            Row row4 = sheet.createRow(4);
            setRowValues(row4, new String[]{
                    "PROD_001", "查询商品列表", "商品模块", "GET", "/api/products",
                    "empty.json", "", "0", "0", "",
                    "200", "[0].name", "2B铅笔",
                    "", "", ""
            });
            LOG.info("[DemoExcelGenerator] 写入示例 4: PROD_001 查询商品列表");

            // ========== 自动调整列宽 ==========
            // 让每列宽度自适应内容，避免打开 Excel 后文字被截断
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // ========== 写入文件 ==========
            String outPath = "src/test/resources/cases/sample_cases.xlsx";
            try (FileOutputStream fos = new FileOutputStream(outPath)) {
                wb.write(fos);
            }
            LOG.info("[DemoExcelGenerator] 示例 Excel 已生成: {}", outPath);
        }
    }

    /**
     * 辅助方法：将字符串数组批量写入 Excel 行的连续单元格。
     *
     * @param row    目标行
     * @param values 要写入的字符串数组，长度应与表头一致
     */
    private static void setRowValues(Row row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
