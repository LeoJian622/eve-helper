package xyz.foolcat.eve.evehelper.infrastructure.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Leojan
 * date 2022-03-22 14:32
 */

@Component
@ConfigurationProperties(prefix = "eve.helper")
@Data
public class EveHelperSecurityConfig {

    private List<String> whiteUrlList;

    /**
     * 系统内部调用所用的用户 ID（定时任务、机器人等无安全上下文的场景）。
     * <p>
     * 对应配置项 {@code eve.helper.system-user-id}，默认 1。
     * 生产部署须通过环境变量 {@code SYSTEM_USER_ID} 指定非默认值，避免「用户 1 即特权账户」
     * 固化进代码（IDOR 审计 LOW-9）。
     */
    private Integer systemUserId = 1;
}
