package xyz.foolcat.eve.evehelper.infrastructure.config.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的 ChatMemory 实现
 * 支持会话记忆的持久化存储和自动过期
 */
@Slf4j
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private static final String CHAT_MEMORY_PREFIX = "ai:chat:memory:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlMinutes;

    /**
     * 默认24小时过期
     */
    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this(redisTemplate, objectMapper, 1440);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String key = getKey(conversationId);

        try {
            List<Message> existing = get(conversationId);
            List<Message> allMessages = new ArrayList<>(existing);
            allMessages.addAll(messages);

            List<Map<String, Object>> serialized = allMessages.stream()
                    .map(this::serializeMessage)
                    .collect(Collectors.toList());

            redisTemplate.opsForValue().set(key, serialized, ttlMinutes, TimeUnit.MINUTES);

            log.debug("Added {} messages to Redis chat memory, conversationId: {}, total: {}",
                    messages.size(), conversationId, allMessages.size());
        } catch (Exception e) {
            log.error("Failed to add messages to Redis chat memory: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, Integer.MAX_VALUE);
    }

    @Override
    public void clear(String conversationId) {
        String key = getKey(conversationId);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Cleared chat memory for conversationId: {}, deleted: {}", conversationId, deleted);
    }

    /**
     * 获取最近 N 条消息
     */
    @SuppressWarnings("unchecked")
    public List<Message> get(String conversationId, int lastN) {
        String key = getKey(conversationId);

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> serialized = objectMapper.convertValue(value,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<Message> allMessages = serialized.stream()
                    .map(this::deserializeMessage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (lastN >= allMessages.size()) {
                return allMessages;
            }

            return allMessages.subList(allMessages.size() - lastN, allMessages.size());
        } catch (Exception e) {
            log.error("Failed to get messages from Redis chat memory: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 刷新会话过期时间
     */
    public void touch(String conversationId) {
        String key = getKey(conversationId);
        redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);
    }

    private String getKey(String conversationId) {
        return CHAT_MEMORY_PREFIX + conversationId;
    }

    private Map<String, Object> serializeMessage(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageType", message.getMessageType().name());
        map.put("text", message.getText());
        map.put("metadata", message.getMetadata());
        return map;
    }

    private Message deserializeMessage(Map<String, Object> map) {
        try {
            String messageTypeStr = (String) map.get("messageType");
            String text = (String) map.get("text");

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) map.get("metadata");
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            MessageType messageType = MessageType.valueOf(messageTypeStr);

            return switch (messageType) {
                case USER -> new UserMessage(text);
                case ASSISTANT -> new AssistantMessage(text);
                case SYSTEM -> new SystemMessage(text);
                default -> throw new IllegalArgumentException("Unsupported message type: " + messageType);
            };
        } catch (Exception e) {
            log.warn("Failed to deserialize message: {}", e.getMessage());
            return null;
        }
    }
}
