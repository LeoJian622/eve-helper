package xyz.foolcat.eve.evehelper.domain.service.ai;

/**
 * AI 聊天客户端接口
 * 抽象不同的 AI 提供商实现
 */
public interface AiChatClient {

    /**
     * 发送消息到 AI 并获取响应
     *
     * @param systemMessage 系统提示词
     * @param userMessage   用户消息
     * @return AI 响应内容
     */
    String chat(String systemMessage, String userMessage);
}
