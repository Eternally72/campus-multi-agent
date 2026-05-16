package com.campus.agent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiGateway {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public String complete(String systemPrompt, String userPrompt) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return fallback(userPrompt);
        }

        try {
            String content = builder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            return content == null || content.isBlank() ? fallback(userPrompt) : content;
        } catch (RuntimeException exception) {
            return fallback(userPrompt) + "\n\n[本地降级] AI 服务暂不可用：" + exception.getMessage();
        }
    }

    private String fallback(String userPrompt) {
        return "我已经收到你的问题。当前处于本地降级模式：系统会保存会话、调用业务工具和检索知识库；配置 AI_DASHSCOPE_API_KEY 后会切换为百炼模型回答。\n\n问题摘要：" + summarize(userPrompt);
    }

    private String summarize(String text) {
        return text.length() <= 260 ? text : text.substring(0, 260) + "...";
    }
}
