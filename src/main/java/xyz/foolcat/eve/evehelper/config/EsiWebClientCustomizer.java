package xyz.foolcat.eve.evehelper.config;

import kotlin.text.Charsets;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * 定义访问ESI用的webclient
 *
 * @author Leojan
 * date 2023-08-01 14:50
 */

@Component
public class EsiWebClientCustomizer implements WebClientCustomizer {

    @Override
    public void customize(WebClient.Builder webClientBuilder) {
        webClientBuilder.defaultHeaders(httpHeaders -> {
            httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED));
            httpHeaders.setAcceptCharset(List.of(Charsets.UTF_8));
            httpHeaders.setCacheControl(CacheControl.noCache());
        });
    }
}
