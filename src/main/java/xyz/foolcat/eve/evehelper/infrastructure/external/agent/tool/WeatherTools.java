package xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service.WeatherService;
import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service.schema.WeatherRequest;
import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service.schema.WeatherResponse;

import java.util.function.Function;

/**
 * 示例：动态规范 @Bean 配置tools工具
 *
 * @author yongj
 * date 2026-04-14 14:35
 */

@Configuration(proxyBeanMethods = false)
public class WeatherTools {

    WeatherService weatherService = new WeatherService();

    public static final String CURRENT_WEATHER_TOOL = "currentWeather";

    @Bean(CURRENT_WEATHER_TOOL)
    @Description("Get the weather in location")
    public Function<WeatherRequest, WeatherResponse> currentWeather(){
        return weatherService;
    }

}
