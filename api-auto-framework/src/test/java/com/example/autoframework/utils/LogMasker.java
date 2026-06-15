package com.example.autoframework.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志脱敏工具 —— 防止敏感信息（密码、Token、密文等）泄露到日志中。
 *
 * <p>在接口自动化测试中，日志是必不可少的排查手段，但直接将明文密码、UUID Token、
 * 加密密钥等写入日志文件会带来严重的安全风险（尤其是 CI 环境中的共享日志）。
 * 本工具通过正则表达式识别并替换敏感字段的值，在保留日志可读性的同时确保数据安全。</p>
 *
 * <p>脱敏规则：</p>
 * <ul>
 *   <li>password / passwd / pwd — 任何形式的密码</li>
 *   <li>token / authorization / auth — 认证令牌</li>
 *   <li>secret / key / cipher / encryptedData — 密钥与密文</li>
 *   <li>phone / mobile / idCard / identity — 个人隐私信息</li>
 * </ul>
 *
 * <p>脱敏后格式：{@code "password":"******"} 或 {@code "token":"******"}</p>
 */
public class LogMasker {

    /**
     * 敏感字段名正则（不区分大小写）。
     * 匹配 JSON 键名：从双引号开始到双引号结束，后面紧跟冒号。
     */
    private static final Pattern SENSITIVE_KEY_PATTERN = Pattern.compile(
            "\"(password|passwd|pwd|token|authorization|auth|secret|key|cipher|encryptedData|phone|mobile|idCard|identity)\"\\s*:",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 敏感字段的值匹配正则。
     * 匹配冒号后的值：可能是双引号字符串、数字、true/false/null。
     * 本正则较为宽松，能覆盖绝大多数 JSON 值形式。
     */
    private static final Pattern SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(\"(?:password|passwd|pwd|token|authorization|auth|secret|key|cipher|encryptedData|phone|mobile|idCard|identity)\"\\s*:\\s*)" +
            "(\"(?:[^\"\\\\]|\\\\.)*\"|[^,\\}\\]\\s]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 对 JSON 字符串中的敏感字段进行脱敏。
     *
     * <p>如果输入不是 JSON（如纯文本密文），则不会匹配任何内容，原样返回。
     * 若输入为 null，返回空字符串。</p>
     *
     * @param jsonOrText 可能包含敏感字段的 JSON 字符串或普通文本
     * @return 脱敏后的字符串，敏感值被替换为 "******"
     */
    public static String mask(String jsonOrText) {
        if (jsonOrText == null) return "";
        Matcher matcher = SENSITIVE_VALUE_PATTERN.matcher(jsonOrText);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            // group(1) 是 key + 冒号及空白，group(2) 是原始值
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + "\"******\""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 对 Map 的 toString() 结果进行脱敏。
     *
     * <p>常用于对 {@code variables.toString()} 进行安全打印。
     * 由于 Map 的 toString 格式为 {@code {key1=value1, key2=value2}}，
     * 本方法先将等号转为冒号，复用 JSON 脱敏逻辑，再转回等号。</p>
     *
     * @param mapString Map 的字符串表示
     * @return 脱敏后的字符串
     */
    public static String maskMap(String mapString) {
        if (mapString == null) return "";
        String jsonLike = mapString.replace('=', ':');
        String masked = mask(jsonLike);
        return masked.replace(':', '=');
    }

    /**
     * 判断某个 key 是否为敏感字段。
     *
     * @param key 字段名（如 "password"）
     * @return true 表示该字段需要脱敏
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("passwd") || lower.contains("pwd")
                || lower.contains("token") || lower.contains("authorization") || lower.contains("auth")
                || lower.contains("secret") || lower.contains("cipher") || lower.contains("encrypteddata")
                || lower.contains("phone") || lower.contains("mobile") || lower.contains("idcard");
    }
}
