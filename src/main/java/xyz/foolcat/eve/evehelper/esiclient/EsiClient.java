package xyz.foolcat.eve.evehelper.esiclient;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ESI访问客户端
 *
 * @author Leojan
 * @date 2023-08-01 15:37
 */

@Configuration
@RequiredArgsConstructor
public class EsiClient {

    private final EsiClientProperties esiClientProperties;

    @Bean
     WebClient authClient() {
        return WebClient.builder().clone().baseUrl(esiClientProperties.getAuthUrl())
                .defaultHeader("Host", esiClientProperties.getHost())
                .build();
    }

    @Bean
     WebClient apiClient() {
        return WebClient.builder().clone().baseUrl(esiClientProperties.getBasePath())
                .build();
    }

}
