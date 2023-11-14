package xyz.foolcat.eve.evehelper.esi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ESI Client 配置参数
 *
 * @author Leojan
 * date 2023-08-01 16:04
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "eve.esi")
public class EsiClientProperties {

    private String clientId = "bc90aa496a404724a93f41b4f4e97761";

    private String clientSecret = "";

    private String host = "login.evepc.163.com";

    private String authUrl = "https://login.evepc.163.com/v2/oauth";

    private String basePath = "https://ali-esi.evepc.163.com/latest";

    private String callBackUrl = "https://ali-esi.evepc.163.com/ui/oauth2-redirect.html";

    private String datasource = "serenity";

}
