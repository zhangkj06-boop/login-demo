package com.example.autoframework.tests;

import com.example.autoframework.core.ApiContext;
import com.example.autoframework.core.ApiEngine;
import com.example.autoframework.core.ExcelReader;
import com.example.autoframework.pojo.ApiResponse;
import com.example.autoframework.pojo.TestCase;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

/**
 * 按模块拆分的测试类示例 —— 展示如何将统一入口拆分为多个 Class。
 *
 * <p>{@link BaseApiTest} 采用 "一个类跑全部用例" 的模式，适合快速上手和中小型项目。
 * 当项目规模扩大、接口模块增多时，按业务模块拆分测试类有以下优势：</p>
 * <ul>
 *   <li><b>报告更清晰</b>：Allure 中每个 Class 对应一个独立的 Feature 分组，便于按模块查看通过率</li>
 *   <li><b>团队协同</b>：不同测试工程师负责不同模块，避免修改同一个 Java 文件产生冲突</li>
 *   <li><b>依赖控制</b>：利用 TestNG 的 {@code dependsOnGroups} 实现模块级依赖，
 *       如 "订单模块" 必须等 "认证模块" 全部通过后才执行</li>
 *   <li><b>失败隔离</b>：某个模块的用例文件损坏不会影响其他模块的执行</li>
 * </ul>
 *
 * <p><b>与 BaseApiTest 的核心差异</b>：</p>
 * <pre>
 *   BaseApiTest    : 读取 Excel 所有 Sheet，一个 @Test 方法跑完全部
 *   ModuleApiTest  : 每个模块对应一个 @DataProvider + @Test，只读取指定 Sheet
 * </pre>
 *
 * @see BaseApiTest
 */
@Listeners(com.example.autoframework.listeners.AllureTestListener.class)
public class ModuleApiTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleApiTest.class);

    /** Excel 用例文件路径，固定读取 sample_cases.xlsx */
    private static final String CASE_EXCEL = "cases/sample_cases.xlsx";

    /**
     * 测试类初始化：清理全局上下文。
     *
     * <p>每个 @Test Class 执行前都应清理上下文，因为 TestNG 可能按类顺序执行，
     * 前一个类的 token 不应带到后一个类。</p>
     */
    @BeforeClass
    public void setup() {
        ApiContext.clear();
        LOG.info("[ModuleApiTest] 测试类初始化完成");
    }

    // ==================== 认证模块 ====================

    /**
     * 认证模块的数据驱动器。
     *
     * <p>只加载 Excel 中名为 "用户模块" 的 Sheet，返回该 Sheet 下的所有用例。
     * 若 Sheet 不存在会抛出 IllegalArgumentException，提醒检查 Excel 文件。</p>
     *
     * @return Object[][] 数据数组
     * @throws IOException Excel 读取异常
     */
    @DataProvider(name = "authCases")
    public Object[][] authCases() throws IOException {
        LOG.info("[ModuleApiTest] 加载认证模块用例...");
        return loadSheet("用户模块");
    }

    /**
     * 认证模块的测试入口。
     *
     * <p>标记 {@code groups = "auth"} 后，其他模块可以通过 {@code dependsOnGroups = "auth"}
     * 声明对认证模块的依赖。如果认证模块中任何一条用例失败，依赖它的模块会被 TestNG 跳过。</p>
     *
     * @param testCase 单条测试用例
     * @throws Exception 执行过程中的各类异常
     */
    @Test(dataProvider = "authCases", groups = "auth")
    @Epic("接口自动化")
    @Feature("认证模块")
    public void authTests(TestCase testCase) throws Exception {
        LOG.info("[ModuleApiTest] 执行认证模块用例: {}", testCase.getCaseId());
        executeAndAssert(testCase);
    }

    // ==================== 订单模块（示例占位） ====================

    /**
     * 订单模块的数据驱动器（示例）。
     *
     * <p>当前 Excel 中没有 "订单模块" Sheet，若直接运行此方法会报错。
     * 仅作为演示代码保留，实际使用时需要：</p>
     * <ol>
     *   <li>在 Excel 中新建 "订单模块" Sheet</li>
     *   <li>或删除此方法及其对应的 @Test</li>
     * </ol>
     *
     * @return Object[][] 数据数组
     * @throws IOException Excel 读取异常
     */
    @DataProvider(name = "orderCases")
    public Object[][] orderCases() throws IOException {
        LOG.info("[ModuleApiTest] 加载订单模块用例...");
        return loadSheet("订单模块");
    }

    /**
     * 订单模块的测试入口（示例）。
     *
     * <p>{@code dependsOnGroups = "auth"} 表示：只有当 "auth" 分组下的所有用例
     * 全部通过后，订单模块才会执行。如果登录接口全部失败了，创建订单接口自然也无意义，
     * 这样可以节省无意义的执行时间和错误日志。</p>
     *
     * @param testCase 单条测试用例
     * @throws Exception 执行过程中的各类异常
     */
    @Test(dataProvider = "orderCases", groups = "order", dependsOnGroups = "auth")
    @Epic("接口自动化")
    @Feature("订单模块")
    public void orderTests(TestCase testCase) throws Exception {
        LOG.info("[ModuleApiTest] 执行订单模块用例: {}", testCase.getCaseId());
        executeAndAssert(testCase);
    }

    // ==================== 通用执行与断言逻辑 ====================

    /**
     * 抽取的公共执行与断言方法。
     *
     * <p>与 {@link BaseApiTest#runApiTest} 类似，但省略了 Allure 附件附加逻辑，
     * 因为 {@link com.example.autoframework.listeners.AllureTestListener} 已经通过监听机制
     * 全局附加上下文快照。如需在此类中也附加请求/响应详情，可自行补充 Allure.addAttachment。</p>
     *
     * @param testCase 单条测试用例
     * @throws Exception 请求执行或断言失败时抛出
     */
    private void executeAndAssert(TestCase testCase) throws Exception {
        // Allure 的故事层级：比 Feature 更细，通常对应单条用例
        Allure.story(testCase.getCaseName());
        // Issue 编号：方便从报告跳转到用例管理工具（如 Jira）
        Allure.issue(testCase.getCaseId(), testCase.getCaseId());

        // 调用引擎完成请求全生命周期
        ApiResponse response = ApiEngine.execute(testCase);

        // ========== 状态码断言 ==========
        if (testCase.getExpectedCode() != null && !testCase.getExpectedCode().isEmpty()) {
            int expected = Integer.parseInt(testCase.getExpectedCode());
            LOG.info("[ModuleApiTest] 断言状态码: expected={}, actual={}", expected, response.getStatusCode());
            Assertions.assertThat(response.getStatusCode())
                    .as("[%s] HTTP 状态码校验", testCase.getCaseId())
                    .isEqualTo(expected);
            LOG.info("[ModuleApiTest] 状态码断言通过");
        }

        // ========== JSON Path 字段断言 ==========
        if (testCase.getExpectedField() != null && !testCase.getExpectedField().isEmpty()) {
            Object actual = JsonPath.from(response.getBody()).get(testCase.getExpectedField());
            LOG.info("[ModuleApiTest] 断言字段: path='{}', expected='{}', actual='{}'",
                    testCase.getExpectedField(), testCase.getExpectedValue(), actual);
            Assertions.assertThat(String.valueOf(actual))
                    .as("[%s] 字段 [%s] 校验", testCase.getCaseId(), testCase.getExpectedField())
                    .isEqualTo(testCase.getExpectedValue());
            LOG.info("[ModuleApiTest] 字段断言通过");
        }
    }

    /**
     * 加载指定 Sheet 的数据并转换为 TestNG 数据驱动所需的 Object[][] 格式。
     *
     * <p>内部调用 {@link ExcelReader#readSheet}，只读取目标 Sheet 而非整个 Excel 文件。
     * 二维数组的构造方式与 {@link BaseApiTest#apiCaseProvider} 完全一致。</p>
     *
     * @param sheetName Excel 中的 Sheet 名称
     * @return TestNG 数据驱动数组
     * @throws IOException 文件不存在或 Sheet 不存在时抛出
     */
    private Object[][] loadSheet(String sheetName) throws IOException {
        String path = getClass().getClassLoader().getResource(CASE_EXCEL).getFile();
        List<TestCase> cases = ExcelReader.readSheet(path, sheetName);
        Object[][] data = new Object[cases.size()][1];
        for (int i = 0; i < cases.size(); i++) {
            data[i][0] = cases.get(i);
        }
        LOG.info("[ModuleApiTest] Sheet '{}' 加载完成，共 {} 条用例", sheetName, cases.size());
        return data;
    }
}
