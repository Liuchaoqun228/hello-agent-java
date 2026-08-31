package com.example.agent;

import com.example.agent.agent.impl.BaseAgent;
import com.example.agent.tool.Tool;
import com.example.agent.tool.ToolParameter;
import com.example.agent.tool.ToolRegistry;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolCallingTest {

    @Test
    void shouldExecuteToolAndSendResultBackToModel() {
        OpenAIClient openAIClient = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        when(openAIClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
                .thenReturn(toolCallCompletion(), finalAnswerCompletion());

        AtomicReference<Map<String, Object>> receivedArguments = new AtomicReference<>();
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new Tool("add_numbers", "Add two integers") {
            @Override
            public List<ToolParameter> getParameters() {
                return Arrays.asList(
                        new ToolParameter("left", "integer", "The first integer"),
                        new ToolParameter("right", "integer", "The second integer")
                );
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                receivedArguments.set(arguments);
                int left = ((Number) arguments.get("left")).intValue();
                int right = ((Number) arguments.get("right")).intValue();
                return String.valueOf(left + right);
            }
        });

        BaseAgent agent = new BaseAgent(openAIClient, "test-model", toolRegistry);
        String answer = agent.chat("请计算 12 + 30");

        assertThat(answer).isEqualTo("计算结果是 42");
        assertThat(receivedArguments.get())
                .containsEntry("left", 12)
                .containsEntry("right", 30);

        ArgumentCaptor<ChatCompletionCreateParams> requestCaptor =
                ArgumentCaptor.forClass(ChatCompletionCreateParams.class);
        verify(openAIClient.chat().completions(), times(2)).create(requestCaptor.capture());

        ChatCompletionCreateParams firstRequest = requestCaptor.getAllValues().get(0);
        assertThat(firstRequest.tools()).isPresent();
        assertThat(firstRequest.tools().get()).hasSize(1);
        assertThat(firstRequest.tools().get().get(0).asFunction().function().name())
                .isEqualTo("add_numbers");

        ChatCompletionCreateParams secondRequest = requestCaptor.getAllValues().get(1);
        assertThat(secondRequest.messages()).hasSize(3);
        assertThat(secondRequest.messages().get(0).isUser()).isTrue();
        assertThat(secondRequest.messages().get(1).isAssistant()).isTrue();
        assertThat(secondRequest.messages().get(1).asAssistant().toolCalls())
                .isPresent();
        assertThat(secondRequest.messages().get(2).isTool()).isTrue();
        assertThat(secondRequest.messages().get(2).asTool().toolCallId())
                .isEqualTo("call_add_numbers");
        assertThat(secondRequest.messages().get(2).asTool().content().asText())
                .isEqualTo("42");
    }

    private ChatCompletion toolCallCompletion() {
        ChatCompletionMessageFunctionToolCall.Function function =
                ChatCompletionMessageFunctionToolCall.Function.builder()
                        .name("add_numbers")
                        .arguments("{\"left\":12,\"right\":30}")
                        .build();
        ChatCompletionMessage message = ChatCompletionMessage.builder()
                .content(Optional.empty())
                .refusal(Optional.empty())
                .addToolCall(ChatCompletionMessageFunctionToolCall.builder()
                        .id("call_add_numbers")
                        .function(function)
                        .build())
                .build();
        return completion(message, ChatCompletion.Choice.FinishReason.TOOL_CALLS);
    }

    private ChatCompletion finalAnswerCompletion() {
        ChatCompletionMessage message = ChatCompletionMessage.builder()
                .content("计算结果是 42")
                .refusal(Optional.empty())
                .build();
        return completion(message, ChatCompletion.Choice.FinishReason.STOP);
    }

    private ChatCompletion completion(ChatCompletionMessage message,
                                      ChatCompletion.Choice.FinishReason finishReason) {
        ChatCompletion.Choice choice = ChatCompletion.Choice.builder()
                .index(0)
                .finishReason(finishReason)
                .logprobs(Optional.empty())
                .message(message)
                .build();
        return ChatCompletion.builder()
                .id("chatcmpl-test")
                .created(0)
                .model("test-model")
                .addChoice(choice)
                .build();
    }
}
