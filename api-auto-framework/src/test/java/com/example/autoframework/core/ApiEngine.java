package com.example.autoframework.core;

import com.example.autoframework.pojo.ApiResponse;
import com.example.autoframework.pojo.TestCase;
import com.example.autoframework.utils.LogMasker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求引擎 —— 框架的核心调度器。
 *
 * <p>本类负责串联整个接口请求的生命周期，处理顺序如下：</p>
 * <ol>
 *   <li><b>模板渲染</b>：调用 {@link JsonTemplate#render} 将 ${变量} 替换为实际值</li>
 *   <li><b>请求加密</b>：若 Excel 中 encrypt=1，则对渲染后的 JSON 做 AES/RSA 加密</li>
 *   <li><b>构建请求</b>：组装 RestAssured 请求对象，注入自定义 Header + 上下文 Token</li>
 *   <li><b>发送请求</b>：根据 method（GET/POST/PUT/DELETE/PATCH）路由到对应 HTTP 动作</li>
 *   <li><b>响应解密</b>：若 Excel 中 decrypt=1，对响应密文解密为明文 JSON</li>
 *   <li><b>变量提取</b>：根据 extract 规则，将响应字段存入 {@link ApiContext}，供下游接口使用</li>
 * </ol>
 *
 * <p><b>环境配置</b>：BASE_URL、CRYPTO_MODE 等参数优先从 JVM 系统属性读取（如 {@code -Dapi.baseUrl=http://localhost:8080}），
 * 未指定时采用默认值。这使得同一套代码可以在不修改源码的情况下切换测试环境（dev / sit / prod）。</p>
 *
 * <p><b>自动 Token 注入</b>：如果 {@link ApiContext} 中存在 key 为 "token" 的变量，
 * 引擎会自动将其加入请求头的 {@code Authorization: Bearer xxx}，无需在每个用例中手动填写 Header。</p>
 *
 * @see TestCase
 * @see JsonTemplate
 * @see CryptoUtils
 */
public class ApiEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ApiEngine.class);

    /** Jackson 对象映射器，用于 JSON 和 Map 之间的互相转换 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 接口基础地址，如 http://localhost:8080 */
    private static final String BASE_URL;

    /** 加解密算法模式：AES / RSA / NONE */
    private static final String CRYPTO_MODE;

    /**
     * 请求加密后的封装字段名。
     * 为空表示直接发送裸密文；非空则封装为 {"encryptedData": "密文"}。
     */
    private static final String ENCRYPT_FIELD;

    /**
     * 响应解密时的密文字段名。
     * 与 ENCRYPT_FIELD 配对使用，用于从外层 JSON 提取密文。
     */
    private static final String DECRYPT_FIELD;

    /**
     * 静态代码块：在类加载时从系统属性或环境变量读取全局配置。
     * 优先级：JVM 参数 (-Dxxx) > 默认值。
     */
    static {
        BASE_URL = System.getProperty("api.baseUrl", "http://localhost:8080");
        CRYPTO_MODE = System.getProperty("api.cryptoMode", "AES");
        ENCRYPT_FIELD = System.getProperty("api.encryptField", "");
        DECRYPT_FIELD = System.getProperty("api.decryptField", "");
        LOG.info("[ApiEngine] 全局配置加载完成: baseUrl={}, cryptoMode={}, encryptField='{}', decryptField='{}'",
                BASE_URL, CRYPTO_MODE, ENCRYPT_FIELD, DECRYPT_FIELD);
    }

    /**
     * 执行单个测试用例的完整链路。
     *
     * <p>本方法是框架的核心调度入口，{@link com.example.autoframework.tests.BaseApiTest#runApiTest}
     * 会循环调用此方法执行每一条 Excel 用例。</p>
     *
     * @param testCase 从 Excel 解析出的单条测试用例，包含请求参数、断言规则、变量映射等全部信息
     * @return 封装后的响应对象（含明文 body、原始密文、状态码、响应头）
     * @throws Exception 模板渲染失败、加解密异常、网络超时、JSON 解析错误等
     */
    public static ApiResponse execute(TestCase testCase) throws Exception {
        String caseId = testCase.getCaseId();
        LOG.info("[{}] ========== 用例执行开始 ==========", caseId);
        LOG.info("[{}] 基本信息: apiName={}, method={}, url={}, template={}",
                caseId, testCase.getApiName(), testCase.getMethod(), testCase.getUrl(), testCase.getTemplateFile());
        LOG.info("[{}] 加解密标记: encrypt={}, decrypt={}, extract='{}'",
                caseId, testCase.isEncrypt(), testCase.isDecrypt(), testCase.getExtract());
        LOG.debug("[{}] Excel 变量: {}", caseId, LogMasker.maskMap(testCase.getVariables().toString()));

        // ========== 第 1 步：渲染 JSON 模板 ==========
        LOG.info("[{}] Step 1/6 - 渲染 JSON 模板...", caseId);
        String requestBody = JsonTemplate.render(testCase.getTemplateFile(), testCase.getVariables());
        LOG.debug("[{}] 模板渲染结果: {}", caseId, LogMasker.mask(requestBody));

        // ========== 第 2 步：请求体加密（可选） ==========
        String payload = requestBody;
        if (testCase.isEncrypt()) {
            LOG.info("[{}] Step 2/6 - 请求体加密（模式: {}）...", caseId, CRYPTO_MODE);
            payload = encryptPayload(requestBody);
            LOG.debug("[{}] 加密后 payload 长度: {}", caseId, payload.length());
        } else {
            LOG.info("[{}] Step 2/6 - 请求体无需加密", caseId);
        }

        // ========== 第 3 步：构建 RestAssured 请求 ==========
        LOG.info("[{}] Step 3/6 - 构建 HTTP 请求...", caseId);
        RequestSpecification spec = RestAssured.given()
                .filter(new AllureRestAssured())
                .config(RestAssuredConfig.config()
                        .encoderConfig(EncoderConfig.encoderConfig()
                                .defaultContentCharset("UTF-8")))
                .baseUri(BASE_URL)
                .log().all();

        // 3.1 解析并注入 Excel 中定义的自定义请求头
        if (testCase.getHeaders() != null && !testCase.getHeaders().isEmpty()) {
            Map<String, String> headerMap = MAPPER.readValue(testCase.getHeaders(), new TypeReference<>() {});
            headerMap.forEach((k, v) -> LOG.debug("[{}] 自定义 Header: {}={}", caseId, k, v));
            headerMap.forEach(spec::header);
        }

        // 3.2 自动注入上下文中的 token（接口串联的关键机制）
        if (ApiContext.contains("token")) {
            String token = ApiContext.get("token");
            LOG.info("[{}] 自动注入 Authorization: Bearer {}...", caseId,
                    token != null && token.length() > 6 ? token.substring(0, 6) + "***" : token);
            spec.header("Authorization", "Bearer " + token);
        } else {
            LOG.info("[{}] 上下文中无 token，跳过 Authorization 注入", caseId);
        }

        // ========== 第 4 步：根据 HTTP 方法发送请求 ==========
        LOG.info("[{}] Step 4/6 - 发送 {} 请求到 {}{} ...", caseId, testCase.getMethod(), BASE_URL, testCase.getUrl());
        Response response;
        String url = testCase.getUrl();
        String method = testCase.getMethod().toUpperCase();

        long startTime = System.currentTimeMillis();
        switch (method) {
            case "GET" -> {
                Map<String, Object> params = parseQueryBody(requestBody);
                LOG.debug("[{}] GET Query 参数: {}", caseId, params);
                response = spec.queryParams(params).get(url);
            }
            case "POST" -> response = spec.contentType(ContentType.JSON).body(payload).post(url);
            case "PUT" -> response = spec.contentType(ContentType.JSON).body(payload).put(url);
            case "DELETE" -> response = spec.contentType(ContentType.JSON).body(payload).delete(url);
            case "PATCH" -> response = spec.contentType(ContentType.JSON).body(payload).patch(url);
            default -> throw new IllegalArgumentException("不支持的 HTTP 方法: " + method
                    + "，当前仅支持 GET/POST/PUT/DELETE/PATCH");
        }
        long cost = System.currentTimeMillis() - startTime;
        LOG.info("[{}] 请求完成，耗时 {} ms，HTTP 状态码: {}", caseId, cost, response.statusCode());

        // ========== 第 5 步：处理响应体（解密） ==========
        String rawBody = response.body().asString();
        LOG.debug("[{}] 原始响应体长度: {} 字节", caseId, rawBody.length());
        String decryptedBody = rawBody;

        if (testCase.isDecrypt()) {
            LOG.info("[{}] Step 5/6 - 响应体解密（模式: {}）...", caseId, CRYPTO_MODE);
            decryptedBody = decryptPayload(rawBody);
            LOG.debug("[{}] 解密后响应体: {}", caseId, LogMasker.mask(decryptedBody));
        } else {
            LOG.info("[{}] Step 5/6 - 响应体无需解密", caseId);
        }

        // ========== 第 6 步：提取响应字段到上下文 ==========
        if (testCase.getExtract() != null && !testCase.getExtract().isEmpty()) {
            LOG.info("[{}] Step 6/6 - 提取响应变量，规则: '{}'", caseId, testCase.getExtract());
            extractFields(decryptedBody, testCase.getExtract(), caseId);
        } else {
            LOG.info("[{}] Step 6/6 - 无需提取响应变量", caseId);
        }

        LOG.info("[{}] ========== 用例执行结束 ==========\n", caseId);

        return ApiResponse.builder()
                .statusCode(response.statusCode())
                .body(decryptedBody)
                .headers(response.headers())
                .rawBody(rawBody)
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 对明文请求体进行加密。
     *
     * <p>如果配置了 api.encryptField（如 encryptedData），则将密文封装为 JSON 对象：
     * {@code {"encryptedData":"Base64Cipher"}}；否则直接返回裸密文字符串。</p>
     *
     * @param plainJson 渲染后的明文 JSON
     * @return 加密后的字符串或封装 JSON
     * @throws Exception 加密失败时抛出
     */
    private static String encryptPayload(String plainJson) throws Exception {
        if (!ENCRYPT_FIELD.isEmpty()) {
            LOG.debug("[ApiEngine] 请求体将封装到字段 '{}' 中", ENCRYPT_FIELD);
            String cipher = CryptoUtils.encryptBody(plainJson, CRYPTO_MODE);
            return "{\"" + ENCRYPT_FIELD + "\":\"" + cipher + "\"}";
        }
        return CryptoUtils.encryptBody(plainJson, CRYPTO_MODE);
    }

    /**
     * 对响应体进行解密。
     *
     * <p>如果配置了 api.decryptField，先从外层 JSON 提取密文字段，再解密；
     * 否则直接对整个响应体做解密。</p>
     *
     * @param rawJson 服务端返回的原始响应字符串
     * @return 解密后的明文 JSON
     * @throws Exception 解密失败时抛出
     */
    private static String decryptPayload(String rawJson) throws Exception {
        if (!DECRYPT_FIELD.isEmpty()) {
            return CryptoUtils.decryptWrappedBody(rawJson, CRYPTO_MODE, DECRYPT_FIELD);
        }
        return CryptoUtils.decryptBody(rawJson, CRYPTO_MODE);
    }

    /**
     * 从响应 JSON 中提取指定字段，并存入全局上下文。
     *
     * <p>支持多字段同时提取，规则之间用英文分号 {@code ;} 分隔。
     * 左侧为存入上下文时的变量名，右侧为 JSON Path 表达式。</p>
     *
     * <p>示例：{@code token=data.token;userId=data.id}</p>
     *
     * <p>底层使用 RestAssured 的 JsonPath 实现，支持标准 JSON Path 语法，如：</p>
     * <ul>
     *   <li>{@code data.token} — 取对象属性</li>
     *   <li>{@code data.users[0].name} — 取数组第一个元素的属性</li>
     *   <li>{@code [0].id} — 取根数组第一个元素的 id</li>
     * </ul>
     *
     * @param jsonBody     解密后的响应 JSON 字符串
     * @param extractRule  提取规则，格式为 "varName=jsonPath;..."
     * @param caseId       用例编号，用于日志标识
     * @throws IOException 响应体不是合法 JSON 时可能抛出
     */
    private static void extractFields(String jsonBody, String extractRule, String caseId) throws IOException {
        String[] rules = extractRule.split(";");
        for (String rule : rules) {
            String[] kv = rule.trim().split("=", 2);
            if (kv.length != 2) {
                LOG.warn("[{}] 提取规则格式非法，已跳过: '{}'", caseId, rule);
                continue;
            }

            String varName = kv[0].trim();
            String jsonPath = kv[1].trim();

            try {
                Object value = io.restassured.path.json.JsonPath.from(jsonBody).get(jsonPath);
                if (value != null) {
                    String strValue = String.valueOf(value);
                    ApiContext.put(varName, strValue);
                    if (strValue.length() > 8) {
                        LOG.info("[{}] 提取成功: {} = {}... (JSON Path: {})", caseId, varName, strValue.substring(0, 8), jsonPath);
                    } else {
                        LOG.info("[{}] 提取成功: {} = {} (JSON Path: {})", caseId, varName, strValue, jsonPath);
                    }
                } else {
                    LOG.warn("[{}] 提取结果为空: {} (JSON Path: {})，未存入上下文", caseId, varName, jsonPath);
                }
            } catch (Exception e) {
                LOG.error("[{}] 提取失败: {} (JSON Path: {})，原因: {}", caseId, varName, jsonPath, e.getMessage());
            }
        }
    }

    /**
     * 将 JSON 字符串解析为 Map，用于 GET 请求的 Query 参数。
     *
     * <p>例如模板渲染后得到 {@code {"page":1,"size":10}}，
     * 此方法将其转为 Map，然后通过 {@code spec.queryParams(params)} 附加到 URL 上，
     * 最终形成 {@code /api/users?page=1&size=10}。</p>
     *
     * <p>若解析失败（如模板为空 JSON {}），返回空 Map，不会中断请求。</p>
     *
     * @param json 模板渲染后的 JSON 字符串
     * @return 键值对形式的参数 Map
     */
    private static Map<String, Object> parseQueryBody(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            LOG.debug("[ApiEngine] GET Query 参数解析失败（可能模板为空），返回空 Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
