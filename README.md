# Hello Agent Java

一个极简的 Spring Boot Starter，用于在 Java 项目中快速接入大语言模型并构建对话式 Agent。基于 [OpenAI Java SDK](https://github.com/openai/openai-java) 封装，开箱即用，几行配置即可让应用具备多轮对话能力。

## 特性

- 🚀 **开箱即用**：引入 starter 后，只需在 `application.properties` 中配置 `api-key` 和 `model`，即可注入一个可用的 `Agent` Bean。
- 💬 **多轮对话**：`BaseAgent` 内置 `MessageHistoryManager`，自动维护会话上下文，无需手动拼装历史消息。
- 🛠️ **注解式工具**：使用 `@Tool` 和 `@ToolDescription` 声明普通 Java 方法，自动完成注册、参数 Schema 生成与类型转换。
- 🧩 **易于扩展**：提供 `Agent` 接口与 `AbstractAgent` 抽象类，可方便地派生出具备工具调用、RAG 等能力的自定义 Agent。
- 🔌 **兼容 OpenAI 协议**：底层使用 `OpenAIOkHttpClient`，支持任意兼容 OpenAI 接口的服务（OpenAI、Azure、本地部署等），通过 `base-url` 切换即可。

## 模块结构

```
hello-agent-java
├── simple-agent-spring-boot-starter   # Starter 核心模块
│   ├── agent        # Agent 接口、AbstractAgent、BaseAgent 实现
│   ├── message      # Message、MessageRoleEnum、MessageHistoryManager
│   └── autoconfigure # LLMProperties 与自动配置
└── simple-agent-starter-test          # 使用示例与集成测试
```

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.example.agent</groupId>
    <artifactId>simple-agent-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置参数

```properties
simple.llm.api-key=sk-xxx
simple.llm.base-url=https://api.openai.com/v1/
simple.llm.model=gpt-4o-mini
```

### 3. 使用 Agent

```java
@Autowired
private Agent agent;

public void demo() {
    String reply = agent.chat("你好，介绍一下自己");
    System.out.println(reply);
}
```

### 4. 声明工具

在 Spring Boot 扫描路径下添加一个工具类。`@Tool` 会将类注册为 Spring Bean，只有带 `@ToolDescription` 的 public 方法才会暴露给模型：

```java
import com.example.agent.tool.anno.Tool;
import com.example.agent.tool.anno.ToolDescription;

@Tool
public class CommonTool {

    @ToolDescription("计算两个整数之和；入参为 a、b，出参为计算结果")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
}
```

工具名使用方法名。框架会从方法参数自动生成 JSON Schema，并在调用时将模型参数转换成声明的 Java 类型，无需手工注册或处理 `Map<String, Object>`。

如需设置系统提示词，可通过 `ChatConfig` 传入：

```java
ChatConfig config = new ChatConfig();
config.setSystemPrompt("你是一个专业的 Java 导师。");
String reply = agent.chat("如何理解依赖注入？", config);
```

## 核心概念

| 类 | 职责 |
| --- | --- |
| `Agent` | 顶层接口，定义 `chat(input)` 与 `chat(input, config)` |
| `AbstractAgent` | 封装 OpenAI 调用细节，处理消息角色映射 |
| `BaseAgent` | 默认实现，自动管理对话历史并在调用失败时回滚 |
| `Message` | 消息实体，包含内容、角色、时间戳与扩展元数据 |
| `MessageHistoryManager` | 线程安全的对话历史管理器 |
| `ToolDefinition` | 框架根据工具方法生成的不可变模型定义 |
| `ToolRegistry` | 自动注册并执行注解工具方法 |

## 环境要求

- Java 8+
- Spring Boot 2.7.x
- OpenAI Java SDK 4.52.0

## License

MIT
