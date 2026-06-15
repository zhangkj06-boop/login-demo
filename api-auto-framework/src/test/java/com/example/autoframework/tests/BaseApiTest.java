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
 * 接口自动化测试统一入口 —— 所有用例通过 Excel 数据驱动执行。
 *
 * <p>本类的设计哲学是 "一个测试方法跑全部用例"：</p>
 * <ul>
 *   <li><b>零代码新增用例</b>：测试工程师只需在 Excel 中新增一行数据，无需编写任何 Java 代码</li>
 *   <li><b>统一执行逻辑</b>：渲染、加密、请求、解密、断言、提取变量全部封装在 {@link ApiEngine} 中，
 *       本类只做调度和断言，避免重复代码</li>
 *   <li><b>Allure 报告集成</b>：通过注解自动为每条用例生成 Epic/Feature/Story 层级，
 *       并附加请求模板、变量值、响应体等关键附件</li>
 * </ul>
 *
 * <p><b>执行链路</b>：</p>
 * <pre>
 *   1. @BeforeClass 清理 ApiContext，防止历史用例数据污染
 *   2. @DataProvider 从 Excel 读取全部用例，转换为 Object[][] 供 TestNG 迭代
 *   3. @Test 循环执行每一条用例：
 *      a. Allure 元数据标注（Epic / Feature / Story / Issue）
 *      b. 附加请求模板和变量值到报告
 *      c. 调用 ApiEngine.execute() 完成请求全生命周期
 *      d. 附加响应体到报告
 *      e. AssertJ 断言状态码和字段值
 * </pre>
 *
 * @see com.example.autoframework.core.ApiEngine
 * @see com.example.autoframework.core.ExcelReader
 * @see com.example.autoframework.listeners.AllureTestListener
 */
@Listeners(com.example.autoframework.listeners.AllureTestListener.class)
public class BaseApiTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseApiTest.class);

    /**
     * Excel 用例文件路径。
     * 优先从 JVM 系统属性 {@code case.excel} 读取，支持运行时动态指定：
     * {@code mvn test -Dcase.excel=cases/prod_cases.xlsx}
     * 未指定时默认使用 {@code cases/sample_cases.xlsx}。
     */
    private static final String CASE_EXCEL = System.getProperty("case.excel", "cases/sample_cases.xlsx");

    /**
     * 测试类初始化：清理全局上下文并打印加载信息。
     *
     * <p>为什么要在 @BeforeClass 中 clear？
     * 因为 TestNG 的默认行为是按类实例化，如果不清理，上一次运行残留的 token 等变量
     * 会污染本次执行，导致断言结果不可预期。</p>
     */
    @BeforeClass
    public void setup() {
        ApiContext.clear();
        LOG.info("[BaseApiTest] 测试类初始化完成，加载用例文件: {}", CASE_EXCEL);
    }

    /**
     * TestNG 数据驱动：从 Excel 读取全部用例。
     *
     * <p>返回 {@code Object[用例数][1]} 的二维数组，每个元素是一个 {@link TestCase} 对象。
     * TestNG 会按数组顺序迭代调用标记了 {@code @Test(dataProvider = "apiCaseProvider")} 的测试方法。
     * {@code parallel = false} 表示串行执行，避免并发导致的上下文竞争；
     * 如需并行，需将 {@link ApiContext} 的 ThreadLocal 机制配合 {@code parallel = true} 使用。</p>
     *
     * @return 二维数组，每个内层数组包含一个 TestCase
     * @throws IOException Excel 文件不存在或解析失败时抛出
     */
    @DataProvider(name = "apiCaseProvider", parallel = false)
    public Object[][] apiCaseProvider() throws IOException {
        LOG.info("[BaseApiTest] 数据驱动加载中...");
        // 通过类加载器从 classpath 中定位 Excel 文件（支持打包后从 jar 内读取）
        List<TestCase> cases = ExcelReader.readAllCases(
                getClass().getClassLoader().getResource(CASE_EXCEL).getFile()
        );
        Object[][] data = new Object[cases.size()][1];
        for (int i = 0; i < cases.size(); i++) {
            data[i][0] = cases.get(i);
        }
        LOG.info("[BaseApiTest] 数据驱动加载完成，共 {} 条用例", cases.size());
        return data;
    }

    /**
     * 通用接口执行入口 —— 所有用例统一走这一个测试方法。
     *
     * <p>方法参数 {@code testCase} 由 TestNG 的数据驱动注入，每条 Excel 行数据触发一次此方法调用。
     * 方法内部不负责具体 HTTP 逻辑，只负责：</p>
     * <ol>
     *   <li>Allure 报告装饰（分层、附件）</li>
     *   <li>调用引擎执行请求</li>
     *   <li>AssertJ 流式断言</li>
     * </ol>
     *
     * @param testCase 单条测试用例数据（来自 Excel）
     * @throws Exception 模板渲染、网络请求、加解密、JSON 解析等环节的异常
     */
    @Test(dataProvider = "apiCaseProvider")
    @Step("执行用例: {testCase.caseId}")
    @Description("接口自动化用例执行")
    public void runApiTest(TestCase testCase) throws Exception {
        // ========== Allure 报告元数据 ==========
        // Epic 为最高层级，Feature 按 apiName 分组，Story 为具体用例名
        Allure.epic("接口自动化");
        Allure.feature(testCase.getApiName());
        Allure.story(testCase.getCaseName());
        // Issue 链接：caseId 作为 issue 编号，方便在报告中反向追溯到 Excel 行号
        Allure.issue(testCase.getCaseId(), testCase.getCaseId());

        LOG.info("[BaseApiTest] 开始执行用例: {} - {}", testCase.getCaseId(), testCase.getCaseName());

        // ========== 附加请求信息到 Allure 报告 ==========
        // 将原始模板内容作为附件输出，便于排查 "变量是否被正确替换"
        String templateContent = loadTemplateInfo(testCase.getTemplateFile());
        Allure.addAttachment("请求模板", "application/json", templateContent, ".txt");
        // 将 Excel 变量 Map 作为附件输出，便于排查 "变量值是否符合预期"
        Allure.addAttachment("用例变量", "text/plain", testCase.getVariables().toString(), ".txt");

        // ========== 调用请求引擎执行接口调用 ==========
        // 内部完成：模板渲染 -> 加密 -> 发送 -> 解密 -> 提取变量
        ApiResponse response = ApiEngine.execute(testCase);

        // ========== 附加响应信息到 Allure 报告 ==========
        // 明文响应体：用于人工审查和断言回溯
        Allure.addAttachment("响应体(明文)", "application/json", response.getBody(), ".txt");
        // 密文响应体：仅当启用了解密时才附加，用于排查加解密问题
        if (testCase.isDecrypt()) {
            Allure.addAttachment("响应体(密文)", "text/plain", response.getRawBody(), ".txt");
        }

        // ========== 断言 1：HTTP 状态码 ==========
        // 使用 AssertJ 的 as() 方法提供自定义错误消息，断言失败时能立即知道是哪条用例出错
        if (testCase.getExpectedCode() != null && !testCase.getExpectedCode().isEmpty()) {
            int expectedStatus = Integer.parseInt(testCase.getExpectedCode());
            LOG.info("[BaseApiTest] 断言状态码: expected={}, actual={}", expectedStatus, response.getStatusCode());
            Assertions.assertThat(response.getStatusCode())
                    .as("HTTP 状态码校验失败 [%s]", testCase.getCaseId())
                    .isEqualTo(expectedStatus);
            LOG.info("[BaseApiTest] 状态码断言通过");
        } else {
            LOG.info("[BaseApiTest] 无状态码断言，跳过");
        }

        // ========== 断言 2：JSON Path 字段值 ==========
        // 使用 RestAssured 的 JsonPath 从响应体中提取字段，支持嵌套路径和数组索引
        // 注意：JsonPath.get() 返回类型随 JSON 实际类型变化（String / Boolean / Integer / Long 等），
        // 因此使用 Object 接收，再通过 String.valueOf() 统一转为字符串比较，避免 ClassCastException
        if (testCase.getExpectedField() != null && !testCase.getExpectedField().isEmpty()) {
            Object actualValue = JsonPath.from(response.getBody()).get(testCase.getExpectedField());
            String actual = String.valueOf(actualValue);
            LOG.info("[BaseApiTest] 断言字段值: path='{}', expected='{}', actual='{}' (原始类型: {})",
                    testCase.getExpectedField(), testCase.getExpectedValue(), actual,
                    actualValue != null ? actualValue.getClass().getSimpleName() : "null");
            Assertions.assertThat(actual)
                    .as("字段 [%s] 校验失败 [%s]", testCase.getExpectedField(), testCase.getCaseId())
                    .isEqualTo(testCase.getExpectedValue());
            LOG.info("[BaseApiTest] 字段断言通过");
        } else {
            LOG.info("[BaseApiTest] 无字段断言，跳过");
        }

        LOG.info("[BaseApiTest] 用例执行完成: {} - {}\n", testCase.getCaseId(), testCase.getCaseName());
    }

    /**
     * 从 classpath 加载模板文件内容，用于附加到 Allure 报告中。
     *
     * <p>若模板文件不存在或读取失败，返回错误提示字符串而非抛异常，
     * 避免因为一个附件的失败导致整条用例中断。</p>
     *
     * @param templateFile 模板文件名，如 "login.json"
     * @return 模板文件内容字符串
     */
    private String loadTemplateInfo(String templateFile) {
        try {
            return new String(getClass().getClassLoader()
                    .getResourceAsStream("templates/" + templateFile)
                    .readAllBytes());
        } catch (Exception e) {
            LOG.warn("[BaseApiTest] 读取模板失败: {}", templateFile);
            return "读取模板失败: " + templateFile;
        }
    }
}
