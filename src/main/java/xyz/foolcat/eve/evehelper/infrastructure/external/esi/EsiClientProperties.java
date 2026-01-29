package xyz.foolcat.eve.evehelper.infrastructure.external.esi;

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

    /**
     * 客户端ID
     */
    private String clientId = "bc90aa496a404724a93f41b4f4e97761";

    /**
     * 客户端密钥
     */
    private String clientSecret = "";

    /**
     * 登录域名
     */
    private String host = "login.evepc.163.com";

    /**
     * 授权URL
     */
    private String authUrl = "https://login.evepc.163.com/v2/oauth";

    /**
     * ESI服务器地址
     */
    private String basePath = "https://ali-esi.evepc.163.com/latest";

    /**
     * 回调URL
     */
    private String callBackUrl = "https://ali-esi.evepc.163.com/ui/oauth2-redirect.html";

    /**
     * 服务器数据源
     */
    private String datasource = "serenity";

}
