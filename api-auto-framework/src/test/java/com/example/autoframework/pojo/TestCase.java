package com.example.autoframework.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 测试用例实体 —— 对应 Excel 中的每一行数据，是框架流转的核心数据载体。
 *
 * <p>本类使用 Lombok 的 {@code @Data} 和 {@code @Builder} 注解，自动生成 Getter/Setter、
 * equals/hashCode/toString 以及 Builder 构造器，大幅简化代码量并提高可读性。</p>
 *
 * <p>字段分为三大类：</p>
 * <ul>
 *   <li><b>基础信息</b>：caseId、caseName、apiName —— 用于标识和报告展示</li>
 *   <li><b>请求配置</b>：method、url、templateFile、headers、encrypt、decrypt —— 控制 HTTP 请求行为</li>
 *   <li><b>断言与变量</b>：expectedCode、expectedField、expectedValue、extract、variables —— 控制校验和链路传递</li>
 * </ul>
 *
 * <p>动态变量 {@link #variables} 的 key 是 Excel 中的原始列名（大小写敏感），
 * value 是单元格内容。该 Map 会直接传递给 {@link com.example.autoframework.core.JsonTemplate#render} 进行模板替换。</p>
 *
 * @see com.example.autoframework.core.ExcelReader
 * @see com.example.autoframework.core.ApiEngine
 */
@Data
@Builder
public class TestCase {

    /** 用例编号，唯一标识一条测试用例。如 LOGIN_001、ORDER_003。 */
    private String caseId;

    /** 用例名称，用于人类阅读和理解测试意图。如 "正常登录"、"密码错误"。 */
    private String caseName;

    /**
     * 接口标识 / 模块名称，用于 Allure 报告的分组（Feature 级别）。
     * 相同 apiName 的用例会在报告中折叠到同一个功能模块下。
     */
    private String apiName;

    /**
     * HTTP 请求方法，支持：GET、POST、PUT、DELETE、PATCH（大小写不敏感）。
     * 在 {@link com.example.autoframework.core.ApiEngine} 中通过 switch 路由到对应的 RestAssured 方法。
     */
    private String method;

    /**
     * 请求路径（不含 baseUrl）。
     * 如 {@code /api/login}、{@code /api/users}。
     * 最终请求的完整 URI = baseUrl + url。
     */
    private String url;

    /**
     * JSON 模板文件名，必须位于 {@code src/test/resources/templates/} 目录下。
     * 如 "login.json"。GET 请求若无需 body，可使用空模板 "empty.json"。
     */
    private String templateFile;

    /**
     * 自定义请求头，JSON 格式字符串。
     * 示例：{@code {"X-Request-Id":"uuid001","X-Source":"automation"}}
     * 为空时表示不额外添加自定义头（ApiEngine 仍会自动注入 Authorization）。
     */
    private String headers;

    /**
     * 是否加密请求体。
     * 取值 "1" 或 "true" 表示启用加密；其他值（包括空字符串）表示不加密。
     * 加密逻辑在 {@link com.example.autoframework.core.ApiEngine#execute} 中处理。
     */
    private String encrypt;

    /**
     * 是否解密响应体。
     * 取值 "1" 或 "true" 表示启用解密；其他值表示不解密。
     * 解密后的明文存入 {@link ApiResponse#body}，原始密文保留在 {@link ApiResponse#rawBody}。
     */
    private String decrypt;

    /**
     * 响应提取规则，支持多字段同时提取，字段之间用英文分号 {@code ;} 分隔。
     *
     * <p>格式：{@code 上下文变量名=JSONPath表达式}</p>
     * <p>示例：</p>
     * <pre>
     *   token=data.token
     *   token=data.token;userId=data.user.id
     * </pre>
     *
     * <p>提取后的变量存入 {@link com.example.autoframework.core.ApiContext}，
     * 下游接口的模板中可通过 {@code ${__token}} 引用。</p>
     */
    private String extract;

    /**
     * 期望 HTTP 状态码。
     * 如 "200"、"401"、"500"。为空时不做状态码断言。
     */
    private String expectedCode;

    /**
     * 期望校验字段，使用 JSON Path 语法定位响应体中的字段。
     *
     * <p>示例：</p>
     * <ul>
     *   <li>{@code role} — 顶层字段</li>
     *   <li>{@code data.status} — 嵌套对象字段</li>
     *   <li>{@code [0].username} — 根数组第一个元素的字段</li>
     * </ul>
     *
     * 为空时不做字段值断言。
     */
    private String expectedField;

    /**
     * 期望字段值，与 {@link #expectedField} 配对使用。
     * 框架会使用 AssertJ 断言实际值是否等于该期望值。
     */
    private String expectedValue;

    /**
     * 模板变量映射表。
     * key 为 Excel 中的动态列名（保留原始大小写），value 为单元格字符串值。
     * 此 Map 由 {@link com.example.autoframework.core.ExcelReader} 解析生成，
     * 供 {@link com.example.autoframework.core.JsonTemplate#render} 消费。
     */
    private Map<String, String> variables;

    /**
     * 判断是否需要加密请求体。
     * 支持 "1" 和 "true"（不区分大小写）两种写法，兼容不同测试工程师的习惯。
     *
     * @return true 表示需要加密
     */
    public boolean isEncrypt() {
        return "1".equals(encrypt) || "true".equalsIgnoreCase(encrypt);
    }

    /**
     * 判断是否需要解密响应体。
     * 支持 "1" 和 "true"（不区分大小写）两种写法。
     *
     * @return true 表示需要解密
     */
    public boolean isDecrypt() {
        return "1".equals(decrypt) || "true".equalsIgnoreCase(decrypt);
    }
}
