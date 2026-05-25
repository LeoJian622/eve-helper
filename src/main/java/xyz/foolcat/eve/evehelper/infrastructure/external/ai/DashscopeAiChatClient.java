package xyz.foolcat.eve.evehelper.infrastructure.external.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.service.ai.AiChatClient;

import java.util.List;

/**
 * 通义千问 AI 客户端实现
 * 使用 Spring AI Alibaba Dashscope 集成
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "dashscope")
public class DashscopeAiChatClient implements AiChatClient {


    private final ChatModel dashscopeChatModel;

    @Override
    public String chat(String systemMessage, String userMessage) {
        log.debug("Calling Dashscope AI with system message length: {}, user message: {}",
                systemMessage.length(), userMessage);

        try {
            SystemMessage systemMsg =
                    new SystemMessage(systemMessage);
            UserMessage userMsg =
                    new UserMessage(userMessage);

            Prompt prompt = new Prompt(List.of(systemMsg, userMsg));
            ChatResponse response = dashscopeChatModel.call(prompt);

            String result = response.getResult().getOutput().getText();
            log.debug("Dashscope response received, length: {}", result != null ? result.length() : 0);

            return result;
        } catch (Exception e) {
            log.error("Dashscope AI call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }
}
