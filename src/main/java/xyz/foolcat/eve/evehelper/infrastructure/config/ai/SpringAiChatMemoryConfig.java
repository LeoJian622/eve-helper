package xyz.foolcat.eve.evehelper.infrastructure.config.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI Chat Memory 配置
 * 支持内存版和 Redis 版两种实现
 *
 * 配置方式:
 * spring.ai.memory.type=inmemory  # 内存版（默认）
 * spring.ai.memory.type=redis     # Redis持久化版
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SpringAiChatMemoryConfig {

    private final AiMemoryProperties properties;

    /**
     * 内存版 ChatMemory - 默认配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.memory", name = "type", havingValue = "inmemory", matchIfMissing = true)
    public ChatMemory inMemoryChatMemory() {
        log.info("Using InMemoryChatMemory for AI chat memory");
        return new InMemoryChatMemory();
    }

    /**
     * Redis 版 ChatMemory
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.memory", name = "type", havingValue = "redis")
    public ChatMemory redisChatMemory(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        log.info("Using RedisChatMemory for AI chat memory, ttl: {} minutes", properties.getRedisTtlMinutes());
        return new RedisChatMemory(redisTemplate, objectMapper, properties.getRedisTtlMinutes());
    }

    /**
     * 默认 ChatMemory（兜底配置）
     */
    @Bean
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory defaultChatMemory() {
        log.info("Using default InMemoryChatMemory for AI chat memory");
        return new InMemoryChatMemory();
    }

    /**
     * 简单的内存版 ChatMemory 实现
     */
    public static class InMemoryChatMemory implements ChatMemory {

        private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();

        @Override
        public void add(String conversationId, List<Message> messages) {
            conversationStore.computeIfAbsent(conversationId, k -> new ArrayList<>())
                    .addAll(messages);
        }

        @Override
        public List<Message> get(String conversationId) {
            return get(conversationId, 10);
        }

        @Override
        public void clear(String conversationId) {
            conversationStore.remove(conversationId);
        }

        /**
         * 获取最近 N 条消息
         */
        public List<Message> get(String conversationId, int lastN) {
            List<Message> allMessages = conversationStore.getOrDefault(conversationId, new ArrayList<>());
            return lastN >= allMessages.size() ? allMessages :
                    allMessages.subList(allMessages.size() - lastN, allMessages.size());
        }
    }
}
