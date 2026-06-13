package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类。
 * <p>
 * Spring Boot 应用的入口类，负责启动整个应用容器。
 * {@link SpringBootApplication} 是组合注解，等价于同时标注：
 * <ul>
 *   <li>@Configuration — 标记为配置类</li>
 *   <li>@EnableAutoConfiguration — 启用自动配置</li>
 *   <li>@ComponentScan — 自动扫描同包及子包下的 Spring 组件</li>
 * </ul>
 * </p>
 * <p>
 * 访问地址：http://zhengzhou:8081/index.html#2（开发环境内部备注）
 * </p>
 */
@SpringBootApplication
public class DemoApplication {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    /**
     * 应用主入口方法。
     *
     * @param args 命令行传入的启动参数（如 --server.port=8080）
     */
    public static void main(String[] args) {
        logger.info("开始启动应用...");
        // SpringApplication.run 负责初始化 Spring 上下文、启动内嵌 Tomcat、加载所有 Bean
        SpringApplication.run(DemoApplication.class, args);
        logger.info("应用启动完成");
    }
}
