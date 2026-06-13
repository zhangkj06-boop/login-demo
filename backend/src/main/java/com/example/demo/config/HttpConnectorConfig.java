package com.example.demo.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP + HTTPS 双协议配置类。
 * <p>
 * 背景：application.properties 中配置了 HTTPS（端口 8443，自签名证书），
 * 但前端开发或某些内网场景仍需通过 HTTP（端口 8080）访问。
 * 本类通过向 Tomcat 添加额外的 HTTP Connector，实现双协议并存。
 * </p>
 * <p>
 * 注意：生产环境建议仅保留 HTTPS，或配置 HTTP 自动重定向到 HTTPS。
 * </p>
 */
@Configuration
public class HttpConnectorConfig {

    /**
     * 配置额外的 HTTP Connector。
     * <p>
     * 返回的 WebServerFactoryCustomizer 会在 Tomcat 启动时被执行，
     * 向 Tomcat 实例追加一个监听 8080 端口的 HTTP Connector。
     * </p>
     *
     * @return 自定义 Tomcat WebServer 工厂配置器
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return factory -> factory.addAdditionalTomcatConnectors(createHttpConnector());
    }

    /**
     * 创建 HTTP Connector。
     * <p>
     * 协议：HTTP/1.1（NIO 实现）<br>
     * 端口：8080<br>
     * secure：false（非安全连接）
     * </p>
     *
     * @return Tomcat Connector 实例
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        return connector;
    }
}
