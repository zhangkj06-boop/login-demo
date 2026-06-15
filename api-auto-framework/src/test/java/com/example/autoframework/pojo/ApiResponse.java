package com.example.autoframework.pojo;

import io.restassured.http.Headers;
import lombok.Builder;
import lombok.Data;

/**
 * 统一响应封装对象 —— 将 RestAssured 的原始响应转换为框架内部可操作的 POJO。
 *
 * <p>为什么需要这个封装？</p>
 * <ul>
 *   <li>RestAssured 的 {@code Response} 对象在多次读取 body 后会出现流已关闭的问题</li>
 *   <li>加解密场景下，需要同时保留明文（用于断言）和密文（用于问题追溯）</li>
 *   <li>统一的数据结构便于 Allure 监听器、断言工具等下游组件处理</li>
 * </ul>
 *
 * <p>设计时保留了原始密文字段 {@link #rawBody}，当测试失败时，测试工程师可以
 * 对比明文和密文，快速定位是解密算法错误还是服务端返回了异常数据。</p>
 *
 * @see ApiEngine#execute(TestCase)
 * @see com.example.autoframework.tests.BaseApiTest#runApiTest(TestCase)
 */
@Data
@Builder
public class ApiResponse {

    /**
     * HTTP 响应状态码，如 200、401、500。
     * 由 {@code response.statusCode()} 直接获取，对应 Excel 中的 expectedCode 断言字段。
     */
    private int statusCode;

    /**
     * 响应体字符串（解密后的明文 JSON）。
     * 若未启用解密，则与 rawBody 内容相同。
     * 断言组件（如 JsonPath）会对此字段进行解析和校验。
     */
    private String body;

    /**
     * 响应头集合。
     * 类型使用 RestAssured 的 {@link Headers}，支持按名称获取多个同名头的值。
     * 目前框架自动注入的是响应头中的 Set-Cookie、Content-Type 等通用字段。
     */
    private Headers headers;

    /**
     * 原始响应体（解密前的密文字符串，或服务器返回的原始内容）。
     * 仅在 decrypt=1 时与 body 不同；用于日志追溯和加解密问题排查。
     */
    private String rawBody;
}
