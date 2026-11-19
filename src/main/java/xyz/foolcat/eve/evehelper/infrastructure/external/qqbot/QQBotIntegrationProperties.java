package xyz.foolcat.eve.evehelper.infrastructure.external.qqbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "eve.helper.qqbot")
@Data
public class QQBotIntegrationProperties {

    private Set<String> adminOpenids = new HashSet<>();

    private String notificationGroupOpenid = "";

    private String miningGroupOpenid = "";

    public boolean isAdmin(String openid) {
        return openid != null && adminOpenids.contains(openid);
    }
}
