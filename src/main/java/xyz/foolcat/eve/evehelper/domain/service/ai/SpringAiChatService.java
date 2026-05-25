package xyz.foolcat.eve.evehelper.domain.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.infrastructure.config.ai.AiMemoryProperties;
import xyz.foolcat.eve.evehelper.infrastructure.config.ai.RedisChatMemory;
import xyz.foolcat.eve.evehelper.infrastructure.config.ai.SpringAiChatMemoryConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Spring AI 框架的聊天服务
 * 集成 ChatMemory 管理多轮对话记忆
 * 支持 InMemory 和 Redis 两种存储模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiChatService {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final AiMemoryProperties memoryProperties;

    /**
     * 执行带记忆的聊天
     *
     * @param conversationId 会话ID
     * @param systemMessage  系统提示词
     * @param userMessage    用户消息
     * @return AI 响应
     */
    public String chat(String conversationId, String systemMessage, String userMessage) {
        log.debug("Chatting with conversationId: {}, user message length: {}",
                conversationId, userMessage.length());

        // 1. 获取最近 10 条历史消息
        List<Message> historyMessages = getLastNMessages(conversationId, 10);
        List<Message> allMessages = new ArrayList<>(historyMessages);

        // 2. 添加系统提示词（如果有历史消息则不需要重复添加系统消息）
        if (allMessages.isEmpty()) {
            allMessages.add(new SystemMessage(systemMessage));
        }

        // 3. 添加当前用户消息
        allMessages.add(new UserMessage(userMessage));

        // 4. 调用 AI
        try {
            Prompt prompt = new Prompt(allMessages);
            ChatResponse response = chatModel.call(prompt);
            String result = response.getResult().getOutput().getText();

            // 5. 保存对话到记忆
            chatMemory.add(conversationId, List.of(
                    new UserMessage(userMessage),
                    new AssistantMessage(result)
            ));

            // 6. 刷新 Redis TTL（如果启用）
            refreshTtlIfNeeded(conversationId);

            log.debug("Chat completed, response length: {}", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("Chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取最近 N 条消息
     * 统一处理不同 ChatMemory 实现的获取方式
     */
    private List<Message> getLastNMessages(String conversationId, int lastN) {
        if (chatMemory instanceof SpringAiChatMemoryConfig.InMemoryChatMemory) {
            return ((SpringAiChatMemoryConfig.InMemoryChatMemory) chatMemory).get(conversationId, lastN);
        } else if (chatMemory instanceof RedisChatMemory) {
            return ((RedisChatMemory) chatMemory).get(conversationId, lastN);
        } else {
            // 默认实现：获取所有消息然后截取
            List<Message> all = chatMemory.get(conversationId);
            if (lastN >= all.size()) {
                return all;
            }
            return all.subList(all.size() - lastN, all.size());
        }
    }

    /**
     * 如果配置了刷新 TTL，则刷新会话过期时间（仅 Redis 模式）
     */
    private void refreshTtlIfNeeded(String conversationId) {
        if (memoryProperties.isRefreshTtl() && chatMemory instanceof RedisChatMemory) {
            ((RedisChatMemory) chatMemory).touch(conversationId);
        }
    }

    /**
     * 清空会话记忆
     *
     * @param conversationId 会话ID
     */
    public void clearMemory(String conversationId) {
        log.debug("Clearing chat memory for conversationId: {}", conversationId);
        chatMemory.clear(conversationId);
    }

    /**
     * 获取会话历史消息数
     *
     * @param conversationId 会话ID
     * @return 历史消息数
     */
    public int getHistoryMessageCount(String conversationId) {
        return getLastNMessages(conversationId, 1000).size();
    }
}
