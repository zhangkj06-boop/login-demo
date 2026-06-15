package com.example.autoframework.core;

import com.example.autoframework.pojo.TestCase;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Excel 测试数据读取器 —— 负责将 Excel 中的测试用例解析为 {@link TestCase} 对象列表。
 *
 * <p>框架采用 "Excel 数据驱动" 设计，测试工程师只需在 Excel 中维护用例，
 * 无需修改 Java 代码即可新增、修改、删除测试场景。本类是连接 Excel 与 Java 世界的桥梁。</p>
 *
 * <p>Excel 列分为两类：</p>
 * <ul>
 *   <li><b>固定列</b>（框架保留）：caseId, caseName, apiName, method, url, templateFile,
 *       headers, encrypt, decrypt, extract, expectedCode, expectedField, expectedValue</li>
 *   <li><b>动态列</b>（用户自定义）：除固定列外的任意列，列名自动成为模板变量名，
 *       单元格值成为变量值。例如列名 "username"，在模板中通过 ${username} 引用</li>
 * </ul>
 *
 * <p><b>大小写处理策略</b>：固定列匹配时忽略大小写（便于不同人的习惯），
 * 但动态变量列保留原始大小写（因为 JSON 模板中的 ${loginType} 是大小写敏感的）。
 * 这是通过一个内部小写列表做固定列判断，同时保留原始列表做变量名映射实现的。</p>
 *
 * @see TestCase
 * @see JsonTemplate#render(String, Map)
 */
public class ExcelReader {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelReader.class);

    /**
     * 框架保留的固定列名（统一小写，用于忽略大小写的匹配）。
     * 若 Excel 中出现同名列（不区分大小写），则不会被纳入 variables Map。
     */
    private static final Set<String> FIXED_COLUMNS = Set.of(
            "caseid", "casename", "apiname", "method", "url",
            "templatefile", "headers", "encrypt", "decrypt", "extract",
            "expectedcode", "expectedfield", "expectedvalue"
    );

    /**
     * 读取 Excel 文件中的所有 Sheet，合并返回全部用例。
     *
     * <p>适用于将所有模块的用例放在同一个 Excel 文件的不同 Sheet 中，
     * 运行时可以一次性加载全部用例并顺序执行。
     * 空 Sheet 或空行会被自动跳过，不影响其他有效数据。</p>
     *
     * @param excelPath Excel 文件的绝对路径或相对路径
     * @return 解析后的 TestCase 列表，按 Sheet 顺序、行顺序排列
     * @throws IOException 文件不存在、格式错误或 POI 解析异常时抛出
     */
    public static List<TestCase> readAllCases(String excelPath) throws IOException {
        LOG.info("[ExcelReader] 开始读取 Excel 文件: {}", excelPath);
        List<TestCase> allCases = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new File(excelPath))) {
            int sheetCount = wb.getNumberOfSheets();
            LOG.info("[ExcelReader] 发现 {} 个 Sheet", sheetCount);
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = wb.getSheetAt(i);
                LOG.info("[ExcelReader] 读取 Sheet[{}]: '{}'", i, sheet.getSheetName());
                List<TestCase> sheetCases = readSheet(sheet);
                LOG.info("[ExcelReader] Sheet[{}] 解析完成，有效用例数: {}", i, sheetCases.size());
                allCases.addAll(sheetCases);
            }
        }
        LOG.info("[ExcelReader] Excel 读取完成，总用例数: {}", allCases.size());
        return allCases;
    }

    /**
     * 读取 Excel 中指定名称的单个 Sheet。
     *
     * <p>适用于按模块拆分测试类时使用，例如：</p>
     * <pre>
     *   // 认证模块只加载 "认证" Sheet
     *   ExcelReader.readSheet(path, "认证模块");
     * </pre>
     *
     * @param excelPath  Excel 文件路径
     * @param sheetName  Sheet 名称（大小写敏感）
     * @return 该 Sheet 中的 TestCase 列表
     * @throws IOException              文件读取异常
     * @throws IllegalArgumentException Sheet 不存在时抛出
     */
    public static List<TestCase> readSheet(String excelPath, String sheetName) throws IOException {
        LOG.info("[ExcelReader] 开始读取指定 Sheet: '{}', 文件: {}", sheetName, excelPath);
        try (Workbook wb = WorkbookFactory.create(new File(excelPath))) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                LOG.error("[ExcelReader] Sheet 不存在: '{}'", sheetName);
                throw new IllegalArgumentException("Sheet 不存在: " + sheetName
                        + "，请检查 Excel 中 Sheet 名称是否与代码中一致");
            }
            List<TestCase> cases = readSheet(sheet);
            LOG.info("[ExcelReader] Sheet '{}' 解析完成，有效用例数: {}", sheetName, cases.size());
            return cases;
        }
    }

    /**
     * 解析单个 Sheet 的核心逻辑。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>读取第 0 行作为表头，同时保留原始大小写和全小写两份列表</li>
     *   <li>从第 1 行开始逐行解析，若 caseId 为空则跳过（允许存在视觉分隔空行）</li>
     *   <li>固定列直接映射到 TestCase 的对应字段</li>
     *   <li>动态列按 "原始列名 -> 单元格值" 存入 variables Map</li>
     * </ol>
     *
     * @param sheet Apache POI 的 Sheet 对象
     * @return 该 Sheet 解析出的 TestCase 列表
     */
    private static List<TestCase> readSheet(Sheet sheet) {
        List<TestCase> cases = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            LOG.warn("[ExcelReader] Sheet 表头为空，跳过解析");
            return cases;
        }

        // 解析表头：保留原始大小写用于变量名，同时存小写版本用于固定列匹配
        List<String> originalHeaders = new ArrayList<>();
        List<String> lowerHeaders = new ArrayList<>();
        for (Cell cell : headerRow) {
            String raw = getCellValue(cell).trim();
            originalHeaders.add(raw);
            lowerHeaders.add(raw.toLowerCase());
        }
        LOG.debug("[ExcelReader] 表头列数: {}, 固定列: {}, 变量列: {}",
                originalHeaders.size(),
                lowerHeaders.stream().filter(FIXED_COLUMNS::contains).count(),
                lowerHeaders.stream().filter(h -> !FIXED_COLUMNS.contains(h)).count());

        int skipCount = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                skipCount++;
                continue;
            }

            // 如果 caseId 为空，跳过空行（Excel 中可能存在视觉分隔行）
            String caseId = getCellValueByHeader(row, lowerHeaders, "caseid");
            if (caseId == null || caseId.isEmpty()) {
                skipCount++;
                continue;
            }

            // 收集动态变量列：只要不是框架固定列，就视为模板变量
            Map<String, String> variables = new LinkedHashMap<>();
            for (int j = 0; j < lowerHeaders.size(); j++) {
                String colLower = lowerHeaders.get(j);
                if (!FIXED_COLUMNS.contains(colLower)) {
                    String val = getCellValue(row.getCell(j));
                    if (!val.isEmpty()) {
                        // 变量名保留原始大小写，与模板 ${loginType} 等保持一致
                        variables.put(originalHeaders.get(j), val);
                    }
                }
            }

            // 使用 Builder 模式构建 TestCase 对象，避免冗长的构造函数参数
            TestCase testCase = TestCase.builder()
                    .caseId(caseId)
                    .caseName(getCellValueByHeader(row, lowerHeaders, "casename"))
                    .apiName(getCellValueByHeader(row, lowerHeaders, "apiname"))
                    .method(getCellValueByHeader(row, lowerHeaders, "method"))
                    .url(getCellValueByHeader(row, lowerHeaders, "url"))
                    .templateFile(getCellValueByHeader(row, lowerHeaders, "templatefile"))
                    .headers(getCellValueByHeader(row, lowerHeaders, "headers"))
                    .encrypt(getCellValueByHeader(row, lowerHeaders, "encrypt"))
                    .decrypt(getCellValueByHeader(row, lowerHeaders, "decrypt"))
                    .extract(getCellValueByHeader(row, lowerHeaders, "extract"))
                    .expectedCode(getCellValueByHeader(row, lowerHeaders, "expectedcode"))
                    .expectedField(getCellValueByHeader(row, lowerHeaders, "expectedfield"))
                    .expectedValue(getCellValueByHeader(row, lowerHeaders, "expectedvalue"))
                    .variables(variables)
                    .build();

            LOG.debug("[ExcelReader] 解析用例: caseId={}, method={}, url={}, 变量数={}",
                    caseId, testCase.getMethod(), testCase.getUrl(), variables.size());
            cases.add(testCase);
        }

        if (skipCount > 0) {
            LOG.debug("[ExcelReader] 跳过空行/无效行: {} 行", skipCount);
        }
        return cases;
    }

    /**
     * 根据列名从指定行中读取单元格值。
     *
     * <p>内部实现：在 headers 列表中查找目标列名的索引，然后取 row 中对应索引的 Cell。
     * 搜索时统一转小写，实现忽略大小写的列名匹配。
     * 若列不存在则返回空字符串，避免空指针导致整批用例解析失败。</p>
     *
     * @param row          当前数据行
     * @param headers      表头列表（小写）
     * @param targetHeader 要查找的目标列名
     * @return 单元格字符串值；列不存在时返回空字符串
     */
    private static String getCellValueByHeader(Row row, List<String> headers, String targetHeader) {
        int idx = headers.indexOf(targetHeader.toLowerCase());
        if (idx < 0) return "";
        return getCellValue(row.getCell(idx));
    }

    /**
     * 读取单元格内容并统一转为字符串。
     *
     * <p>支持的数据类型处理：</p>
     * <ul>
     *   <li>STRING：直接取字符串并 trim</li>
     *   <li>NUMERIC：判断是否为日期格式，日期转字符串；普通数字去掉小数点后多余的 .0</li>
     *   <li>BOOLEAN：转为 "true" / "false"</li>
     *   <li>FORMULA：取公式表达式本身（如 SUM(A1:A2)）</li>
     *   <li>BLANK / ERROR：返回空字符串</li>
     * </ul>
     *
     * @param cell POI Cell 对象，可能为 null
     * @return 单元格字符串表示，不会返回 null
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double d = cell.getNumericCellValue();
                // 如果数值是整数（如 200），避免输出 "200.0"
                if (d == (long) d) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
