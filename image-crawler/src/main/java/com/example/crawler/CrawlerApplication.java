package com.example.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 图片爬虫独立应用启动类。
 * <p>
 * 无需登录、无需数据库，启动后访问 http://localhost:8080/
 * </p>
 */
@SpringBootApplication
public class CrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlerApplication.class, args);
        System.out.println("图片爬虫已启动: http://localhost:8080/");
    }
}
