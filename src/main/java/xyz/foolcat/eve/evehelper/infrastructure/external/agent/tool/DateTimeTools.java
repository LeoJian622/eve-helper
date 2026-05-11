package xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 大模型可用的时间工具类
 *
 * @author yongj
 * date 2026-04-14 10:50
 */

@Component
public class DateTimeTools {

    @Tool(description = "Get the current date and time in the user's timezone")
    String getCurrenDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}
