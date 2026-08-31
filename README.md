# Hello Agent Java

一个极简的 Spring Boot Starter，用于在 Java 项目中快速接入大语言模型并构建对话式 Agent。基于 [OpenAI Java SDK](https://github.com/openai/openai-java) 封装，开箱即用，几行配置即可让应用具备多轮对话能力。

## 特性

- 🚀 **开箱即用**：引入 starter 后，只需在 `application.properties` 中配置 `api-key` 和 `model`，即可注入 `Agent` Bean。
- 💬 **多轮对话**：`BaseAgent` 内置 `MessageHistoryManager`，自动维护会话上下文，无需手动拼装历史消息。
- 🛠️ **普通 Java 工具**：调用方自行创建 `ToolManager` 并注册带 `@ToolDescription` 的方法；框架负责参数 Schema 生成、类型转换和调用。
- 🧩 **易于扩展**：提供 `Agent` 接口与 `AbstractAgent` 抽象类，可方便地派生出具备工具调用、RAG 等能力的自定义 Agent。
- 🔌 **兼容 OpenAI 协议**：底层使用 `OpenAIOkHttpClient`，支持任意兼容 OpenAI 接口的服务（OpenAI、Azure、本地部署等），通过 `base-url` 切换即可。

## 模块结构

```
hello-agent-java
├── simple-agent-spring-boot-starter   # Starter 核心模块
│   ├── agent        # Agent 接口、AbstractAgent、BaseAgent 实现
│   ├── message      # Message、MessageRoleEnum、MessageHistoryManager
│   ├── tool         # ToolManager、工具注解与工具定义
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
simple.agent.openai.api-key=sk-xxx
simple.agent.openai.base-url=https://api.openai.com/v1
simple.agent.openai.model=gpt-4o-mini
```

### 3. 使用 Agent

```java
@Resource
private Agent agent;

public void demo() {
    ToolManager toolManager = new ToolManager();
    agent.setToolManager(toolManager);

    String reply = agent.chat("你好，介绍一下自己");
    System.out.println(reply);
}
```

`ToolManager` 由调用方创建并绑定给 `Agent`。即使暂时不使用工具，也应传入一个空的 `ToolManager`。

### 4. 注册工具

工具就是普通 Java 对象。`ToolManager` 会扫描其中带 `@ToolDescription` 的 public 方法，并将方法名作为工具名：

```java
import com.example.agent.tool.anno.ToolDescription;
import com.example.agent.tool.ToolManager;

public class CommonTool {

    @ToolDescription("计算两个整数之和；入参为 a、b，出参为计算结果")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
}
```

调用方负责注册并绑定工具：

```java
ToolManager toolManager = new ToolManager();
toolManager.register(new CommonTool());
agent.setToolManager(toolManager);

String reply = agent.chat("请调用 add 工具计算 12 + 30，只返回计算结果。");
System.out.println(reply);
```

框架会从方法参数生成基础 JSON Schema，并在调用时将模型参数转换成声明的 Java 类型；业务方法无需处理 `Map<String, Object>`。

如果工具对象希望由 Spring 管理，可添加 `@Tool`。它等价于 Spring 的 `@Component`，只负责创建 Bean，不会自动注册到 `ToolManager`：

```java
@Tool
public class CommonTool {
    @ToolDescription("计算两个整数之和；入参为 a、b，出参为计算结果")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
}
```

如需设置系统提示词，可通过 `ChatOptions` 传入：

```java
ChatOptions options = new ChatOptions();
options.setSystemPrompt("你是一个专业的 Java 导师。");
String reply = agent.chat("如何理解依赖注入？", options);
```

## 核心概念

| 类 | 职责 |
| --- | --- |
| `Agent` | 顶层接口，定义 `chat(input)` 与 `chat(input, config)` |
| `AbstractAgent` | 封装 OpenAI 调用细节，处理消息角色映射 |
| `BaseAgent` | 默认实现，自动管理对话历史并在调用失败时回滚 |
| `Message` | 消息实体，包含内容、角色、时间戳与扩展元数据 |
| `MessageHistoryManager` | 线程安全的对话历史管理器 |
| `ToolDefinition` | 一个工具的模型定义和实际调用目标 |
| `ToolManager` | 手动注册工具、提供工具定义并执行模型请求 |

## 环境要求

- Java 8+
- Spring Boot 2.7.x
- OpenAI Java SDK 4.52.0

## License

MIT
