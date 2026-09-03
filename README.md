# Hello Agent Java

一个极简的 Spring Boot Starter，用于在 Java 项目中快速接入大语言模型并构建对话式 Agent。基于 [OpenAI Java SDK](https://github.com/openai/openai-java) 封装，开箱即用，几行配置即可让应用具备多轮对话能力。

## 特性

- 🚀 **开箱即用**：引入 starter 后，只需在 `application.properties` 中配置 `api-key` 和 `model`，即可注入 `Agent` Bean。
- 💬 **多轮对话**：`BaseAgent` 内置 `MessageHistoryManager`，自动维护会话上下文，无需手动拼装历史消息。
- 🛠️ **普通 Java 工具**：调用方自行创建 `ToolManager` 并注册带 `@ToolDescription` 的方法；框架负责参数 Schema 生成、类型转换和调用。
- 🔄 **ReAct 式工具循环**：`BaseAgent` 基于 OpenAI 原生 Tool Calling 完成“决策—行动—观察—继续决策”循环，直到生成最终答案。
- 🧠 **记忆系统**：`MemoryStrategy` 接口 + JSON 文件默认实现，按用户隔离、持久化存储；注册 `MemoryTool` 即启用，由大模型自主决定何时记住与回忆。
- 🧩 **易于扩展**：提供 `Agent` 接口与 `AbstractAgent` 抽象类，可方便地派生出具备工具调用、RAG 等能力的自定义 Agent。
- 🔌 **兼容 OpenAI 协议**：底层使用 `OpenAIOkHttpClient`，支持任意兼容 OpenAI 接口的服务（OpenAI、Azure、本地部署等），通过 `base-url` 切换即可。

## 模块结构

```
hello-agent-java
├── simple-agent-spring-boot-starter   # Starter 核心模块
│   ├── agent        # Agent 接口、AbstractAgent、BaseAgent 实现
│   ├── message      # Message、MessageRoleEnum、MessageHistoryManager
│   ├── memory       # MemoryStrategy、JSON 默认实现、MemoryItem、MemoryTool
│   ├── tool         # ToolManager、工具注解与工具定义
│   └── autoconfigure # LLMProperties、MemoryProperties 与自动配置
└── simple-agent-examples              # 可直接运行的使用示例
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

`ToolManager` 由调用方创建并绑定给 `Agent`。不使用工具时可以不设置 `ToolManager`。

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

### 5. 启用记忆

starter 自动装配了 `MemoryTool`（可直接 `@Resource` 注入）。**是否启用记忆由调用方决定：把 `MemoryTool` 注册进 `ToolManager` 即启用，不注册则不生效**。注册后大模型会自主决定何时记住信息、何时查询记忆：

```java
import com.example.agent.memory.MemoryTool;

@Resource
private Agent agent;
@Resource
private MemoryTool memoryTool;

public void demo() {
    ToolManager toolManager = new ToolManager();
    toolManager.register(memoryTool); // 注册即启用记忆
    agent.setToolManager(toolManager);

    ChatOptions options = new ChatOptions();
    options.setUserId("user123");
    memoryTool.setUserId(options.getUserId()); // 绑定当前记忆用户

    agent.chat("我叫张三，是一名 Java 开发者。", options);
    String reply = agent.chat("我叫什么名字？我是做什么工作的？", options);
    System.out.println(reply);
}
```

`MemoryTool` 向模型暴露四个操作：`addMemory`（记住信息，可附重要性 0-1）、`searchMemory`（按关键词搜索）、`listMemory`（列出全部记忆）、`updateMemory`（按 id 更新）。

默认的 `JsonMemoryStrategyStrategyImpl` 将记忆以 JSON 文件形式保存在数据目录，每个用户一个文件，可通过配置调整位置：

```properties
# 可选：记忆文件存储目录，默认 ./data/memory/
simple.agent.memory.storage-path=./data/memory/
```

如需接入其他存储（数据库、向量库等），实现 `MemoryStrategy` 接口并提供对应 Bean，即可自动替换默认实现。

## BaseAgent 与 ReAct

`BaseAgent` 没有要求模型输出 `Thought: ... Action: ...` 形式的文本，而是使用 OpenAI 原生 Tool Calling 表达行动。两者的核心循环是一致的：

| ReAct 阶段 | `BaseAgent` 中的实现 |
| --- | --- |
| 决策下一步行动 | 模型根据消息历史和工具 Schema 生成响应 |
| Action | 模型返回结构化 `tool_calls` |
| 执行行动 | `ToolManager` 转换参数并调用 Java 方法 |
| Observation | 工具结果转换为 Tool Message 追加到本轮消息 |
| 继续决策 | 携带 Observation 再次调用模型 |
| Finish | 模型不再返回工具调用，直接生成最终答案 |

`BaseAgent` 目前最多允许 3 轮工具调用，用于防止模型陷入无限循环。与经典文本 ReAct 相比，这种实现不依赖正则表达式解析模型输出，对工具名称和参数的约束也更明确。

因此，本项目暂不另外提供与 `BaseAgent` 逻辑重复的 `ReActAgent`。如果需要研究经典的显式 ReAct 轨迹，可以直接继承 `AbstractAgent`，使用结构化 JSON 定义 Action，并自行维护 Action / Observation 历史。

## 可运行示例

`simple-agent-examples` 的 main 代码只保留 `ExampleApplication` 启动类和可复用的 `CalculatorTool`。Agent 的创建和组装过程直接写在三个 Spring Boot 测试中，可在 IDE 中分别运行：

| 示例 | 说明 |
| --- | --- |
| `ChatAgentTest` | 在测试中创建 Agent 并连续对话两轮，验证会话上下文 |
| `ToolAgentTest` | 注入 `CalculatorTool` 并注册到新 Agent，验证模型工具调用 |
| `MemoryAgentTest` | 注入 `MemoryTool`，使用两个独立 Agent 验证持久记忆 |

运行前至少需要配置 API Key；使用兼容 OpenAI 协议的其他服务时，可以继续覆盖地址和模型：

```bash
export OPENAI_API_KEY=sk-xxx
export OPENAI_BASE_URL=https://api.openai.com/v1
export OPENAI_MODEL=gpt-4o-mini
```

## 核心概念

| 类 | 职责 |
| --- | --- |
| `Agent` | 顶层接口，定义 `chat(input)` 与 `chat(input, config)` |
| `AbstractAgent` | 封装 OpenAI 调用细节，处理消息角色映射 |
| `BaseAgent` | 默认实现，管理多轮历史并执行 ReAct 式原生工具调用循环 |
| `Message` | 消息实体，包含内容、角色、时间戳与扩展元数据 |
| `MessageHistoryManager` | 线程安全的对话历史管理器 |
| `ToolDefinition` | 一个工具的模型定义和实际调用目标 |
| `ToolManager` | 手动注册工具、提供工具定义并执行模型请求 |
| `MemoryStrategy` | 记忆存储接口，定义 add / search / list / update |
| `JsonMemoryStrategyStrategyImpl` | 默认记忆实现，每个用户一个 JSON 文件 |
| `MemoryTool` | 暴露给模型的记忆工具，注册即启用 |
| `MemoryItem` | 记忆项模型，包含内容、重要性与时间戳 |

## 环境要求

- Java 8+
- Spring Boot 2.7.x
- OpenAI Java SDK 4.52.0

## License

MIT
