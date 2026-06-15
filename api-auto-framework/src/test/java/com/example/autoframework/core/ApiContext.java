package com.example.autoframework.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局上下文管理器（线程隔离）
 *
 * <p>在接口自动化测试中，多个接口之间经常需要传递动态数据，例如：</p>
 * <ul>
 *   <li>登录接口返回的 token，需要传递给后续所有需要鉴权的接口</li>
 *   <li>创建订单接口返回的 orderId，需要传递给查询/支付接口</li>
 * </ul>
 *
 * <p>本类使用 {@link ThreadLocal} 实现线程级别的变量隔离，确保并发执行用例时
 * 各个线程的上下文数据互不干扰。每个测试线程拥有自己独立的 HashMap 存储空间。</p>
 *
 * <p>典型使用链路：</p>
 * <pre>
 *   1. 登录接口执行成功后，通过 {@link ApiEngine#execute} 的 extract 机制
 *      自动将 token 存入上下文：ApiContext.put("token", "xxx")
 *   2. 后续接口在 {@link ApiEngine} 中自动读取：ApiContext.get("token")
 *      并注入到请求头的 Authorization 字段中
 *   3. 每个测试类执行前（{@code @BeforeClass}）应调用 {@link #clear()} 清理上下文，
 *      防止用例间数据污染
 * </pre>
 *
 * @see ApiEngine#execute(com.example.autoframework.pojo.TestCase)
 * @see com.example.autoframework.tests.BaseApiTest#setup()
 */
public class ApiContext {

    private static final Logger LOG = LoggerFactory.getLogger(ApiContext.class);

    /**
     * ThreadLocal 存储结构：每个线程独立维护一个 HashMap。
     * 使用 {@code ThreadLocal.withInitial(HashMap::new)} 确保首次 get() 时自动初始化，
     * 避免后续出现 NullPointerException。
     */
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    /**
     * 向当前线程的上下文中存入键值对。
     *
     * @param key   变量名（如 "token", "userId"），建议统一使用驼峰命名
     * @param value 变量值，允许为 null（但后续引用时需注意空指针）
     */
    public static void put(String key, String value) {
        CONTEXT.get().put(key, value);
        // 日志中记录 key 即可，value 可能敏感（如 token），在 DEBUG 级别下只打印前 8 位
        if (value != null && value.length() > 8) {
            LOG.debug("[ApiContext] put key={}, value={}...", key, value.substring(0, 8));
        } else {
            LOG.debug("[ApiContext] put key={}, value={}", key, value);
        }
    }

    /**
     * 从当前线程的上下文中读取变量值。
     *
     * @param key 变量名
     * @return 变量值；若 key 不存在则返回 null
     */
    public static String get(String key) {
        String value = CONTEXT.get().get(key);
        LOG.debug("[ApiContext] get key={}, hit={}", key, value != null);
        return value;
    }

    /**
     * 判断当前线程上下文中是否包含指定 key。
     *
     * <p>在 {@link ApiEngine} 中用于判断是否需要自动注入 Authorization Header，
     * 避免每次都往请求头里塞一个空值。</p>
     *
     * @param key 变量名
     * @return true 表示存在该 key
     */
    public static boolean contains(String key) {
        boolean exists = CONTEXT.get().containsKey(key);
        LOG.debug("[ApiContext] contains key={}, result={}", key, exists);
        return exists;
    }

    /**
     * 从当前线程上下文中移除指定 key。
     *
     * @param key 变量名
     */
    public static void remove(String key) {
        CONTEXT.get().remove(key);
        LOG.debug("[ApiContext] remove key={}", key);
    }

    /**
     * 清空当前线程上下文中的所有变量。
     *
     * <p>建议在 {@code @BeforeClass} 或 {@code @AfterClass} 中调用，
     * 确保用例组之间的数据完全隔离。</p>
     */
    public static void clear() {
        CONTEXT.get().clear();
        LOG.info("[ApiContext] 上下文已清空，当前线程所有变量已移除");
    }

    /**
     * 获取当前线程上下文的快照（深拷贝）。
     *
     * <p>主要用于 Allure 报告附件输出，将当前所有变量打印到报告中，
     * 方便排查接口链路问题。返回的是新 HashMap，外部修改不会影响上下文本身。</p>
     *
     * @return 包含当前所有上下文变量的 Map 副本
     */
    public static Map<String, String> snapshot() {
        Map<String, String> snap = new HashMap<>(CONTEXT.get());
        LOG.debug("[ApiContext] snapshot taken, size={}", snap.size());
        return snap;
    }
}
