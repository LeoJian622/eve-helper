package xyz.foolcat.eve.evehelper.esi;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ESI访问客户端
 *
 * @author Leojan
 * date 2023-08-01 15:37
 */

@Configuration
@RequiredArgsConstructor
public class EsiClient {

    private final EsiClientProperties esiClientProperties;

    public static final String SERENITY = "serenity";

    @Bean
     WebClient authClient() {
        return WebClient.builder().clone().baseUrl(esiClientProperties.getAuthUrl())
                .defaultHeader("Host", esiClientProperties.getHost())
                .defaultHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Bean
     WebClient apiClient() {
        return WebClient.builder().clone().baseUrl(esiClientProperties.getBasePath())
                .codecs(item->item.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

}
