package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 应用配置类。
 * <p>
 * 使用 {@link Configuration} 标记，Spring 会在启动时扫描并加载本类中所有带有 {@link Bean} 注解的方法，
 * 将返回的对象纳入 Spring 容器管理（单例模式）。
 * </p>
 * <p>
 * 当前配置内容：
 * <ul>
 *   <li>BCryptPasswordEncoder — 密码加密器，用于用户/学生/教师密码的哈希存储</li>
 * </ul>
 * </p>
 */
@Configuration
public class AppConfig {

    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /**
     * 密码加密器 Bean。
     * <p>
     * 使用 BCrypt 强哈希算法（自适应单向函数），特点：
     * <ul>
     *   <li>自动为每个密码生成随机盐值，相同明文加密结果不同</li>
     *   <li>内置工作因子（默认 10），可通过构造函数调整强度</li>
     *   <li>无需单独存储盐值，哈希字符串中已包含盐和工作因子信息</li>
     * </ul>
     * </p>
     *
     * @return BCryptPasswordEncoder 实例，供 Service/Controller 层注入使用
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        logger.info("初始化密码加密器: BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }
}
