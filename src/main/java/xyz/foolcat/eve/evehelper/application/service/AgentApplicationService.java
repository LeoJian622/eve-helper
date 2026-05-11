package xyz.foolcat.eve.evehelper.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.DateTimeTools;
import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.WeatherTools;

/**
 * 智能体应用服务
 *
 * @author yongj
 * date 2026-04-14 15:07
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {

    public final ChatClient.Builder chatClientBuilder;

    public String simpleAgent(String message) {
//        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey("sk-5808bc18986b4e15a521c6b7c8021736").build();
//        DashScopeChatModel model = DashScopeChatModel.builder()
//                .dashScopeApi(dashScopeApi)
//                .defaultOptions(
//                        DashScopeChatOptions.builder()
//                                .model("qwen3-max-2026-01-23")
//                                .temperature(0.7)
//                                .maxToken(64000)
//                                .topP(0.9)
//                                .build()
//                ).build();
        ChatClient build1 = chatClientBuilder.build();
        return build1
                .prompt(message)
                .toolNames(WeatherTools.CURRENT_WEATHER_TOOL)
                .tools(new DateTimeTools())
                .call().content();
    }
}
