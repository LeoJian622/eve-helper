package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ESI访问客户端
 *
 * @author Leojan
 * date 2023-08-01 15:37
 */

@Configuration
@RequiredArgsConstructor
public class EsiClientConfig {

    private final EsiClientProperties esiClientProperties;

    public static final String SERENITY = "serenity";

    public static final String ZH_CN = "zh";
    public static final String EN_US = "en";

    @Bean("authClient")
     WebClient authClient() {
        return WebClient.builder().clone().baseUrl(esiClientProperties.getAuthUrl())
                .defaultHeader("Host", esiClientProperties.getHost())
                .defaultHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED));
                    httpHeaders.setAcceptCharset(List.of(StandardCharsets.UTF_8));
                    httpHeaders.setCacheControl(CacheControl.noCache());
                })
                .build();
    }

    @Bean("esiClient")
     WebClient esiClient() {
        return WebClient.builder().clone().baseUrl(esiClientProperties.getBasePath())
                .codecs(item->item.defaultCodecs().maxInMemorySize(1024 * 1024))
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED));
                    httpHeaders.setAcceptCharset(List.of(StandardCharsets.UTF_8));
                    httpHeaders.setCacheControl(CacheControl.noCache());
                })
                .build();
    }

}
