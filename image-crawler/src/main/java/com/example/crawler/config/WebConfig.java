package com.example.crawler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置类。
 * <p>
 * 实现 {@link WebMvcConfigurer} 接口，对 Spring MVC 进行自定义配置，
 * 如静态资源映射、拦截器、跨域规则、消息转换器等。
 * </p>
 * <p>
 * 当前配置内容：将本地文件系统目录映射为可访问的静态资源路径，
 * 主要用于爬虫下载图片等上传文件的对外访问。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    /**
     * 配置静态资源处理器。
     * <p>
     * 将本地 {@code uploads/} 目录映射为 HTTP 路径 {@code /uploads/**}，
     * 使得前端可通过 {@code <img src="/uploads/crawler/xxx.png">} 直接访问下载的图片文件。
     * </p>
     * <p>
     * 资源定位前缀 {@code file:} 表示从文件系统加载，而非 classpath。
     * </p>
     *
     * @param registry Spring MVC 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // uploads 目录映射
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        logger.info("配置静态资源映射: /uploads/** -> file:{}/", uploadPath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");

        // crawler 目录映射（爬虫下载的图片）
        Path crawlerDir = Paths.get("uploads/crawler");
        if (!crawlerDir.toFile().exists()) {
            crawlerDir.toFile().mkdirs();
            logger.info("创建爬虫下载目录: {}", crawlerDir.toAbsolutePath());
        }
        String crawlerPath = crawlerDir.toFile().getAbsolutePath();
        logger.info("配置静态资源映射: /crawler/** -> file:{}/", crawlerPath);
        registry.addResourceHandler("/crawler/**")
                .addResourceLocations("file:" + crawlerPath + "/");

        logger.debug("静态资源映射配置完成");
    }
}
