package com.example.autoframework.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 模板引擎 —— 负责将模板文件中的占位符替换为实际数据。
 *
 * <p>框架的核心设计之一：测试数据与请求结构分离。</p>
 * <ul>
 *   <li>请求结构（字段名、嵌套层级）写在 JSON 模板文件中，便于版本管理和代码审查</li>
 *   <li>具体字段值写在 Excel 中，便于测试工程师维护，无需改代码即可新增用例</li>
 * </ul>
 *
 * <p>支持两种变量占位符：</p>
 * <pre>
 *   ${varName}     → 从 Excel 当前行读取变量值（如 ${username}）
 *   ${__varName}   → 从 ApiContext 全局上下文读取（如 ${__token}，用于接口串联）
 * </pre>
 *
 * <p>渲染顺序：先替换上下文变量 ${__xxx}，再替换 Excel 变量 ${xxx}。
 * 这样设计的目的是：如果某个 Excel 变量恰好也叫 token，上下文变量的优先级更高，
 * 确保接口链路传递的变量不会被 Excel 列意外覆盖。</p>
 *
 * @see ExcelReader
 * @see ApiContext
 */
public class JsonTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(JsonTemplate.class);

    /**
     * 匹配 Excel 变量的正则：${varName}
     * 变量名规则：首字符必须是字母或下划线，后续可跟字母、数字、下划线。
     * 注意：不支持中划线（-）和点号（.），避免与 JSON 语法冲突。
     */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_]\\w*)\\}");

    /**
     * 匹配上下文变量的正则：${__varName}
     * 双下划线是框架约定的上下文变量前缀，便于与 Excel 普通变量区分。
     */
    private static final Pattern CTX_PATTERN = Pattern.compile("\\$\\{__([a-zA-Z_]\\w*)\\}");

    /**
     * 渲染模板文件，返回替换后的最终 JSON 字符串。
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>从 classpath 的 resources/templates/ 目录加载模板文件</li>
     *   <li>第一轮：扫描 ${__xxx}，从 {@link ApiContext} 读取值并替换</li>
     *   <li>第二轮：扫描 ${xxx}，从 Excel variables Map 读取值并替换</li>
     *   <li>对替换值进行 JSON 转义，防止引号破坏整体 JSON 结构</li>
     * </ol>
     *
     * @param templateFile resources/templates/ 下的文件名
     * @param variables    Excel 读取的变量映射
     * @return 替换后的最终 JSON 字符串
     * @throws IOException 模板文件不存在或读取失败时抛出
     */
    public static String render(String templateFile, Map<String, String> variables) throws IOException {
        LOG.debug("[JsonTemplate] 开始渲染模板: {}, 可用变量数: {}", templateFile, variables.size());

        // 1. 加载原始模板内容
        String template = loadTemplate(templateFile);
        LOG.trace("[JsonTemplate] 原始模板内容:\n{}", template);

        // ========== 第一步：替换上下文变量 ${__token} ==========
        LOG.debug("[JsonTemplate] 第一轮：替换上下文变量 ${__xxx} ...");
        Matcher ctxMatcher = CTX_PATTERN.matcher(template);
        StringBuffer sb1 = new StringBuffer();
        int ctxReplaceCount = 0;
        while (ctxMatcher.find()) {
            String varName = ctxMatcher.group(1);
            String value = ApiContext.get(varName);
            if (value == null) {
                LOG.error("[JsonTemplate] 模板 [{}] 上下文变量缺失: ${__{}}, 当前上下文 keys={}",
                        templateFile, varName, ApiContext.snapshot().keySet());
                throw new IllegalArgumentException(
                        String.format("模板 [%s] 上下文变量缺失: ${__%s}，请检查前置接口是否已提取该字段", templateFile, varName)
                );
            }
            LOG.debug("[JsonTemplate] 上下文变量替换: ${__{}} -> {}...", varName,
                    value.length() > 8 ? value.substring(0, 8) : value);
            ctxMatcher.appendReplacement(sb1, Matcher.quoteReplacement(escapeJson(value)));
            ctxReplaceCount++;
        }
        ctxMatcher.appendTail(sb1);
        template = sb1.toString();
        LOG.debug("[JsonTemplate] 上下文变量替换完成，共替换 {} 处", ctxReplaceCount);

        // ========== 第二步：替换 Excel 变量 ${username} ==========
        LOG.debug("[JsonTemplate] 第二轮：替换 Excel 变量 ${xxx} ...");
        Matcher varMatcher = VAR_PATTERN.matcher(template);
        StringBuffer sb2 = new StringBuffer();
        int varReplaceCount = 0;
        int varMissingCount = 0;
        while (varMatcher.find()) {
            String varName = varMatcher.group(1);
            String value = variables.get(varName);
            if (value == null) {
                LOG.warn("[JsonTemplate] 模板 [{}] 变量缺失: ${{}}, 可用变量 keys={}",
                        templateFile, varName, variables.keySet());
                value = "";
                varMissingCount++;
            } else {
                LOG.debug("[JsonTemplate] Excel 变量替换: ${{}} -> '{}'", varName, value);
                varReplaceCount++;
            }
            varMatcher.appendReplacement(sb2, Matcher.quoteReplacement(escapeJson(value)));
        }
        varMatcher.appendTail(sb2);
        LOG.debug("[JsonTemplate] Excel 变量替换完成，成功 {} 处，缺失 {} 处", varReplaceCount, varMissingCount);

        String result = sb2.toString();
        LOG.trace("[JsonTemplate] 渲染结果:\n{}", result);
        return result;
    }

    /**
     * 从 classpath 加载模板文件内容。
     *
     * <p>模板统一存放在 {@code src/test/resources/templates/} 目录下，
     * 通过 {@code ClassLoader.getResourceAsStream("templates/xxx.json")} 读取。
     * 使用 try-with-resources 确保 InputStream 自动关闭，避免资源泄漏。</p>
     *
     * @param templateFile 模板文件名
     * @return 模板文件的 UTF-8 字符串内容
     * @throws IOException 当模板文件不存在或读取 IO 异常时抛出
     */
    private static String loadTemplate(String templateFile) throws IOException {
        String path = "templates/" + templateFile;
        LOG.debug("[JsonTemplate] 加载模板路径: {}", path);
        try (InputStream is = JsonTemplate.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                LOG.error("[JsonTemplate] 模板文件不存在: {}", path);
                throw new IOException("模板文件不存在: " + path + "，请检查文件是否位于 src/test/resources/templates/ 目录");
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LOG.debug("[JsonTemplate] 模板加载成功，大小: {} 字节", content.length());
            return content;
        }
    }

    /**
     * 简单的 JSON 字符串转义。
     *
     * <p>为什么需要转义？假设 Excel 中某个变量值为 {@code He said "Hello"}，
     * 如果不转义双引号，直接替换到 JSON 模板中会导致整体结构非法：</p>
     * <pre>{"message": "He said "Hello""}  ← 解析报错</pre>
     *
     * <p>转义后变为：</p>
     * <pre>{"message": "He said \"Hello\""}  ← 合法 JSON</pre>
     *
     * @param s 原始字符串（可能来自 Excel 单元格）
     * @return 转义后的字符串，可直接嵌入 JSON 值中
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
