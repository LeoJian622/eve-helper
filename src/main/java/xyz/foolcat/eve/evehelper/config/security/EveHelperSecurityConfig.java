package xyz.foolcat.eve.evehelper.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Leojan
 * @date 2022-03-22 14:32
 */

@Component
@ConfigurationProperties(prefix = "eve.helper")
@Data
public class EveHelperSecurityConfig {

    private List<String> whiteUrlList;
}
