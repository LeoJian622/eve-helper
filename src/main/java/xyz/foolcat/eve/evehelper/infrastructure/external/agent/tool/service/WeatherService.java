package xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service;

import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service.schema.WeatherRequest;
import xyz.foolcat.eve.evehelper.infrastructure.external.agent.tool.service.schema.WeatherResponse;

import java.util.function.Function;

/**
 * 示例：天气工具
 *
 * @author yongj
 * date 2026-04-14 14:32
 */

public class WeatherService implements Function<WeatherRequest, WeatherResponse> {

    @Override
    public WeatherResponse apply(WeatherRequest weatherRequest) {
        return new WeatherResponse(39.0, "C");
    }
}
