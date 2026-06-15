package com.example.autoframework.listeners;

import com.example.autoframework.core.ApiContext;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Map;

/**
 * TestNG 全局监听器 —— 负责在测试生命周期关键节点与 Allure 报告交互。
 *
 * <p>本类实现了 {@link ITestListener} 接口，通过 {@code @Listeners} 注解注册到测试类上。
 * 它的核心职责是：</p>
 * <ul>
 *   <li><b>上下文追踪</b>：用例成功或失败时，将 {@link ApiContext} 中的全部变量打印为 Allure 附件，
 *       方便排查 "为什么下游接口拿不到 token" 这类链路问题</li>
 *   <li><b>失败增强</b>：用例失败时，在报告中附加失败原因摘要，减少查看原始日志的时间</li>
 *   <li><b>生命周期日志</b>：在控制台输出测试套件启动/结束标记，便于在 CI 日志中快速定位测试区间</li>
 * </ul>
 *
 * <p><b>为什么不直接在测试方法里写 Allure.step？</b>
 * 因为本类是全局监听，无论用例写在哪个 Test Class 中，都能统一附加上下文快照，
 * 避免每个测试类重复编写相同的上下文打印逻辑。</p>
 *
 * @see com.example.autoframework.tests.BaseApiTest
 */
public class AllureTestListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(AllureTestListener.class);

    /**
     * 测试套件启动时触发。
     *
     * @param context TestNG 的测试上下文，包含套件名称、参数等信息
     */
    @Override
    public void onStart(ITestContext context) {
        LOG.info("========== 测试套件启动: {} ==========", context.getName());
    }

    /**
     * 测试套件结束时触发。
     *
     * @param context TestNG 的测试上下文
     */
    @Override
    public void onFinish(ITestContext context) {
        LOG.info("========== 测试套件结束: {} ==========", context.getName());
    }

    /**
     * 单个测试方法开始执行前触发。
     *
     * <p>如果测试方法带有参数（如数据驱动），则取第一个参数（即 {@link com.example.autoframework.pojo.TestCase}）
     * 的 toString() 作为用例标识；否则使用方法名。</p>
     *
     * @param result 当前测试方法的结果对象，包含方法元数据、参数等信息
     */
    @Override
    public void onTestStart(ITestResult result) {
        String caseName = result.getParameters().length > 0
                ? result.getParameters()[0].toString()
                : result.getMethod().getMethodName();
        LOG.info("[AllureListener] 用例开始: {}", caseName);
        Allure.step("开始执行: " + caseName);
    }

    /**
     * 测试方法执行成功时触发。
     *
     * <p>将当前线程上下文中的全部变量作为附件附加到 Allure 报告中，
     * 标签为 "成功时上下文快照"。</p>
     *
     * @param result 当前测试方法的结果对象
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("[AllureListener] 用例执行成功: {}", result.getMethod().getMethodName());
        attachContextSnapshot("成功时上下文快照");
    }

    /**
     * 测试方法执行失败时触发。
     *
     * <p>除了附加上下文快照外，还会额外附加一条失败原因说明，
     * 内容为异常的消息文本（通常是断言失败的描述）。</p>
     *
     * @param result 当前测试方法的结果对象，包含抛出的异常信息
     */
    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("[AllureListener] 用例执行失败: {}, 原因: {}",
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "未知错误");
        attachContextSnapshot("失败时上下文快照");
        Allure.step("失败原因: " + result.getThrowable().getMessage());
    }

    /**
     * 测试方法被跳过时触发（通常因为依赖的上游方法失败）。
     *
     * @param result 当前测试方法的结果对象
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("[AllureListener] 用例跳过: {}, 原因: {}",
                result.getMethod().getMethodName(),
                result.getThrowable() != null ? result.getThrowable().getMessage() : "无异常信息");
        Allure.step("用例跳过: " + result.getThrowable().getMessage());
    }

    /**
     * 将当前 {@link ApiContext} 的快照附加到 Allure 报告中。
     *
     * <p>输出格式为 key=value 的纯文本，每行一个变量。
     * 如果上下文中没有任何变量，则输出 "(空)"，避免报告中出现空白附件。</p>
     *
     * @param title 附件标题，如 "成功时上下文快照" 或 "失败时上下文快照"
     */
    private void attachContextSnapshot(String title) {
        Map<String, String> snapshot = ApiContext.snapshot();
        StringBuilder sb = new StringBuilder();
        snapshot.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
        if (sb.isEmpty()) sb.append("(空)");
        LOG.debug("[AllureListener] 附加上下文快照到 Allure: {}，变量数: {}", title, snapshot.size());
        Allure.addAttachment(title, "text/plain", sb.toString(), "txt");
    }
}
