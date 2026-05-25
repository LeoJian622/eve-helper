package xyz.foolcat.eve.evehelper.infrastructure.external.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.domain.service.ai.AiChatClient;

/**
 * Mock AI 客户端实现
 * 用于开发测试，当没有配置真实 AI 提供商时使用
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiChatClient implements AiChatClient {

    @Override
    public String chat(String systemMessage, String userMessage) {
        log.warn("Using MOCK AI client. Question: {}", userMessage);

        // 返回一个模拟的 SQL 用于测试
        return """
                SELECT * FROM (
                    SELECT id, user_id, question, created_at
                    FROM ai_query_history
                    ORDER BY created_at DESC
                ) temp LIMIT 50
                """;
    }
}
