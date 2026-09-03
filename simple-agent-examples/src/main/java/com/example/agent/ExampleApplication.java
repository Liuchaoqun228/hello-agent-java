package com.example.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统一启动对话、工具和记忆示例所需的 Spring 容器。
 */
@SpringBootApplication
public class ExampleApplication {

    /**
     * 启动示例应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
