package com.example.agent.agent;

import com.example.agent.tool.dto.ToolCall;
import com.example.agent.tool.dto.ToolDefinition;
import com.example.agent.tool.dto.ToolParameter;
import com.example.agent.message.Message;
import com.example.agent.message.MessageRoleEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractAgent implements Agent {

    private static final TypeReference<Map<String, Object>> TOOL_ARGUMENTS_TYPE =
            new TypeReference<Map<String, Object>>() {
            };

    private final OpenAIClient openAIClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected AbstractAgent(OpenAIClient openAIClient, String model) {
        Assert.notNull(openAIClient, "openAIClient must not be null");
        Assert.hasText(model, "model must not be empty");
        this.openAIClient = openAIClient;
        this.model = model;
    }


    protected final Message call(List<Message> messages) {
        return call(messages, Collections.emptyList());
    }

    protected final Message call(List<Message> messages, List<ToolDefinition> tools) {
        Assert.notEmpty(messages, "messages must not be empty");
        Assert.notNull(tools, "tools must not be null");

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(model);

        for (Message message : messages) {
            addMessage(builder, message);
        }
        for (ToolDefinition tool : tools) {
            addTool(builder, tool);
        }

        ChatCompletion completion = openAIClient.chat().completions().create(builder.build());
        Assert.state(!completion.choices().isEmpty(), "completion choices must not be empty");

        return toMessage(completion.choices().get(0).message());
    }

    private void addMessage(ChatCompletionCreateParams.Builder builder, Message message) {
        Assert.notNull(message, "message must not be null");
        Assert.notNull(message.getMessageRole(), "message role must not be null");

        if (message.getMessageRole() == MessageRoleEnum.SYSTEM) {
            Assert.hasText(message.getContent(), "system message content must not be empty");
            builder.addSystemMessage(message.getContent());
        } else if (message.getMessageRole() == MessageRoleEnum.USER) {
            Assert.hasText(message.getContent(), "user message content must not be empty");
            builder.addUserMessage(message.getContent());
        } else if (message.getMessageRole() == MessageRoleEnum.ASSISTANT) {
            addAssistantMessage(builder, message);
        } else if (message.getMessageRole() == MessageRoleEnum.TOOL_EXEC_RESULT) {
            Assert.hasText(message.getToolCallId(), "toolCallId must not be empty");
            Assert.hasText(message.getContent(), "tool exec result content must not be empty");
            builder.addMessage(ChatCompletionToolMessageParam.builder()
                    .toolCallId(message.getToolCallId())
                    .content(message.getContent())
                    .build());
        } else {
            throw new IllegalArgumentException("unsupported message role: " + message.getMessageRole());
        }
    }

    private void addAssistantMessage(ChatCompletionCreateParams.Builder builder, Message message) {
        List<ToolCall> toolCalls = message.getToolCallList();
        if (CollectionUtils.isEmpty(toolCalls)) {
            Assert.hasText(message.getContent(), "assistant message content must not be empty");
            builder.addAssistantMessage(message.getContent());
            return;
        }

        ChatCompletionAssistantMessageParam.Builder assistantBuilder =
                ChatCompletionAssistantMessageParam.builder();
        if (StringUtils.hasText(message.getContent())) {
            assistantBuilder.content(message.getContent());
        }

        for (ToolCall toolCall : toolCalls) {
            Assert.notNull(toolCall, "tool call must not be null");
            Assert.hasText(toolCall.getId(), "tool call id must not be empty");
            Assert.hasText(toolCall.getName(), "tool call name must not be empty");

            ChatCompletionMessageFunctionToolCall.Function function =
                    ChatCompletionMessageFunctionToolCall.Function.builder()
                            .name(toolCall.getName())
                            .arguments(writeArguments(toolCall.getArguments()))
                            .build();
            assistantBuilder.addToolCall(ChatCompletionMessageFunctionToolCall.builder()
                    .id(toolCall.getId())
                    .function(function)
                    .build());
        }
        builder.addMessage(assistantBuilder.build());
    }

    private void addTool(ChatCompletionCreateParams.Builder builder, ToolDefinition tool) {
        Assert.notNull(tool, "tool must not be null");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        List<ToolParameter> parameters = tool.getParameters();
        if (parameters != null) {
            for (ToolParameter parameter : parameters) {
                Assert.notNull(parameter, "tool parameter must not be null");
                Assert.isTrue(!properties.containsKey(parameter.getName()),
                        "duplicate parameter name in tool " + tool.getName() + ": " + parameter.getName());

                Map<String, Object> property = new LinkedHashMap<>();
                property.put("type", parameter.getType());
                properties.put(parameter.getName(), property);
                required.add(parameter.getName());
            }
        }

        FunctionParameters.Builder parametersBuilder = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false));
        if (!required.isEmpty()) {
            parametersBuilder.putAdditionalProperty("required", JsonValue.from(required));
        }

        FunctionDefinition functionDefinition = FunctionDefinition.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .parameters(parametersBuilder.build())
                .build();
        builder.addFunctionTool(functionDefinition);
    }

    private Message toMessage(ChatCompletionMessage openAIMessage) {
        List<ToolCall> toolCalls = new ArrayList<>();
        for (ChatCompletionMessageToolCall openAIToolCall : openAIMessage.toolCalls().orElse(Collections.emptyList())) {
            Assert.state(openAIToolCall.isFunction(), "only function tool calls are supported");
            ChatCompletionMessageFunctionToolCall functionToolCall = openAIToolCall.asFunction();
            toolCalls.add(new ToolCall(
                    functionToolCall.id(),
                    functionToolCall.function().name(),
                    readArguments(functionToolCall.function().arguments())
            ));
        }

        String content = openAIMessage.content().orElse(null);
        Assert.state(!toolCalls.isEmpty() || StringUtils.hasText(content),
                "completion does not contain text or function tool calls");

        Message message = new Message(content, MessageRoleEnum.ASSISTANT);
        if (!toolCalls.isEmpty()) {
            message.setToolCallList(toolCalls);
        }
        return message;
    }

    private Map<String, Object> readArguments(String arguments) {
        try {
            return objectMapper.readValue(arguments, TOOL_ARGUMENTS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("tool call arguments are not valid JSON", exception);
        }
    }

    private String writeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(
                    arguments == null ? Collections.emptyMap() : arguments);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("tool call arguments cannot be serialized", exception);
        }
    }
}
