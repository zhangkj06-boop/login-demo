package com.example.autoframework.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加解密工具类 —— 负责请求体的加密和响应体的解密。
 *
 * <p>在安全性要求较高的业务场景中，接口传输的数据需要加密处理：</p>
 * <ul>
 *   <li><b>请求加密</b>：防止敏感信息（密码、身份证等）在 HTTP 传输过程中被嗅探</li>
 *   <li><b>响应解密</b>：将服务端返回的密文还原为明文，以便后续做 JSON 断言</li>
 * </ul>
 *
 * <p>当前内置两种算法示例：</p>
 * <ul>
 *   <li><b>AES 对称加密</b>：加解密使用同一个密钥，适合内部系统对接，速度快</li>
 *   <li><b>RSA 非对称加密</b>：公钥加密、私钥解密，适合跨网络的高安全场景</li>
 * </ul>
 *
 * <p><b>重要提示</b>：示例中的 AES_KEY 为硬编码，仅用于演示。
 * 生产环境请通过 JVM 参数（{@code -Dapi.aesKey=xxx}）或配置中心注入密钥，
 * 切勿将真实密钥提交到代码仓库。</p>
 *
 * @see ApiEngine#execute(com.example.autoframework.pojo.TestCase)
 */
public class CryptoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoUtils.class);

    /** Jackson 对象映射器，用于解析封装型密文 JSON（如 {"encryptedData":"密文"}） */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * AES 密钥，长度必须为 16/24/32 字节（对应 AES-128/192/256）。
     * 当前值 "AutoFrameWork2024" 恰好 16 字节，满足 AES-128 要求。
     */
    private static final String AES_KEY = "AutoFrameWork2024";

    // ==================== AES 对称加密 ====================

    /**
     * AES 加密（ECB 模式 + PKCS5Padding）。
     *
     * <p>ECB 模式虽然简单，但相同明文会生成相同密文，安全性不如 CBC/GCM。
     * 实际生产环境建议升级至 AES/GCM/NoPadding 并配合随机 IV 使用。
     * 此处保留 ECB 是为了降低框架上手门槛，让测试工程师无需关心 IV 传递问题。</p>
     *
     * @param plainText 明文 JSON 字符串
     * @return Base64 编码后的密文字符串
     * @throws Exception 加密过程中的异常（如密钥长度不合法）
     */
    public static String aesEncrypt(String plainText) throws Exception {
        LOG.debug("[CryptoUtils] AES 加密开始，明文长度: {} 字节", plainText != null ? plainText.length() : 0);
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        String result = Base64.getEncoder().encodeToString(encrypted);
        LOG.debug("[CryptoUtils] AES 加密完成，密文长度: {} 字节", result.length());
        return result;
    }

    /**
     * AES 解密（ECB 模式 + PKCS5Padding）。
     *
     * @param cipherText Base64 编码的密文字符串
     * @return 解密后的明文 JSON 字符串
     * @throws Exception 解密过程中的异常（如密文损坏、密钥错误）
     */
    public static String aesDecrypt(String cipherText) throws Exception {
        LOG.debug("[CryptoUtils] AES 解密开始，密文长度: {} 字节", cipherText != null ? cipherText.length() : 0);
        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(cipherText);
        String result = new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        LOG.debug("[CryptoUtils] AES 解密完成，明文长度: {} 字节", result.length());
        return result;
    }

    // ==================== RSA 非对称加密（示例） ====================

    /**
     * RSA 公钥加密。
     *
     * @param plainText        明文字符串
     * @param base64PublicKey  Base64 编码的 X.509 格式公钥
     * @return Base64 编码的密文字符串
     * @throws Exception 加密异常
     */
    public static String rsaEncrypt(String plainText, String base64PublicKey) throws Exception {
        LOG.info("[CryptoUtils] RSA 公钥加密开始...");
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pubKey = kf.generatePublic(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        String result = Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        LOG.info("[CryptoUtils] RSA 公钥加密完成，密文长度: {} 字节", result.length());
        return result;
    }

    /**
     * RSA 私钥解密。
     *
     * @param cipherText        Base64 编码的密文字符串
     * @param base64PrivateKey  Base64 编码的 PKCS#8 格式私钥
     * @return 解密后的明文字符串
     * @throws Exception 解密异常
     */
    public static String rsaDecrypt(String cipherText, String base64PrivateKey) throws Exception {
        LOG.info("[CryptoUtils] RSA 私钥解密开始...");
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey priKey = kf.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        byte[] decoded = Base64.getDecoder().decode(cipherText);
        String result = new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        LOG.info("[CryptoUtils] RSA 私钥解密完成，明文长度: {} 字节", result.length());
        return result;
    }

    // ==================== 统一入口 ====================

    /**
     * 加密请求体统一入口。
     *
     * <p>根据 mode 参数路由到对应的算法实现。若后续需要接入业务自定义算法（如 SM4、国密），
     * 只需在此 switch 中新增 case 分支即可，调用方 {@link ApiEngine} 无需修改。</p>
     *
     * @param body 明文 JSON 字符串
     * @param mode 加密模式: AES, RSA, NONE（不区分大小写）
     * @return 密文字符串
     * @throws Exception 加密失败时抛出
     */
    public static String encryptBody(String body, String mode) throws Exception {
        if (body == null || body.isEmpty()) {
            LOG.warn("[CryptoUtils] 加密输入为空，直接返回空字符串");
            return body;
        }
        LOG.info("[CryptoUtils] 加密入口: mode={}, bodyLength={}", mode, body.length());
        return switch (mode.toUpperCase()) {
            case "AES" -> aesEncrypt(body);
            case "RSA" -> throw new IllegalArgumentException(
                    "RSA 加密需要公钥，请调用 rsaEncrypt(plainText, publicKey)");
            default -> {
                LOG.warn("[CryptoUtils] 未知加密模式 '{}', 透传明文", mode);
                yield body;
            }
        };
    }

    /**
     * 解密响应体统一入口。
     *
     * @param body 密文字符串
     * @param mode 解密模式: AES, RSA, NONE
     * @return 明文 JSON 字符串
     * @throws Exception 解密失败时抛出
     */
    public static String decryptBody(String body, String mode) throws Exception {
        if (body == null || body.isEmpty()) {
            LOG.warn("[CryptoUtils] 解密输入为空，直接返回空字符串");
            return body;
        }
        LOG.info("[CryptoUtils] 解密入口: mode={}, bodyLength={}", mode, body.length());
        return switch (mode.toUpperCase()) {
            case "AES" -> aesDecrypt(body);
            case "RSA" -> throw new IllegalArgumentException(
                    "RSA 解密需要私钥，请调用 rsaDecrypt(cipherText, privateKey)");
            default -> {
                LOG.warn("[CryptoUtils] 未知解密模式 '{}', 透传密文", mode);
                yield body;
            }
        };
    }

    /**
     * 解密业务封装型密文。
     *
     * <p>某些后端接口不会直接返回裸密文，而是包装在 JSON 中：</p>
     * <pre>{"encryptedData": "Base64CipherText=="}</pre>
     * 此方法先从外层 JSON 提取密文字段，再调用底层解密算法还原明文。
     *
     * @param jsonBody   外层包装 JSON
     * @param mode       解密模式（AES / RSA）
     * @param fieldName  密文字段名（如 "encryptedData"）
     * @return 解密后的明文 JSON 字符串
     * @throws Exception 解析或解密失败时抛出
     */
    public static String decryptWrappedBody(String jsonBody, String mode, String fieldName) throws Exception {
        if (jsonBody == null || jsonBody.isEmpty()) {
            LOG.warn("[CryptoUtils] 封装型解密输入为空");
            return jsonBody;
        }
        LOG.info("[CryptoUtils] 封装型解密: fieldName='{}', mode={}", fieldName, mode);
        var node = MAPPER.readTree(jsonBody);
        if (node.has(fieldName)) {
            String cipherText = node.get(fieldName).asText();
            LOG.debug("[CryptoUtils] 从字段 '{}' 提取密文，长度: {} 字节", fieldName, cipherText.length());
            return decryptBody(cipherText, mode);
        }
        LOG.warn("[CryptoUtils] 响应 JSON 中不存在字段 '{}', 返回原始响应", fieldName);
        return jsonBody;
    }
}
