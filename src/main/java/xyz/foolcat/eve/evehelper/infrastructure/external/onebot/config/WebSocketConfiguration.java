package xyz.foolcat.eve.evehelper.infrastructure.external.onebot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * Websocket服务配置文件
 *
 * @author Leojan
 * @date 2024-06-21 11:53
 */

@Configuration
@EnableWebSocket
@Profile("!test")
public class WebSocketConfiguration {
    /**
     * 	注入ServerEndpointExporter，
     * 	这个bean会自动注册使用了@ServerEndpoint注解声明的Websocket endpoint
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

