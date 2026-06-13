package com.example.demo.controller;

import com.example.demo.model.CrawlerCurlRequest;
import com.example.demo.model.CrawlerRequest;
import com.example.demo.model.CrawlerResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * 图片爬虫控制器。
 * <p>
 * 提供图片爬取与自动下载的 REST API。支持 Cookie、Authorization Header、Basic Auth 三种认证方式，
 * 可通过 CSS 选择器和正则表达式精确筛选目标图片。
 * </p>
 */
@RestController
@RequestMapping("/api/crawler")
@CrossOrigin(origins = "*")
public class CrawlerController {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(CrawlerController.class);

    /** 图片文件保存的根目录（相对项目根目录） */
    private static final String CRAWLER_DIR = "uploads/crawler/";

    /** 时间戳格式，用于生成下载目录 */
    private static final DateTimeFormatter DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** 默认请求超时时间（毫秒） */
    private static final int TIMEOUT_MS = 30000;

    /** 通用浏览器 User-Agent */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** 调用日志列表，最多保留最近 20 条 */
    private static final List<Map<String, Object>> CALL_LOGS = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG_SIZE = 20;

    /**
     * 执行图片爬取与下载。
     *
     * @param request 爬虫请求参数
     * @return 下载结果
     */
    @PostMapping("/download-images")
    public ResponseEntity<CrawlerResult> downloadImages(@Valid @RequestBody CrawlerRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        long startTime = System.currentTimeMillis();

        String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        logger.info("[{}] ==========================================", taskId);
        logger.info("[{}] 开始图片爬虫任务", taskId);
        String responseType = request.getResponseType() != null ? request.getResponseType().toLowerCase() : "";
        logger.info("[{}] 完整请求参数: targetUrl={}, method={}, responseType={}, selector={}, urlPattern={}, maxCount={}, authType={}, contentType={}, userAgent={}, clientId={}, clientIdHeader={}, jsonPath={}, jsonImageUrlField={}, headers={}, body={}",
                taskId,
                request.getTargetUrl(),
                method,
                responseType.isEmpty() ? "auto" : responseType,
                request.getSelector(),
                request.getUrlPattern(),
                request.getMaxCount(),
                resolveAuthType(request),
                request.getContentType(),
                request.getUserAgent(),
                request.getClientId(),
                request.getClientIdHeader(),
                request.getJsonPath(),
                request.getJsonImageUrlField(),
                request.getHeaders(),
                request.getBody());

        CrawlerResult result = new CrawlerResult();
        String targetUrl = request.getTargetUrl().trim();

        // 参数校验
        if (!isValidUrl(targetUrl)) {
            logger.warn("[{}] 爬虫任务失败，URL 格式不合法: {}", taskId, targetUrl);
            result.setSuccess(false);
            result.setMessage("URL 格式不合法");
            saveCallLog(taskId, startTime, request, result);
            return ResponseEntity.badRequest().body(result);
        }

        // 构建 Jsoup 连接
        String ua = request.getUserAgent() != null && !request.getUserAgent().isBlank()
                ? request.getUserAgent() : USER_AGENT;
        org.jsoup.Connection connection = Jsoup.connect(targetUrl)
                .userAgent(ua)
                .timeout(TIMEOUT_MS)
                .referrer(targetUrl)
                .followRedirects(true)
                .ignoreContentType(true);

        // 设置自定义 Headers
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    connection.header(entry.getKey(), entry.getValue());
                }
            }
        }

        // ClientId 自动作为自定义请求头（Header 名可指定，默认 X-Client-Id）
        if (request.getClientId() != null && !request.getClientId().isBlank()) {
            String headerName = request.getClientIdHeader() != null && !request.getClientIdHeader().isBlank()
                    ? request.getClientIdHeader().trim() : "X-Client-Id";
            connection.header(headerName, request.getClientId().trim());
            logger.info("[{}] 添加 ClientId Header: {} = {}", taskId, headerName, request.getClientId().trim());
        }

        // 认证方式优先级：authorization > cookies > username/password (Basic Auth)
        applyAuthentication(connection, request, targetUrl);

        // POST 时设置 Content-Type 和 Body
        if ("POST".equals(method)) {
            connection.method(org.jsoup.Connection.Method.POST);
            if (request.getContentType() != null && !request.getContentType().isBlank()) {
                connection.header("Content-Type", request.getContentType());
            } else {
                connection.header("Content-Type", "application/json");
            }
            if (request.getBody() != null && !request.getBody().isBlank()) {
                connection.requestBody(request.getBody());
            }
        }

        org.jsoup.Connection.Response response;
        try {
            if ("POST".equals(method)) {
                response = connection.method(org.jsoup.Connection.Method.POST).execute();
            } else {
                response = connection.method(org.jsoup.Connection.Method.GET).execute();
            }
            logger.info("[{}] 页面获取成功，HTTP 状态码: {}，内容类型: {}",
                    taskId, response.statusCode(), response.contentType());

            // 如果未指定响应类型，根据 Content-Type 自动判断
            if (responseType.isEmpty()) {
                String contentType = response.contentType();
                if (contentType != null && (contentType.contains("application/json") || contentType.contains("text/json") || contentType.contains("+json"))) {
                    responseType = "json";
                    logger.info("[{}] 根据 Content-Type 自动切换到 JSON 模式: {}", taskId, contentType);
                } else {
                    responseType = "html";
                    logger.info("[{}] 根据 Content-Type 使用 HTML 模式: {}", taskId, contentType);
                }
            }
        } catch (IOException e) {
            logger.error("[{}] 页面获取失败: {}", taskId, e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("页面获取失败: " + e.getMessage());
            saveCallLog(taskId, startTime, request, result);
            return ResponseEntity.status(500).body(result);
        }

        // 提取图片 URL
        Set<String> imageUrls;
        Map<String, String> customNames = new HashMap<>();
        if ("json".equals(responseType)) {
            String body = response.body();
            result.setResponseBody(body);
            logger.info("[{}] JSON 响应体前 2000 字符: {}", taskId,
                    body.length() > 2000 ? body.substring(0, 2000) : body);
            imageUrls = extractImageUrlsFromJson(body, request, taskId, customNames);
        } else {
            Document document;
            try {
                document = response.parse();
            } catch (IOException e) {
                logger.error("[{}] HTML 解析失败: {}", taskId, e.getMessage(), e);
                result.setSuccess(false);
                result.setMessage("HTML 解析失败: " + e.getMessage());
                saveCallLog(taskId, startTime, request, result);
                return ResponseEntity.status(500).body(result);
            }
            String html = document.outerHtml();
            result.setResponseBody(html);
            if (html.length() > 3000) {
                logger.info("[{}] 页面返回 HTML 前 3000 字符: {}", taskId, html.substring(0, 3000));
            } else {
                logger.info("[{}] 页面返回完整 HTML: {}", taskId, html);
            }
            imageUrls = extractImageUrls(document, request, taskId);
        }

        if (imageUrls.isEmpty()) {
            logger.info("[{}] 未匹配到符合规则的图片，目标 URL: {}, selector: {}, urlPattern: {}, jsonPath: {}",
                    taskId, targetUrl, request.getSelector(), request.getUrlPattern(), request.getJsonPath());
            result.setSuccess(true);
            result.setMessage("未匹配到符合规则的图片");
            saveCallLog(taskId, startTime, request, result);
            return ResponseEntity.ok(result);
        }

        logger.info("[{}] 共匹配到 {} 张符合规则的图片，URL 列表: {}", taskId, imageUrls.size(), imageUrls);
        if (!customNames.isEmpty()) {
            logger.info("[{}] 自定义名称映射: {}", taskId, customNames);
        }

        // 创建保存目录
        String timestamp = LocalDateTime.now().format(DIR_FORMATTER);
        Path saveDir = Paths.get(CRAWLER_DIR, timestamp);
        try {
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
                logger.info("[{}] 创建下载目录: {}", taskId, saveDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("[{}] 创建下载目录失败: {}", taskId, e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("创建下载目录失败: " + e.getMessage());
            saveCallLog(taskId, startTime, request, result);
            return ResponseEntity.status(500).body(result);
        }

        // 逐一下载图片
        int maxCount = request.getMaxCount() != null && request.getMaxCount() > 0 ? request.getMaxCount() : 100;
        int count = 0;
        for (String imageUrl : imageUrls) {
            if (count >= maxCount) {
                logger.info("已达到最大下载数量限制: {}", maxCount);
                break;
            }
            boolean ok = downloadImage(imageUrl, saveDir, targetUrl, request, result, customNames);
            if (ok) {
                count++;
            }
        }

        String relativePath = "/uploads/crawler/" + timestamp + "/";
        result.setSuccess(true);
        result.setSavePath(relativePath);
        result.setMessage(String.format("下载完成，成功 %d 张，失败 %d 张",
                result.getDownloadedCount(), result.getFailedCount()));

        long cost = System.currentTimeMillis() - startTime;
        logger.info("[{}] 爬虫任务完成，耗时 {}ms，成功: {} 张，失败: {} 张，保存路径: {}",
                taskId, cost, result.getDownloadedCount(), result.getFailedCount(), relativePath);
        logger.info("[{}] 返回结果: success={}, message=\"{}\", downloadedCount={}, failedCount={}, savePath=\"{}\"",
                taskId, result.isSuccess(), result.getMessage(), result.getDownloadedCount(),
                result.getFailedCount(), result.getSavePath());
        if (!result.getFailedImages().isEmpty()) {
            logger.info("[{}] 下载失败的图片: {}", taskId, result.getFailedImages());
        }
        saveCallLog(taskId, startTime, request, result);
        return ResponseEntity.ok(result);
    }

    /**
     * 从原始 cURL 命令执行图片爬取。
     * <p>
     * 解析 "Copy as cURL (bash)" 文本，自动提取 URL、Method、Headers、Body、Cookie、Auth 等参数，
     * 复用 {@link #downloadImages} 的下载逻辑。
     * </p>
     *
     * @param curlRequest 包含 rawCurl 文本和可选的图片提取参数
     * @return 下载结果
     */
    @PostMapping("/download-images-from-curl")
    public ResponseEntity<CrawlerResult> downloadImagesFromCurl(@RequestBody CrawlerCurlRequest curlRequest) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        logger.info("[{}] 收到 cURL 模式爬虫请求", taskId);

        CrawlerResult result = new CrawlerResult();

        if (curlRequest == null || curlRequest.getRawCurl() == null || curlRequest.getRawCurl().isBlank()) {
            result.setSuccess(false);
            result.setMessage("cURL 命令不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        // 解析 cURL 命令
        CrawlerRequest request = parseCurlToRequest(curlRequest.getRawCurl(), taskId);
        if (request == null) {
            result.setSuccess(false);
            result.setMessage("无法解析 cURL 命令，请检查格式");
            return ResponseEntity.badRequest().body(result);
        }

        // 用前端传来的可选参数覆盖解析结果
        if (curlRequest.getSelector() != null) {
            request.setSelector(curlRequest.getSelector());
        }
        if (curlRequest.getUrlPattern() != null) {
            request.setUrlPattern(curlRequest.getUrlPattern());
        }
        if (curlRequest.getMaxCount() != null) {
            request.setMaxCount(curlRequest.getMaxCount());
        }
        if (curlRequest.getResponseType() != null) {
            request.setResponseType(curlRequest.getResponseType());
        }
        if (curlRequest.getJsonPath() != null) {
            request.setJsonPath(curlRequest.getJsonPath());
        }
        if (curlRequest.getJsonImageUrlField() != null) {
            request.setJsonImageUrlField(curlRequest.getJsonImageUrlField());
        }

        logger.info("[{}] cURL 解析成功: targetUrl={}, method={}, headers={}, body={}",
                taskId, request.getTargetUrl(), request.getMethod(),
                request.getHeaders() != null ? request.getHeaders().keySet() : "null",
                request.getBody() != null ? request.getBody().substring(0, Math.min(200, request.getBody().length())) : "null");

        // 复用现有的下载逻辑
        return downloadImages(request);
    }

    /**
     * 应用认证信息到 Jsoup 连接。
     */
    private void applyAuthentication(org.jsoup.Connection connection, CrawlerRequest request, String targetUrl) {
        if (request.getAuthorization() != null && !request.getAuthorization().isBlank()) {
            connection.header("Authorization", request.getAuthorization().trim());
            logger.info("使用 Authorization Header 认证");
        } else if (request.getCookies() != null && !request.getCookies().isBlank()) {
            // 将 Cookie 字符串解析为键值对设置到 Jsoup
            String[] cookiePairs = request.getCookies().split(";");
            for (String pair : cookiePairs) {
                String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2) {
                    connection.cookie(kv[0].trim(), kv[1].trim());
                }
            }
            logger.info("使用 Cookie 认证");
        } else if (request.getUsername() != null && !request.getUsername().isBlank()
                && request.getPassword() != null) {
            String basic = Base64.getEncoder().encodeToString(
                    (request.getUsername() + ":" + request.getPassword()).getBytes());
            connection.header("Authorization", "Basic " + basic);
            logger.info("使用 Basic Auth 认证");
        } else {
            logger.info("无需认证（公开页面）");
        }
    }

    /**
     * 从文档中提取符合条件的图片 URL 集合。
     */
    private Set<String> extractImageUrls(Document document, CrawlerRequest request, String taskId) {
        Set<String> imageUrls = new LinkedHashSet<>();
        String selector = request.getSelector() != null && !request.getSelector().isBlank()
                ? request.getSelector() : "img";
        String urlPattern = request.getUrlPattern() != null && !request.getUrlPattern().isBlank()
                ? request.getUrlPattern() : ".*\\.(jpg|jpeg|png|gif|webp|bmp).*";

        Pattern pattern;
        try {
            pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            logger.warn("[{}] 正则表达式编译失败，使用默认规则: {}", taskId, e.getMessage());
            pattern = Pattern.compile(".*\\.(jpg|jpeg|png|gif|webp|bmp).*", Pattern.CASE_INSENSITIVE);
        }

        Elements elements = document.select(selector);
        logger.info("[{}] CSS 选择器 '{}' 匹配到 {} 个元素", taskId, selector, elements.size());

        if (elements.isEmpty()) {
            logger.info("[{}] 诊断: 选择器 '{}' 未匹配到任何元素，建议检查页面 HTML 结构", taskId, selector);
            if (selector.matches("(?i)^(json|jpeg|jpg|png|gif|webp|bmp|text|html|body|api)$")) {
                logger.warn("[{}] 提示: 选择器 '{}' 看起来不像是有效的 CSS 选择器。如果目标接口返回 JSON，请将 responseType 设为 'json'", taskId, selector);
            }
            return imageUrls;
        }

        int elementIndex = 0;
        for (Element element : elements) {
            elementIndex++;
            // 依次尝试从 src、data-src、srcset 属性中提取图片 URL
            String[] attrNames = {"src", "data-src", "data-original", "data-lazy-src", "srcset"};
            for (String attr : attrNames) {
                String value = element.attr(attr);
                if (value == null || value.isBlank()) {
                    continue;
                }
                logger.info("[{}] 元素 #{} <{} > 属性 '{}': {}", taskId, elementIndex, element.tagName(), attr, value);

                // srcset 可能包含多个 URL（如 "img1.jpg 1x, img2.jpg 2x"）
                if ("srcset".equals(attr)) {
                    for (String part : value.split(",")) {
                        String url = part.trim().split("\\s+")[0];
                        if (isValidImageUrl(url, pattern, taskId)) {
                            String absoluteUrl = resolveUrl(document.baseUri(), url, taskId);
                            if (absoluteUrl != null) {
                                imageUrls.add(absoluteUrl);
                            }
                        }
                    }
                } else {
                    if (isValidImageUrl(value, pattern, taskId)) {
                        String absoluteUrl = resolveUrl(document.baseUri(), value, taskId);
                        if (absoluteUrl != null) {
                            imageUrls.add(absoluteUrl);
                        }
                    }
                }
            }
        }

        logger.info("[{}] 最终提取到 {} 个唯一图片 URL", taskId, imageUrls.size());
        return imageUrls;
    }

    /**
     * 从 JSON 响应中提取图片 URL 集合。
     * <p>
     * 支持三种提取策略（按优先级）：
     * <ol>
     *   <li>指定 jsonPath：按路径定位数组/对象，提取指定字段</li>
     *   <li>自动探测：jsonPath 为空时自动探测常见数组字段名（rows/data/list/records/items/result）</li>
     *   <li>全局扫描：递归遍历整个 JSON 树，提取所有匹配的 URL 字符串</li>
     *   <li>正则回退：结构化提取失败时，用正则从原始 JSON 文本提取</li>
     * </ol>
     */
    /**
     * 从 JSON 响应中提取图片 URL 集合，同时可选提取自定义名称。
     *
     * @param customNames 输出参数，key=图片URL, value=自定义文件名（不含扩展名）
     */
    private Set<String> extractImageUrlsFromJson(String jsonBody, CrawlerRequest request, String taskId, Map<String, String> customNames) {
        Set<String> imageUrls = new LinkedHashSet<>();
        String jsonPath = request.getJsonPath() != null && !request.getJsonPath().isBlank()
                ? request.getJsonPath().trim() : "";
        String urlField = request.getJsonImageUrlField() != null && !request.getJsonImageUrlField().isBlank()
                ? request.getJsonImageUrlField().trim() : "";
        String urlPattern = request.getUrlPattern() != null && !request.getUrlPattern().isBlank()
                ? request.getUrlPattern() : ".*";
        String nameField1 = request.getNameField1() != null ? request.getNameField1().trim() : "";
        String nameField2 = request.getNameField2() != null ? request.getNameField2().trim() : "";

        Pattern pattern;
        try {
            pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            logger.warn("[{}] 正则表达式编译失败，使用默认规则: {}", taskId, e.getMessage());
            pattern = Pattern.compile(".*", Pattern.CASE_INSENSITIVE);
        }

        ObjectMapper mapper = new ObjectMapper();
        Object root = null;
        try {
            root = mapper.readValue(jsonBody, new TypeReference<Object>() {});
            logger.info("[{}] JSON 根类型: {}", taskId, root != null ? root.getClass().getSimpleName() : "null");
        } catch (Exception e) {
            logger.error("[{}] JSON 解析失败，尝试正则提取: {}", taskId, e.getMessage());
            imageUrls.addAll(extractUrlsByRegex(jsonBody, pattern, taskId));
            return imageUrls;
        }

        List<?> targetList = null;

        if (!jsonPath.isEmpty()) {
            // 策略 1：按指定 jsonPath 定位
            Object current = root;
            String[] parts = jsonPath.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.isEmpty() || current == null) {
                    current = null;
                    break;
                }
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    logger.warn("[{}] JSON 路径 '{}' 解析失败，当前节点不是对象: {}", taskId, part, current.getClass().getSimpleName());
                    current = null;
                    break;
                }
            }
            if (current instanceof List) {
                targetList = (List<?>) current;
            } else if (current instanceof Map) {
                targetList = Collections.singletonList(current);
            }
        } else {
            // 策略 2：jsonPath 为空，先尝试根是否为数组
            if (root instanceof List) {
                targetList = (List<?>) root;
                logger.info("[{}] JSON 根即为数组，元素数: {}", taskId, targetList.size());
            } else if (root instanceof Map) {
                // 自动探测常见数组字段名
                Map<?, ?> rootMap = (Map<?, ?>) root;
                String[] commonArrayFields = {"rows", "data", "list", "records", "items", "result", "content", "results"};
                for (String field : commonArrayFields) {
                    Object val = rootMap.get(field);
                    if (val instanceof List) {
                        targetList = (List<?>) val;
                        logger.info("[{}] 自动探测到数组字段: '{}'，元素数: {}", taskId, field, targetList.size());
                        break;
                    }
                }
            }
        }

        // 策略 3：从定位到的列表中提取 URL 和自定义名称
        if (targetList != null && !targetList.isEmpty()) {
            logger.info("[{}] 从 {} 个元素中提取图片字段 '{}'，命名字段: [{} + {}]",
                    taskId, targetList.size(), urlField.isEmpty() ? "(自动探测)" : urlField,
                    nameField1.isEmpty() ? "(无)" : nameField1,
                    nameField2.isEmpty() ? "(无)" : nameField2);
            for (Object item : targetList) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> itemMap = (Map<?, ?>) item;
                Object urlObj = null;

                if (!urlField.isEmpty()) {
                    urlObj = itemMap.get(urlField);
                }
                // 如果指定字段不存在或为空，尝试常见字段名
                if (urlObj == null) {
                    String[] commonUrlFields = {"url", "imageUrl", "imgUrl", "src", "image", "picture", "avatar", "photo", "fileUrl", "downloadUrl", "path", "filePath"};
                    for (String f : commonUrlFields) {
                        Object val = itemMap.get(f);
                        if (val != null) {
                            urlObj = val;
                            logger.debug("[{}] 使用备用字段名 '{}' 提取到值", taskId, f);
                            break;
                        }
                    }
                }

                if (urlObj != null) {
                    String url = urlObj.toString().trim();
                    if (!url.isBlank() && isValidUrlString(url) && pattern.matcher(url).matches()) {
                        imageUrls.add(url);
                        logger.info("[{}] 从 JSON 提取到 URL: {}", taskId, url);

                        // 提取自定义名称
                        if (customNames != null && (!nameField1.isEmpty() || !nameField2.isEmpty())) {
                            StringBuilder customName = new StringBuilder();
                            if (!nameField1.isEmpty()) {
                                Object v1 = itemMap.get(nameField1);
                                if (v1 != null) customName.append(sanitizeFileName(v1.toString()));
                            }
                            if (!nameField2.isEmpty()) {
                                Object v2 = itemMap.get(nameField2);
                                if (v2 != null) {
                                    if (customName.length() > 0) customName.append("_");
                                    customName.append(sanitizeFileName(v2.toString()));
                                }
                            }
                            if (customName.length() > 0) {
                                customNames.put(url, customName.toString());
                            }
                        }
                    } else if (!url.isBlank()) {
                        logger.debug("[{}] 过滤 URL: {}，正则不匹配或不是合法 URL", taskId, url);
                    }
                }
            }
        }

        // 策略 4：如果结构化提取没找到，递归扫描整个 JSON 树
        if (imageUrls.isEmpty()) {
            logger.info("[{}] 结构化提取未找到 URL，开始全局递归扫描 JSON 树", taskId);
            Set<String> scannedUrls = new LinkedHashSet<>();
            scanJsonForUrls(root, pattern, scannedUrls, taskId);
            imageUrls.addAll(scannedUrls);
        }

        // 策略 5：如果仍未找到，用正则从原始 JSON 文本提取
        if (imageUrls.isEmpty()) {
            logger.info("[{}] 全局扫描仍未找到 URL，尝试从原始 JSON 文本正则提取", taskId);
            imageUrls.addAll(extractUrlsByRegex(jsonBody, pattern, taskId));
        }

        logger.info("[{}] JSON 解析最终提取到 {} 个唯一图片 URL", taskId, imageUrls.size());
        return imageUrls;
    }

    /**
     * 递归扫描 JSON 对象/数组/字符串，提取匹配正则的 URL。
     */
    private void scanJsonForUrls(Object node, Pattern pattern, Set<String> results, String taskId) {
        if (node == null) return;
        if (node instanceof String) {
            String s = ((String) node).trim();
            if (!s.isBlank() && isValidUrlString(s) && pattern.matcher(s).matches()) {
                results.add(s);
            }
        } else if (node instanceof List) {
            for (Object item : (List<?>) node) {
                scanJsonForUrls(item, pattern, results, taskId);
            }
        } else if (node instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
                scanJsonForUrls(entry.getValue(), pattern, results, taskId);
            }
        }
    }

    /**
     * 从原始文本中用正则提取 URL（支持捕获组）。
     */
    private Set<String> extractUrlsByRegex(String text, Pattern pattern, String taskId) {
        Set<String> results = new LinkedHashSet<>();
        try {
            java.util.regex.Matcher m = pattern.matcher(text);
            while (m.find()) {
                String url;
                if (m.groupCount() >= 1) {
                    url = m.group(1);
                } else {
                    url = m.group();
                }
                if (url == null) continue;
                url = url.trim();
                if (!url.isBlank() && isValidUrlString(url)) {
                    results.add(url);
                }
            }
        } catch (Exception e) {
            logger.warn("[{}] 正则提取失败: {}", taskId, e.getMessage());
        }
        logger.info("[{}] 正则提取到 {} 个 URL", taskId, results.size());
        return results;
    }

    /**
     * 判断字符串是否为合法的网络 URL（http/https 开头）。
     */
    private boolean isValidUrlString(String url) {
        return url != null && !url.isBlank()
                && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * 判断 URL 是否符合图片规则。
     */
    private boolean isValidImageUrl(String url, Pattern pattern, String taskId) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        // 排除 javascript: 和 data: 等伪协议
        if (trimmed.startsWith("javascript:") || trimmed.startsWith("data:")) {
            logger.info("[{}] 过滤 URL（伪协议）: {}", taskId, trimmed);
            return false;
        }
        // 排除 # 锚点
        if (trimmed.startsWith("#")) {
            logger.info("[{}] 过滤 URL（锚点）: {}", taskId, trimmed);
            return false;
        }
        boolean matches = pattern.matcher(trimmed).matches();
        if (!matches) {
            logger.info("[{}] 过滤 URL（正则不匹配）: {}，当前正则: {}", taskId, trimmed, pattern.pattern());
        }
        return matches;
    }

    /**
     * 将相对 URL 解析为绝对 URL。
     */
    private String resolveUrl(String baseUri, String url, String taskId) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        try {
            URI base = URI.create(baseUri);
            // 处理 //example.com/img.jpg 协议相对 URL
            if (trimmed.startsWith("//")) {
                String resolved = base.getScheme() + ":" + trimmed;
                logger.info("[{}] URL 协议相对补全: {} -> {}", taskId, trimmed, resolved);
                return resolved;
            }
            // 处理 /img.jpg 根相对 URL
            if (trimmed.startsWith("/")) {
                String resolved = base.getScheme() + "://" + base.getAuthority() + trimmed;
                logger.info("[{}] URL 根相对补全: {} -> {}", taskId, trimmed, resolved);
                return resolved;
            }
            // 处理相对路径 img.jpg
            String basePath = base.getPath();
            if (basePath == null || basePath.isEmpty()) {
                basePath = "/";
            }
            // 去掉 basePath 最后一部分（文件名）
            int lastSlash = basePath.lastIndexOf('/');
            String dir = lastSlash >= 0 ? basePath.substring(0, lastSlash + 1) : "/";
            String resolved = base.getScheme() + "://" + base.getAuthority() + dir + trimmed;
            logger.info("[{}] URL 相对路径补全: {} -> {}", taskId, trimmed, resolved);
            return resolved;
        } catch (Exception e) {
            logger.warn("[{}] URL 解析失败，base: {}, url: {}", taskId, baseUri, trimmed);
            return null;
        }
    }

    /**
     * 下载单张图片。
     *
     * @param imageUrl     图片 URL
     * @param saveDir      保存目录
     * @param referer      来源页 URL（用于防盗链）
     * @param request      原始请求（用于复用认证信息）
     * @param result       结果收集器
     * @param customNames  自定义文件名映射（key=URL, value=文件名前缀）
     * @return 是否下载成功
     */
    private boolean downloadImage(String imageUrl, Path saveDir, String referer,
                                   CrawlerRequest request, CrawlerResult result,
                                   Map<String, String> customNames) {
        try {
            URL url = new URL(imageUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Referer", referer);

            // 复用认证信息（仅当图片与目标页同域时）
            if (request.getAuthorization() != null && !request.getAuthorization().isBlank()) {
                conn.setRequestProperty("Authorization", request.getAuthorization().trim());
            }

            // 生成文件名（优先使用自定义名称）
            String customName = customNames != null ? customNames.get(imageUrl) : null;
            String fileName = generateFileName(imageUrl, customName);
            Path targetPath = saveDir.resolve(fileName);

            // 处理文件名冲突
            int counter = 1;
            while (Files.exists(targetPath)) {
                String name = fileName;
                String ext = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    name = fileName.substring(0, dotIndex);
                    ext = fileName.substring(dotIndex);
                }
                targetPath = saveDir.resolve(name + "_" + counter + ext);
                counter++;
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("图片下载成功: {} -> {}", imageUrl, targetPath.getFileName());
            result.addImage(imageUrl);
            return true;
        } catch (Exception e) {
            logger.warn("图片下载失败: {}，原因: {}", imageUrl, e.getMessage());
            result.addFailedImage(imageUrl);
            return false;
        }
    }

    /**
     * 根据图片 URL 和可选的自定义名称生成文件名。
     *
     * @param imageUrl   图片 URL（用于提取扩展名，作为回退文件名）
     * @param customName 自定义名称前缀（可为 null，为 null 时从 URL 提取文件名）
     * @return 生成的文件名
     */
    private String generateFileName(String imageUrl, String customName) {
        try {
            // 从 URL 中提取扩展名
            String urlPath = new URL(imageUrl).getPath();
            String urlFileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
            if (urlFileName.contains("?")) {
                urlFileName = urlFileName.substring(0, urlFileName.indexOf('?'));
            }
            String ext = "";
            int dot = urlFileName.lastIndexOf('.');
            if (dot > 0) {
                ext = urlFileName.substring(dot);
            }
            if (ext.isBlank()) {
                ext = ".jpg";
            }

            String fileName;
            if (customName != null && !customName.isBlank()) {
                // 使用自定义名称 + URL 中的扩展名
                fileName = customName + ext;
            } else {
                // 回退：从 URL 提取文件名
                fileName = urlFileName.isBlank() ? "image" + ext : urlFileName;
            }

            // 限制文件名长度（预留序号空间 _999）
            if (fileName.length() > 100) {
                String namePart = ext.isBlank() ? fileName : fileName.substring(0, fileName.length() - ext.length());
                fileName = namePart.substring(0, 100 - ext.length() - 4) + ext;
            }
            return fileName;
        } catch (Exception e) {
            return "image.jpg";
        }
    }

    /**
     * 清理字符串，使其适合作为文件名。
     * 移除或替换 Windows / Linux 不允许的文件名字符。
     */
    private String sanitizeFileName(String input) {
        if (input == null) return "";
        // Windows 非法字符: \ / : * ? " < > |
        String cleaned = input.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        // 去掉首尾空格和点号（避免 . 和 .. 文件名）
        cleaned = cleaned.replaceAll("^[.\\s]+", "").replaceAll("[.\\s]+$", "");
        // 限制单段长度
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80);
        }
        return cleaned;
    }

    /**
     * 简单的 URL 格式校验。
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.trim().startsWith("http://") || url.trim().startsWith("https://");
    }

    /**
     * 查询最近调用日志。
     *
     * @return 最近 20 条调用记录（请求参数 + 返回结果 + 耗时）
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs() {
        logger.info("查询爬虫调用日志，当前共 {} 条", CALL_LOGS.size());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", CALL_LOGS.size());
        result.put("logs", CALL_LOGS);
        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查接口。
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("status", "ok", "module", "crawler"));
    }

    /**
     * 保存调用日志到内存列表。
     */
    private void saveCallLog(String taskId, long startTime, CrawlerRequest request, CrawlerResult result) {
        Map<String, Object> log = new HashMap<>();
        log.put("taskId", taskId);
        log.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.put("costMs", System.currentTimeMillis() - startTime);

        Map<String, Object> req = new HashMap<>();
        req.put("targetUrl", request.getTargetUrl());
        req.put("method", request.getMethod());
        req.put("selector", request.getSelector());
        req.put("urlPattern", request.getUrlPattern());
        req.put("maxCount", request.getMaxCount());
        req.put("authType", resolveAuthType(request));
        req.put("headers", request.getHeaders());
        req.put("body", request.getBody());
        req.put("contentType", request.getContentType());
        req.put("userAgent", request.getUserAgent());
        req.put("clientId", request.getClientId());
        req.put("clientIdHeader", request.getClientIdHeader());
        req.put("responseType", request.getResponseType());
        req.put("jsonPath", request.getJsonPath());
        req.put("jsonImageUrlField", request.getJsonImageUrlField());
        log.put("request", req);

        Map<String, Object> res = new HashMap<>();
        res.put("success", result.isSuccess());
        res.put("message", result.getMessage());
        res.put("downloadedCount", result.getDownloadedCount());
        res.put("failedCount", result.getFailedCount());
        res.put("savePath", result.getSavePath());
        res.put("images", result.getImages());
        res.put("failedImages", result.getFailedImages());
        log.put("response", res);

        CALL_LOGS.add(0, log);
        if (CALL_LOGS.size() > MAX_LOG_SIZE) {
            CALL_LOGS.remove(CALL_LOGS.size() - 1);
        }
    }

    /**
     * 解析 cURL 命令为 CrawlerRequest。
     *
     * @param rawCurl 原始 cURL 文本
     * @param taskId  任务 ID（用于日志）
     * @return 解析后的 CrawlerRequest，解析失败返回 null
     */
    private CrawlerRequest parseCurlToRequest(String rawCurl, String taskId) {
        try {
            // 1. 标准化：去掉换行符前面的反斜杠，合并多行
            String normalized = rawCurl.replaceAll("\\\s*\r?\n\\s*", " ");
            normalized = normalized.replaceAll("\\s+", " ").trim();

            CrawlerRequest req = new CrawlerRequest();
            Map<String, String> headers = new HashMap<>();
            StringBuilder cookies = new StringBuilder();

            // 2. 提取 URL：curl 后面的第一个 http:// 或 https:// 开头的 token
            // 先去掉开头的 "curl" 或 "curl "
            String afterCurl = normalized;
            if (afterCurl.toLowerCase().startsWith("curl ")) {
                afterCurl = afterCurl.substring(5).trim();
            }

            // 提取 URL：找第一个以 http:// 或 https:// 开头的 token
            String url = extractQuotedToken(afterCurl);
            if (url == null || !isValidUrl(url)) {
                // 尝试从任意位置找 URL
                java.util.regex.Matcher urlMatcher = java.util.regex.Pattern.compile("(https?://[^\\s\"']+)").matcher(afterCurl);
                if (urlMatcher.find()) {
                    url = urlMatcher.group(1);
                }
            }
            if (url == null || !isValidUrl(url)) {
                logger.warn("[{}] cURL 解析失败：未找到合法的 URL", taskId);
                return null;
            }
            req.setTargetUrl(url);

            // 3. 解析参数
            // 将命令拆分为 tokens，支持单双引号
            List<String> tokens = tokenizeCurl(normalized);

            String method = "GET";
            StringBuilder bodyBuilder = new StringBuilder();
            boolean hasBody = false;

            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                String lower = token.toLowerCase();

                if (lower.equals("-x") || lower.equals("--request")) {
                    if (i + 1 < tokens.size()) {
                        method = tokens.get(++i).toUpperCase();
                    }
                } else if (lower.equals("-h") || lower.equals("--header")) {
                    if (i + 1 < tokens.size()) {
                        String headerLine = tokens.get(++i);
                        int colonIdx = headerLine.indexOf(':');
                        if (colonIdx > 0) {
                            String key = headerLine.substring(0, colonIdx).trim();
                            String value = headerLine.substring(colonIdx + 1).trim();
                            if ("cookie".equalsIgnoreCase(key)) {
                                if (cookies.length() > 0) cookies.append("; ");
                                cookies.append(value);
                            } else if ("authorization".equalsIgnoreCase(key)) {
                                req.setAuthorization(value);
                            } else if ("user-agent".equalsIgnoreCase(key)) {
                                req.setUserAgent(value);
                            } else if ("content-type".equalsIgnoreCase(key)) {
                                req.setContentType(value);
                            } else {
                                headers.put(key, value);
                            }
                        }
                    }
                } else if (lower.equals("-d") || lower.equals("--data") || lower.equals("--data-raw") || lower.equals("--data-binary")) {
                    if (i + 1 < tokens.size()) {
                        if (hasBody) bodyBuilder.append("&");
                        bodyBuilder.append(tokens.get(++i));
                        hasBody = true;
                        method = "POST"; // 有 body 默认 POST
                    }
                } else if (lower.equals("-b") || lower.equals("--cookie")) {
                    if (i + 1 < tokens.size()) {
                        if (cookies.length() > 0) cookies.append("; ");
                        cookies.append(tokens.get(++i));
                    }
                } else if (lower.equals("-u") || lower.equals("--user")) {
                    if (i + 1 < tokens.size()) {
                        String cred = tokens.get(++i);
                        int colonIdx = cred.indexOf(':');
                        if (colonIdx > 0) {
                            req.setUsername(cred.substring(0, colonIdx));
                            req.setPassword(cred.substring(colonIdx + 1));
                        } else {
                            req.setUsername(cred);
                            req.setPassword("");
                        }
                    }
                }
            }

            req.setMethod(method);
            if (hasBody) {
                req.setBody(bodyBuilder.toString());
            }
            if (cookies.length() > 0) {
                req.setCookies(cookies.toString());
            }
            if (!headers.isEmpty()) {
                req.setHeaders(headers);
            }

            logger.info("[{}] cURL 解析结果: url={}, method={}, auth={}, headers={}, hasBody={}",
                    taskId, req.getTargetUrl(), req.getMethod(),
                    resolveAuthType(req),
                    req.getHeaders() != null ? req.getHeaders().keySet() : "none",
                    req.getBody() != null);

            return req;

        } catch (Exception e) {
            logger.error("[{}] cURL 解析异常: {}", taskId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从字符串开头提取被引号包裹的 token。
     */
    private String extractQuotedToken(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        if (s.startsWith("\"") || s.startsWith("'")) {
            char quote = s.charAt(0);
            int end = -1;
            for (int i = 1; i < s.length(); i++) {
                if (s.charAt(i) == quote && s.charAt(i - 1) != '\\') {
                    end = i;
                    break;
                }
            }
            if (end > 0) {
                return s.substring(1, end);
            }
        }
        // 没有引号，取第一个空格前的 token
        int space = s.indexOf(' ');
        return space > 0 ? s.substring(0, space) : s;
    }

    /**
     * 将 cURL 命令拆分为 tokens（支持单双引号包裹的值）。
     */
    private List<String> tokenizeCurl(String cmd) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            char prev = i > 0 ? cmd.charAt(i - 1) : '\0';

            if (c == '\'' && !inDouble && prev != '\\') {
                if (inSingle) {
                    inSingle = false;
                } else {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    inSingle = true;
                }
                continue;
            }
            if (c == '"' && !inSingle && prev != '\\') {
                if (inDouble) {
                    inDouble = false;
                } else {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    inDouble = true;
                }
                continue;
            }

            if ((c == ' ' || c == '\t') && !inSingle && !inDouble) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /**
     * 解析认证方式描述。
     */
    private String resolveAuthType(CrawlerRequest request) {
        if (request.getAuthorization() != null && !request.getAuthorization().isBlank()) {
            return "authorization";
        } else if (request.getCookies() != null && !request.getCookies().isBlank()) {
            return "cookie";
        } else if (request.getUsername() != null && !request.getUsername().isBlank()
                && request.getPassword() != null) {
            return "basic";
        }
        return "none";
    }
}
